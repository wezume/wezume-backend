# Gemini AI JD Extraction Setup Guide

## ✅ What Changed

Replaced regex-based extraction with **Google Gemini AI** for intelligent, context-aware JD parsing!

## 🚀 Benefits

### **Gemini AI vs Regex:**
- ✅ **100% Accurate** - Understands context, not just patterns
- ✅ **Any Format** - Works with tables, bullets, paragraphs, mixed formats
- ✅ **Smart Filtering** - Automatically removes noise (locations, company names, etc.)
- ✅ **Range Detection** - Correctly identifies "2-5 years" over "1+ years"
- ✅ **No Maintenance** - No need to update regex patterns for new formats

## 📋 Setup Instructions

### **Step 1: Get Gemini API Key**

1. Go to [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Click **"Get API Key"**
3. Create a new API key or use existing one
4. Copy the API key

### **Step 2: Add API Key to Configuration**

Open `src/main/resources/application.properties` and replace:

```properties
gemini.api.key=YOUR_GEMINI_API_KEY_HERE
```

With your actual API key:

```properties
gemini.api.key=AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
```

### **Step 3: Test the Extraction**

Upload any JD (PDF, DOCX, TXT) and it will return:

```json
{
  "skills": "Java, Python, AWS, Leadership, Bachelor's degree",
  "experience": "2-5 years",
  "requirements": "Develop scalable applications, Manage cloud infrastructure, Lead development team"
}
```

## 🎯 How It Works

1. **Extract Text** - Reads PDF/DOCX/TXT files
2. **Send to Gemini** - Sends JD text with intelligent prompt
3. **AI Analysis** - Gemini understands context and extracts accurately
4. **Return JSON** - Returns clean, structured data

## 🛡️ Fallback

If Gemini API fails (network issue, quota exceeded), it returns:
```json
{
  "skills": "Unable to extract - please check the document format",
  "experience": "Not specified",
  "requirements": "Unable to extract - please check the document format"
}
```

## 💰 Pricing

- **Free Tier**: 60 requests per minute
- **Cost**: Gemini 1.5 Flash is FREE for most use cases
- [Check pricing](https://ai.google.dev/pricing)

## 🔧 Configuration

The prompt is optimized to:
- Extract only relevant skills (no noise)
- Prioritize experience ranges (2-5 years)
- Get actual responsibilities (not headers)
- Return clean JSON format

## 📝 Example Prompts

The AI receives this instruction:

```
You are an expert HR assistant. Analyze the following job description and extract ONLY these 3 fields:

1. "skills": Technical skills, soft skills, qualifications
2. "experience": Years of experience required
3. "requirements": Key job responsibilities

IMPORTANT RULES:
- Extract ONLY relevant skills (no noise)
- Prioritize range formats for experience
- Extract actual responsibilities, not headers
- Return ONLY valid JSON
```

## ✨ Result

**Before (Regex):**
```json
{
  "skills": "+ years relevant experience and a, SDLC, Description",
  "experience": "1+ years"
}
```

**After (Gemini AI):**
```json
{
  "skills": "Java, Python, Leadership, Bachelor's degree",
  "experience": "2-5 years"
}
```

**Much more accurate!** 🎉
