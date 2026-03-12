package com.deathstar.vader.asset.service;

import com.deathstar.vader.asset.Asset;
import com.deathstar.vader.asset.AssetReference;
import com.deathstar.vader.asset.AssetStatus;
import com.deathstar.vader.asset.repository.AssetReferenceRepository;
import com.deathstar.vader.asset.repository.AssetRepository;
import com.deathstar.vader.asset.storage.BlobStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AssetServiceTest {

    @Mock
    private BlobStorage blobStorage;
    @Mock
    private AssetRepository assetRepository;
    @Mock
    private AssetReferenceRepository assetReferenceRepository;

    private AssetService assetService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        assetService = new AssetService(blobStorage, assetRepository, assetReferenceRepository);
    }

    @Test
    void testGenerateUploadUrl_CreatesInitAssetAndReturnsPresignedUrl() throws MalformedURLException {
        UUID userId = UUID.randomUUID();
        String filename = "my doc.pdf";
        String contentType = "application/pdf";
        URL fakeUrl = new URL("http://s3/bucket/key");

        when(blobStorage.generatePresignedUploadUrl(anyString(), eq(contentType), any(Duration.class)))
                .thenReturn(fakeUrl);

        URL resultUrl = assetService.generateUploadUrl(userId, filename, contentType);

        assertThat(resultUrl).isEqualTo(fakeUrl);

        ArgumentCaptor<Asset> assetCaptor = ArgumentCaptor.forClass(Asset.class);
        verify(assetRepository, times(1)).save(assetCaptor.capture());

        Asset savedAsset = assetCaptor.getValue();
        assertThat(savedAsset.getOwnerId()).isEqualTo(userId);
        assertThat(savedAsset.getStatus()).isEqualTo(AssetStatus.INIT);
        assertThat(savedAsset.getMimeType()).isEqualTo(contentType);
        assertThat(savedAsset.getRefCount()).isEqualTo(0);
        // Ensure sanitize works
        assertThat(savedAsset.getS3Key()).contains("my_doc.pdf"); 
    }

    @Test
    void testLinkAsset_IncrementsRefCountAndSavesReference() {
        UUID assetId = UUID.randomUUID();
        Asset asset = new Asset();
        asset.setId(assetId);
        asset.setStatus(AssetStatus.UPLOADED);
        asset.setRefCount(0);

        when(assetRepository.findByIdAndNotDeleted(assetId)).thenReturn(Optional.of(asset));

        assetService.linkAsset(assetId, "DOCUMENT", "DOC-123");

        verify(assetReferenceRepository, times(1)).save(any(AssetReference.class));
        assertThat(asset.getRefCount()).isEqualTo(1);
        assertThat(asset.getStatus()).isEqualTo(AssetStatus.LINKED);
    }
}
