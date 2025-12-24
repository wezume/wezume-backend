package com.example.vprofile.linkedIn;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class LinkedInAuthController {

    @Autowired
    private LinkedInAuthService linkedInAuthService;

    @PostMapping("/linkedin")
    public Map<String, String> authenticate(@RequestBody Map<String, String> requestBody) throws LinkedInAuthException {
        String code = requestBody.get("code");
        String clientId = "869zn5otx0ejyt"; // Replace with your LinkedIn client ID
        // Replace with your LinkedIn client secret
        String redirectUri = "https://www.linkedin.com/developers/tools/oauth/redirect"; // Your redirect URI

        return linkedInAuthService.handleLinkedInCallback(code, clientId, clientSecret, redirectUri);
    }
}
