package com.avengers.yoribogo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "spring.security.oauth2.client")
public class OAuthProperties {

    private Map<String, OAuthClient> registration; // 클라이언트 등록 정보
    private Map<String, OAuthProvider> provider; // 프로바이더 정보

    public Map<String, OAuthClient> getRegistration() {
        return registration;
    }

    public void setRegistration(Map<String, OAuthClient> registration) {
        this.registration = registration;
    }

    public Map<String, OAuthProvider> getProvider() {
        return provider;
    }

    public void setProvider(Map<String, OAuthProvider> provider) {
        this.provider = provider;
    }

    public static class OAuthClient {
        private String clientId;
        private String clientSecret;
        private String redirectUri;

        // Getters and Setters
        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }
    }

    public static class OAuthProvider {
        private String authorizationUri;
        private String tokenUri;
        private String userInfoUri;
        private String userNameAttribute;

        // Getters and Setters
        public String getAuthorizationUri() {
            return authorizationUri;
        }

        public void setAuthorizationUri(String authorizationUri) {
            this.authorizationUri = authorizationUri;
        }

        public String getTokenUri() {
            return tokenUri;
        }

        public void setTokenUri(String tokenUri) {
            this.tokenUri = tokenUri;
        }

        public String getUserInfoUri() {
            return userInfoUri;
        }

        public void setUserInfoUri(String userInfoUri) {
            this.userInfoUri = userInfoUri;
        }

        public String getUserNameAttribute() {
            return userNameAttribute;
        }

        public void setUserNameAttribute(String userNameAttribute) {
            this.userNameAttribute = userNameAttribute;
        }
    }
}
