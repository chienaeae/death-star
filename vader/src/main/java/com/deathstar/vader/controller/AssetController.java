package com.deathstar.vader.controller;

import com.deathstar.vader.api.AssetsApi;
import com.deathstar.vader.dto.generated.PresignedUrlRequest;
import com.deathstar.vader.dto.generated.PresignedUrlResponse;
import com.deathstar.vader.asset.service.AssetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.net.URL;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AssetController implements AssetsApi {

    private final AssetService assetService;

    @Override
    public ResponseEntity<PresignedUrlResponse> assetsUploadUrlPost(PresignedUrlRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // Ensure user is authenticated
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UUID userId = UUID.fromString(auth.getName()); // Principle contains String UUID in our IAM logic

        URL presignedUrl = assetService.generateUploadUrl(
                userId,
                request.getFilename(),
                request.getContentType()
        );

        PresignedUrlResponse response = new PresignedUrlResponse();
        response.setUploadUrl(presignedUrl.toString());
        response.setMethod("PUT");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
