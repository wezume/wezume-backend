package com.example.vprofile.voicesearch;

import java.io.IOException;
import java.util.*;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class JDExtractionService {

    @Value("${groq.api.key}")
    private String groqApiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public JDExtractionService() {
        this.webClient = WebClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }

    public String extractText(MultipartFile file) throws Exception {
        String filename = Objects.requireNonNull(file.getOriginalFilename()).toLowerCase();

        if (filename.endsWith(".pdf"))
            return extractPdf(file);
        if (filename.endsWith(".docx"))
            return extractDocx(file);
        if (filename.endsWith(".txt"))
            return new String(file.getBytes());

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

    public Map<String, String> extractStructuredData(String jdText) {
        try {
            return extractWithGroq(jdText);
        } catch (Exception e) {
            System.err.println("Groq extraction failed: " + e.getMessage());
            e.printStackTrace();
            return fallbackExtraction();
        }
    }

    private Map<String, String> extractWithGroq(String jdText) throws Exception {
        String prompt = buildPrompt(jdText);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "llama-3.3-70b-versatile");
        requestBody.put("messages", Collections.singletonList(message));
        requestBody.put("temperature", 0.1);
        requestBody.put("max_tokens", 1000);

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        String response = webClient.post()
                .uri("https://api.groq.com/openai/v1/chat/completions")
                .header("Authorization", "Bearer " + groqApiKey)
                .header("Content-Type", "application/json")
                .bodyValue(jsonBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return parseGroqResponse(response);
    }

    private String buildPrompt(String jdText) {
        return """
                You are an expert Job Description Parser. Extract data from the text below and return ONLY a valid JSON object with exactly these 3 fields:

                CRITICAL RULES:
                1. Skills: Extract ONLY core technical/domain skills as keywords (e.g., "Excel", "Python", "Management", "Marketing"). Remove wrapper words. Just skill names separated by commas.

                2. Qualification: Extract important keywords comprehensively. Be SELECTIVE - extract only the MOST RELEVANT keywords (allow up to 40% extra):
                   - Degrees (MBA, B.Tech, Bachelor's, Master's)
                   - Key certifications (PMP, CFA, Google Analytics)
                   - Primary domain knowledge (Sports, Finance, Healthcare, Technology)
                   - Main industries (Retail, E-commerce, SaaS)
                   - Core responsibilities (Strategy, Planning, Analysis, Coordination)
                   - TOP personal qualities mentioned (Leadership, Communication, Team Player, Problem Solving, Analytical, Creative)
                   - Key experience topics (Business Development, Operations, Marketing, Sales) - WITHOUT time duration
                   - Important soft skills (Collaboration, Initiative, Detail-Oriented)

                   Extract as KEYWORDS only (not full sentences). Separate with commas.
                   IMPORTANT: Focus on CORE requirements. Allow up to 40% noise - be selective but comprehensive!

                3. Experience: Extract ONLY the numerical time duration (e.g., "2-5 years", "3+ months", "Fresher"). If no time mentioned, write "Not specified".

                OUTPUT FORMAT (selective keywords only):
                {
                  "skills": "Excel, Python, Management, Marketing",
                  "qualification": "MBA, Sports Industry, Business Development, Operations, Strategy, Leadership, Communication, Team Player, Problem Solving, Analytical Thinking, Creative, Market Research, Project Management, Stakeholder Management",
                  "experience": "2-5 years"
                }

                IMPORTANT: Be SELECTIVE but comprehensive. Extract the CORE and IMPORTANT keywords. Maximum 40% extra keywords allowed!

                Job Description:
                """

                + jdText;
    }

    private Map<String, String> parseGroqResponse(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        String content = root.path("choices").get(0).path("message").path("content").asText();

        content = content.trim();
        if (content.startsWith("```json")) {
            content = content.substring(7);
        }
        if (content.startsWith("```")) {
            content = content.substring(3);
        }
        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - 3);
        }
        content = content.trim();

        JsonNode resultNode = objectMapper.readTree(content);
        Map<String, String> result = new HashMap<>();
        result.put("skills", resultNode.path("skills").asText("Not specified"));
        result.put("description", resultNode.path("qualification").asText("Not specified"));
        result.put("title", resultNode.path("experience").asText("Not specified"));
        return result;
    }

    private Map<String, String> fallbackExtraction() {
        Map<String, String> result = new HashMap<>();
        result.put("skills", "Extraction failed");
        result.put("description", "Not specified");
        result.put("title", "Not specified");
        return result;
    }
}
