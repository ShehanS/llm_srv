//package com.shehan.llmsvr.config;
//
//import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
//import com.google.api.client.json.gson.GsonFactory;
//import com.google.api.services.drive.Drive;
//import com.google.api.services.drive.DriveScopes;
//import com.google.auth.http.HttpCredentialsAdapter;
//import com.google.auth.oauth2.UserCredentials;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.util.Collections;
//
//@Configuration
//public class GoogleDriveConfig {
//
//    @Value("${google.client-id}")
//    private String clientId;
//
//    @Value("${google.client-secret}")
//    private String clientSecret;
//
//    @Value("${google.refresh-token}")
//    private String refreshToken;
//
//    @Bean
//    public Drive googleDriveService() throws Exception {
//        UserCredentials credentials = UserCredentials.newBuilder()
//                .setClientId(clientId)
//                .setClientSecret(clientSecret)
//                .setRefreshToken(refreshToken)
//                .build();
//
//        return new Drive.Builder(
//                GoogleNetHttpTransport.newTrustedTransport(),
//                GsonFactory.getDefaultInstance(),
//                new HttpCredentialsAdapter(credentials.createScoped(Collections.singleton(DriveScopes.DRIVE_FILE)))
//        )
//                .setApplicationName("LLM-SVR")
//                .build();
//    }
//}
