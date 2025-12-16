package com.example.vprofile.voicesearch;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class JDExtractionService {

    // ----------------------- FILE READERS -----------------------

    public String extractText(MultipartFile file) throws Exception {
        String filename = Objects.requireNonNull(file.getOriginalFilename()).toLowerCase();

        if (filename.endsWith(".pdf")) return extractPdf(file);
        if (filename.endsWith(".docx")) return extractDocx(file);
        if (filename.endsWith(".txt")) return new String(file.getBytes());

        throw new IllegalArgumentException("Unsupported file format: " + filename);
    }

    private String extractPdf(MultipartFile file) throws IOException {
        try (PDDocument doc = PDDocument.load(file.getInputStream())) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private String extractDocx(MultipartFile file) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(file.getInputStream())) {
            StringBuilder sb = new StringBuilder();
            doc.getParagraphs().forEach(p -> sb.append(p.getText()).append("\n"));
            return sb.toString();
        }
    }

    // ----------------------- ADVANCED EXTRACTION -----------------------

    public Map<String, String> extractStructuredData(String jdText) {

        String cleanText = jdText.replaceAll("\\s+", " ").trim();

        String jobTitle = extractJobTitle(cleanText);
        String skills = extractSkills(cleanText);
        String description = extractResponsibilities(cleanText);

        String query = (skills + " " + description).trim();
        if (query.length() < 25) query = cleanText;

        Map<String, String> result = new HashMap<>();
        result.put("title", jobTitle);
        result.put("skills", skills);
        result.put("description", query);
        return result;
    }

    // ----------------------- JOB TITLE DETECTOR -----------------------

    private String extractJobTitle(String text) {
        // Finds first bolded-like title or "We are hiring: X"
        Pattern p = Pattern.compile("(job title|position|role|hiring for)[:\\- ]+(.*?)(\\n|$)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if (m.find()) return m.group(2).trim();

        // fallback: extract first line with 1–5 words
        String[] lines = text.split("\n");
        for (String line : lines) {
            if (line.trim().length() > 3 && line.trim().length() < 50) {
                return line.trim();
            }
        }
        return "Unknown Title";
    }

    // ----------------------- SKILLS DETECTOR -----------------------

    private String extractSkills(String text) {

        List<String> skillTags = Arrays.asList(
                "skills", "key skills", "skills required", "desired skills",
                "expertise", "technical skills", "must have", "requirements"
        );

        for (String tag : skillTags) {
            String sec = extractSection(text, tag, 400);
            if (sec.length() > 10) {
                return removeLabels(sec);
            }
        }

        return "";
    }

    // ----------------------- RESPONSIBILITIES DETECTOR -----------------------

    private String extractResponsibilities(String text) {

        List<String> tags = Arrays.asList(
                "responsibilities", "job description", "role", "about the role",
                "what you will do", "your responsibilities", "duties"
        );

        for (String tag : tags) {
            String sec = extractSection(text, tag, 600);
            if (sec.length() > 20) {
                return removeLabels(sec);
            }
        }

        return "";
    }

    // ----------------------- SECTION EXTRACTION -----------------------

    private String extractSection(String text, String keyword, int length) {
        String lower = text.toLowerCase();
        int idx = lower.indexOf(keyword.toLowerCase());
        if (idx == -1) return "";

        int end = Math.min(idx + length, text.length());
        return text.substring(idx, end).trim();
    }

    private String removeLabels(String text) {
        return text
                .replaceAll("(?i)skills:? ", "")
                .replaceAll("(?i)responsibilities:? ", "")
                .replaceAll("(?i)requirements:? ", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
