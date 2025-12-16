package com.example.vprofile.linkedIn;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class LinkedInAuthService {

    private final String tokenEndpoint = "https://www.linkedin.com/oauth/v2/accessToken";
    private final String userProfileEndpoint = "https://api.linkedin.com/v2/userinfo"; // Updated endpoint

    public String buildAuthorizationUrl(String clientId, String redirectUri) throws UnsupportedEncodingException {
        return "https://www.linkedin.com/oauth/v2/authorization" +
                "?response_type=code" +
                "&client_id=" + clientId +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, "UTF-8") +
                "&scope=profile%20email%20openid"; 
    }

    @SuppressWarnings("UseSpecificCatch")
    public Map<String, String> handleLinkedInCallback(String code, String clientId, String clientSecret, String redirectUri) throws LinkedInAuthException {
        try {
            String accessToken = exchangeAuthorizationCodeForAccessToken(code, clientId, clientSecret, redirectUri);
            Map<String, String> userProfile = getUserProfile(accessToken);
            System.out.println("User Profile: " + userProfile); // Debugging line
            return userProfile;
        } catch (Exception e) {
            throw new LinkedInAuthException("LinkedIn OAuth flow failed: " + e.getMessage(), e);
        }
    }

    private String exchangeAuthorizationCodeForAccessToken(String authorizationCode, String clientId, String clientSecret, String redirectUri) throws IOException, LinkedInAuthException {
        String requestBody = "grant_type=authorization_code" +
                "&code=" + URLEncoder.encode(authorizationCode, "UTF-8") +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, "UTF-8") +
                "&client_id=" + URLEncoder.encode(clientId, "UTF-8") +
                "&client_secret=" + URLEncoder.encode(clientSecret, "UTF-8");

        HttpURLConnection connection = (HttpURLConnection) new URL(tokenEndpoint).openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            String errorResponse = readErrorStream(connection);
            throw new LinkedInAuthException("Failed to exchange authorization code: HTTP error code " + connection.getResponseCode() + ", Response: " + errorResponse);
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            JSONObject jsonResponse = new JSONObject(response.toString());
            return jsonResponse.getString("access_token");
        }
    }

    private Map<String, String> getUserProfile(String accessToken) throws IOException, LinkedInAuthException {
        String response = makeLinkedInApiCall(userProfileEndpoint, accessToken);
        JSONObject profile = new JSONObject(response);
    
        // Map the fields correctly based on the LinkedIn API response
        String givenName = profile.optString("given_name");
        String familyName = profile.optString("family_name");
        String email = profile.optString("email");
        String pictureUrl = profile.optString("picture");
    
        return Map.of(
            "given_name", givenName,
            "family_name", familyName,
            "email", email,
            "picture", pictureUrl
        );
    }
    

    private String makeLinkedInApiCall(String endpoint, String accessToken) throws IOException, LinkedInAuthException {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);

        int responseCode = connection.getResponseCode();
        StringBuilder response = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        } catch (IOException e) {
            String errorResponse = readErrorStream(connection);
            throw new LinkedInAuthException("Failed to call LinkedIn API: HTTP error code " + responseCode + ", Response: " + errorResponse);
        }

        // Check if response is valid JSON
        if (!isValidJson(response.toString())) {
            throw new LinkedInAuthException("Invalid JSON response: " + response.toString());
        }

        return response.toString();
    }

    private boolean isValidJson(String response) {
        try {
            new JSONObject(response);
        } catch (JSONException ex) {
            return false;
        }
        return true;
    }

    private String readErrorStream(HttpURLConnection connection) throws IOException {
        StringBuilder errorResponse = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "utf-8"))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                errorResponse.append(responseLine.trim());
            }
        }
        return errorResponse.toString();
    }
}
