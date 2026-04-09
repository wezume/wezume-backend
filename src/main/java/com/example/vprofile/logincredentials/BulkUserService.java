package com.example.vprofile.logincredentials;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class BulkUserService {

    @Autowired
    private UserRepository userRepository;

    public List<User> processFile(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName == null) return new ArrayList<>();

        if (fileName.endsWith(".csv")) {
            return processCsv(file.getInputStream());
        } else if (fileName.endsWith(".xlsx")) {
            return processExcel(file.getInputStream(), true);
        } else if (fileName.endsWith(".xls")) {
            return processExcel(file.getInputStream(), false);
        } else if (fileName.endsWith(".docx")) {
            return processDocx(file.getInputStream());
        } else {
            throw new IllegalArgumentException("Unsupported file format: " + fileName);
        }
    }

    private List<User> processCsv(InputStream is) throws IOException {
        List<User> users = new ArrayList<>();
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .setAllowMissingColumnNames(true)
                .build();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
             CSVParser csvParser = new CSVParser(reader, csvFormat)) {

            for (CSVRecord record : csvParser) {
                User user = mapRecordToUser(new CsvRowAdapter(record));
                if (user != null) users.add(user);
            }
        }
        return saveNonNullUsers(users);
    }

    private List<User> processExcel(InputStream is, boolean isXlsx) throws IOException {
        List<User> users = new ArrayList<>();
        try (Workbook workbook = isXlsx ? new XSSFWorkbook(is) : new HSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            List<String> headers = new ArrayList<>();
            Row headerRow = null;

            // Scan for a row that looks like a header (contains "Mail id", "Email", or "Name")
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                List<String> currentHeaders = new ArrayList<>();
                boolean isHeader = false;
                // Use a larger range for headers to avoid skipping columns
                for (int i = 0; i < Math.max(row.getLastCellNum(), 20); i++) {
                    String value = getCellValueAsString(row.getCell(i)).trim();
                    currentHeaders.add(value);
                    if (value.equalsIgnoreCase("Mail id") || value.equalsIgnoreCase("Email") || value.equalsIgnoreCase("Name")) {
                        isHeader = true;
                    }
                }
                if (isHeader) {
                    headers = currentHeaders;
                    headerRow = row;
                    break;
                }
            }

            if (headerRow == null) {
                return users;
            }

            while (rowIterator.hasNext()) {
                Row currentRow = rowIterator.next();
                if (isRowEmpty(currentRow)) continue;
                User user = mapRecordToUser(new ExcelRowAdapter(currentRow, headers));
                if (user != null) users.add(user);
            }
        }
        return saveNonNullUsers(users);
    }

    private boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }

    private List<User> processDocx(InputStream is) throws IOException {
        List<User> users = new ArrayList<>();
        try (XWPFDocument document = new XWPFDocument(is)) {
            for (XWPFTable table : document.getTables()) {
                List<XWPFTableRow> rows = table.getRows();
                if (rows.isEmpty()) continue;

                XWPFTableRow headerRow = rows.get(0);
                List<String> headers = new ArrayList<>();
                for (XWPFTableCell cell : headerRow.getTableCells()) {
                    headers.add(cell.getText().trim());
                }

                for (int i = 1; i < rows.size(); i++) {
                    XWPFTableRow currentRow = rows.get(i);
                    User user = mapRecordToUser(new DocxRowAdapter(currentRow, headers));
                    users.add(user);
                }
            }
        }
        return saveNonNullUsers(users);
    }

    private User mapRecordToUser(RowAdapter row) {
        String email = row.get("Mail id", "Mailid", "Email", "Mail");
        if (email == null || email.isBlank()) {
            return null;
        }

        User user = new User();
        user.setEmail(email.trim());
        
        // Handle "Name" by splitting it into first and last name if possible
        String fullName = row.get("Name", "name");
        if (fullName != null && !fullName.isBlank()) {
            String[] parts = fullName.trim().split("\\s+", 2);
            user.setFirstName(parts[0]);
            if (parts.length > 1) {
                user.setLastName(parts[1]);
            }
        }

        String phone = row.get("Phone number.", "Phone number", "PhoneNumber", "Phone");
        if (phone != null && !phone.isBlank()) {
            user.setPhoneNumber(phone.trim());
        } else {
            user.setPhoneNumber(null); // Fixes Duplicate entry '' for phone
        }

        user.setJobOption(row.get("jobOption", "Job Option", "JobOption"));
        user.setPassword(row.get("Password", "password"));
        
        // Ensure enabled is true for new bulk-uploaded users
        user.setEnabled(true);

        return user;
    }

    private List<User> saveNonNullUsers(List<User> users) {
        List<User> validUsers = new ArrayList<>();
        java.util.Set<String> seenEmails = new java.util.HashSet<>();
        java.util.Set<String> seenPhones = new java.util.HashSet<>();
        
        for (User u : users) {
            if (u == null) continue;
            
            String email = u.getEmail().toLowerCase().trim();
            String phone = u.getPhoneNumber() != null ? u.getPhoneNumber().trim() : null;

            // Check for duplicates within the same batch/sheet
            if (seenEmails.contains(email)) {
                continue;
            }
            if (phone != null && !phone.isBlank() && seenPhones.contains(phone)) {
                continue;
            }

            // Check if email already exists in database
            if (userRepository.existsByEmail(email)) {
                continue;
            }
            
            // Check if phone number exists in database
            if (phone != null && !phone.isBlank() && userRepository.existsByPhoneNumber(phone)) {
                continue;
            }

            seenEmails.add(email);
            if (phone != null && !phone.isBlank()) seenPhones.add(phone);
            validUsers.add(u);
        }
        
        if (validUsers.isEmpty()) return validUsers;
        return userRepository.saveAll(validUsers);
    }

    interface RowAdapter {
        String get(String... keys);
    }

    class CsvRowAdapter implements RowAdapter {
        private final CSVRecord record;
        CsvRowAdapter(CSVRecord record) { this.record = record; }
        public String get(String... keys) {
            for (String key : keys) {
                if (record.isMapped(key)) return record.get(key);
            }
            return null;
        }
    }

    class ExcelRowAdapter implements RowAdapter {
        private final Row row;
        private final List<String> headers;
        ExcelRowAdapter(Row row, List<String> headers) { this.row = row; this.headers = headers; }
        public String get(String... keys) {
            for (String key : keys) {
                for (int i = 0; i < headers.size(); i++) {
                    if (headers.get(i).equalsIgnoreCase(key)) {
                        Cell cell = row.getCell(i);
                        if (cell == null) return null;
                        return getCellValueAsString(cell);
                    }
                }
            }
            return null;
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC: 
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                // Handle long numeric values like phone numbers without scientific notation
                double val = cell.getNumericCellValue();
                if (val == (long) val) {
                    return String.valueOf((long) val);
                } else {
                    return String.valueOf(val);
                }
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA: 
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BLANK: return "";
            default: return "";
        }
    }

    class DocxRowAdapter implements RowAdapter {
        private final XWPFTableRow row;
        private final List<String> headers;
        DocxRowAdapter(XWPFTableRow row, List<String> headers) { this.row = row; this.headers = headers; }
        public String get(String... keys) {
            for (String key : keys) {
                for (int i = 0; i < headers.size(); i++) {
                    if (headers.get(i).equalsIgnoreCase(key)) {
                        if (i < row.getTableCells().size()) {
                            return row.getCell(i).getText();
                        }
                    }
                }
            }
            return null;
        }
    }
}
