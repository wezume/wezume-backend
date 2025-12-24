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
                You are an Applicant Tracking System (ATS).

                FILTERING LEVEL: 50% STRICT - Be moderately selective, filter only clearly vague terms.

                TASK:
                Extract from the Job Description:
                1. Skills (using 2-step approach below)
                2. Experience (ONLY if explicitly mentioned - see rules below)

                2-STEP EXTRACTION STRATEGY FOR SKILLS:

                STEP 1: Check if technical/domain skills are EXPLICITLY mentioned in the JD
                - If YES → Extract those exact skills (apply 50% filtering)
                - If NO → Proceed to STEP 2

                STEP 2: Analyze the JD and INFER required skills
                - Look at job title, role description, and responsibilities
                - Identify what specific technical/domain skills would be required
                - Extract the inferred skills (apply 50% filtering)

                SKILLS - EXTRACT (Be Moderately Inclusive):
                ✅ Technical skills: Python, Java, SQL, AWS, React, Docker, Kubernetes
                ✅ Domain skills: Marketing, Business Development, Digital Marketing, Sales, Finance, Data Science, Analytics
                ✅ Tools/platforms: CRM, Salesforce, Google Ads, HubSpot, Tableau, Power BI
                ✅ Frameworks: Spring Boot, Django, TensorFlow
                ✅ Allow borderline terms if they appear relevant to the role

                SKILLS - EXCLUDE ONLY CLEARLY VAGUE TERMS:
                ❌ Very vague single words: Operations, Technology, Management (standalone without context)
                ❌ Pure soft skills: Leadership, Communication, Teamwork (unless part of a technical phrase)
                ❌ Basic office tools: Excel, Word, PowerPoint, MS Office, Google Sheets

                FILTERING GUIDELINES (50% Threshold):
                - When in doubt, INCLUDE the skill (50% means more inclusive)
                - Skip only clearly vague standalone terms
                - Allow domain-specific terms even if broad (e.g., "Marketing", "Sales" are OK)
                - Better to include borderline skills than miss relevant ones

                EXAMPLES:
                - JD: "Required: Python, AWS, Operations, CRM" → Extract: ["Python", "AWS", "CRM"] (skip Operations)
                - JD: "Marketing manager with analytics" → Infer: ["Marketing", "Analytics"]
                - JD: "Technology leader with CRM" → Extract: ["CRM"] (skip Technology)

                EXPERIENCE EXTRACTION RULES (STRICTLY LITERAL):
                ⚠️ CRITICAL: Extract experience ONLY if EXPLICITLY mentioned with numerical years

                VALID FORMATS (Extract these):
                ✅ "1-2 years", "2 years", "3+ years", "5+ years", "3-5 years"
                ✅ "1-2", "2-5", "3+", "5+" (numerical year ranges)
                ✅ "Fresher" or "0 years" or "Entry level with 0-1 years"

                INVALID (Return "Not specified"):
                ❌ "Experience required" (no numbers)
                ❌ "Experienced professional" (no numbers)
                ❌ "Senior level" (no numbers)
                ❌ "3-6 months" (months, not years)
                ❌ "Internship" (no specific years)
                ❌ NO mention of experience at all

                RULE: If there's no explicit numerical year value → ALWAYS return "Not specified"
                DO NOT infer or guess experience from job title or seniority level

                OUTPUT FORMAT (STRICT JSON ONLY):
                {
                  "skills": [],
                  "experience": []
                }

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

        // Convert skills array to comma-separated string
        JsonNode skillsNode = resultNode.path("skills");
        String skills = "Not specified";
        if (skillsNode.isArray() && skillsNode.size() > 0) {
            List<String> skillsList = new ArrayList<>();
            skillsNode.forEach(node -> skillsList.add(node.asText()));
            skills = String.join(", ", skillsList);
        }

        // Convert experience array to comma-separated string
        JsonNode experienceNode = resultNode.path("experience");
        String experience = "Not specified";
        if (experienceNode.isArray() && experienceNode.size() > 0) {
            List<String> expList = new ArrayList<>();
            experienceNode.forEach(node -> expList.add(node.asText()));
            experience = String.join(", ", expList);
        }

        result.put("skills", skills);
        result.put("experience", experience);
        return result;
    }

    private Map<String, String> fallbackExtraction() {
        Map<String, String> result = new HashMap<>();
        result.put("skills", "Extraction failed");
        result.put("experience", "Not specified");
        return result;
    }
}
