package com.example.untitled.e2e;

import com.example.untitled.artist.dto.ArtistRequest;
import com.example.untitled.artist.dto.ArtistResponse;
import com.example.untitled.common.dto.ErrorResponse;
import com.example.untitled.prskmusic.dto.OptionalPrskMusicRequest;
import com.example.untitled.prskmusic.dto.PrskMusicListResponse;
import com.example.untitled.prskmusic.dto.PrskMusicRequest;
import com.example.untitled.prskmusic.dto.PrskMusicResponse;
import com.example.untitled.prskmusic.enums.MusicType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PrskMusic E2E Tests")
class PrskMusicE2ETest extends E2ETestBase {

    private static final String PRSK_MUSIC_PATH = "/prsk-music";
    private static final String ARTISTS_PATH = "/artists";

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private String uniqueTitle() {
        return "Title-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private ArtistResponse createTestArtist() {
        ArtistRequest request = new ArtistRequest();
        request.setArtistName("Artist-" + UUID.randomUUID().toString().substring(0, 8));

        ResponseEntity<ArtistResponse> response = restTemplate.postForEntity(
                getBaseUrl() + ARTISTS_PATH,
                request,
                ArtistResponse.class
        );
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        return response.getBody();
    }

    private PrskMusicResponse createPrskMusic(String title, Long artistId, MusicType musicType, String youtubeLink) {
        PrskMusicRequest request = new PrskMusicRequest();
        request.setTitle(title);
        request.setArtistId(artistId);
        request.setMusicType(musicType);
        request.setYoutubeLink(youtubeLink);

        ResponseEntity<PrskMusicResponse> response = restTemplate.postForEntity(
                getBaseUrl() + PRSK_MUSIC_PATH,
                request,
                PrskMusicResponse.class
        );
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        return response.getBody();
    }

    private PrskMusicResponse createPrskMusic(String title, Long artistId) {
        return createPrskMusic(title, artistId, MusicType.ORIGINAL, "https://youtube.com/test");
    }

    // ========================================================================
    // GET /prsk-music - List PrskMusic
    // ========================================================================

    @Nested
    @DisplayName("GET /prsk-music")
    class GetPrskMusicList {

        @Test
        @DisplayName("Success - returns list with created prsk music")
        void getPrskMusicListSuccess() {
            // Arrange: Create test data
            ArtistResponse artist = createTestArtist();
            createPrskMusic(uniqueTitle(), artist.getId(), MusicType.ORIGINAL, "https://youtube.com/1");
            createPrskMusic(uniqueTitle(), artist.getId(), MusicType.THREE_D_MV, "https://youtube.com/2");

            // Act
            ResponseEntity<PrskMusicListResponse> response = restTemplate.getForEntity(
                    getBaseUrl() + PRSK_MUSIC_PATH,
                    PrskMusicListResponse.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertNotNull(response.getBody().getItems());
            assertNotNull(response.getBody().getMeta());
            assertTrue(response.getBody().getItems().size() >= 2);
        }

        @Test
        @DisplayName("Success - returns paginated results with page and limit params")
        void getPrskMusicListSuccess_withPagination() {
            // Arrange: Create 3 prsk music entries
            ArtistResponse artist = createTestArtist();
            for (int i = 0; i < 3; i++) {
                createPrskMusic(uniqueTitle(), artist.getId());
            }

            // Act: Get page 1 with limit 2
            ResponseEntity<PrskMusicListResponse> response = restTemplate.getForEntity(
                    getBaseUrl() + PRSK_MUSIC_PATH + "?page=1&limit=2",
                    PrskMusicListResponse.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertNotNull(response.getBody().getMeta());
            assertEquals(2, response.getBody().getMeta().getLimit());
            assertTrue(response.getBody().getItems().size() <= 2);
        }
    }

    // ========================================================================
    // POST /prsk-music - Create PrskMusic
    // ========================================================================

    @Nested
    @DisplayName("POST /prsk-music")
    class CreatePrskMusic {

        @Test
        @DisplayName("Success - creates prsk music with all fields")
        void createPrskMusicSuccess() {
            // Arrange
            ArtistResponse artist = createTestArtist();
            PrskMusicRequest request = new PrskMusicRequest();
            request.setTitle(uniqueTitle());
            request.setArtistId(artist.getId());
            request.setMusicType(MusicType.ORIGINAL);
            request.setSpecially(true);
            request.setLyricsName("Test Lyricist");
            request.setMusicName("Test Composer");
            request.setFeaturing("Feat.ABC");
            request.setYoutubeLink("https://youtube.com/test");

            // Act
            ResponseEntity<PrskMusicResponse> response = restTemplate.postForEntity(
                    getBaseUrl() + PRSK_MUSIC_PATH,
                    request,
                    PrskMusicResponse.class
            );

            // Assert
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertNotNull(response.getBody().getId());
            assertEquals(request.getTitle(), response.getBody().getTitle());
            assertEquals(MusicType.ORIGINAL, response.getBody().getMusicType());
            assertEquals(true, response.getBody().getSpecially());
            assertEquals("Test Lyricist", response.getBody().getLyricsName());
            assertNotNull(response.getBody().getAuditInfo());
        }

        @Test
        @DisplayName("Success - creates prsk music with required fields only")
        void createPrskMusicSuccess_withRequiredFieldsOnly() {
            // Arrange
            ArtistResponse artist = createTestArtist();
            PrskMusicRequest request = new PrskMusicRequest();
            request.setTitle(uniqueTitle());
            request.setArtistId(artist.getId());
            request.setMusicType(MusicType.THREE_D_MV);
            request.setYoutubeLink("https://youtube.com/test");

            // Act
            ResponseEntity<PrskMusicResponse> response = restTemplate.postForEntity(
                    getBaseUrl() + PRSK_MUSIC_PATH,
                    request,
                    PrskMusicResponse.class
            );

            // Assert
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertNull(response.getBody().getLyricsName());
            assertNull(response.getBody().getMusicName());
            assertNull(response.getBody().getFeaturing());
        }

        @Test
        @DisplayName("Error - 409 Conflict when title and musicType combination already exists")
        void createPrskMusicError_withConflict() {
            // Arrange: Create prsk music first
            ArtistResponse artist = createTestArtist();
            String existingTitle = uniqueTitle();
            createPrskMusic(existingTitle, artist.getId(), MusicType.ORIGINAL, "https://youtube.com/test");

            // Try to create another with the same title + musicType combination
            PrskMusicRequest duplicateRequest = new PrskMusicRequest();
            duplicateRequest.setTitle(existingTitle);
            duplicateRequest.setArtistId(artist.getId());
            duplicateRequest.setMusicType(MusicType.ORIGINAL);
            duplicateRequest.setYoutubeLink("https://youtube.com/test2");

            // Act
            ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                    getBaseUrl() + PRSK_MUSIC_PATH,
                    duplicateRequest,
                    ErrorResponse.class
            );

            // Assert
            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(409, response.getBody().getStatusCode());
        }

        @Test
        @DisplayName("Error - 400 Bad Request when title is blank")
        void createPrskMusicError_withBadRequest_blankTitle() {
            // Arrange
            ArtistResponse artist = createTestArtist();
            PrskMusicRequest request = new PrskMusicRequest();
            request.setTitle("");
            request.setArtistId(artist.getId());
            request.setMusicType(MusicType.ORIGINAL);
            request.setYoutubeLink("https://youtube.com/test");

            // Act
            ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                    getBaseUrl() + PRSK_MUSIC_PATH,
                    request,
                    ErrorResponse.class
            );

            // Assert
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(400, response.getBody().getStatusCode());
        }

        @Test
        @DisplayName("Error - 400 Bad Request when title is null")
        void createPrskMusicError_withBadRequest_nullTitle() {
            // Arrange
            ArtistResponse artist = createTestArtist();
            PrskMusicRequest request = new PrskMusicRequest();
            request.setTitle(null);
            request.setArtistId(artist.getId());
            request.setMusicType(MusicType.ORIGINAL);
            request.setYoutubeLink("https://youtube.com/test");

            // Act
            ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                    getBaseUrl() + PRSK_MUSIC_PATH,
                    request,
                    ErrorResponse.class
            );

            // Assert
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(400, response.getBody().getStatusCode());
        }

        @Test
        @DisplayName("Error - 400 Bad Request when title exceeds max length")
        void createPrskMusicError_withBadRequest_titleTooLong() {
            // Arrange: title max is 30 characters
            ArtistResponse artist = createTestArtist();
            PrskMusicRequest request = new PrskMusicRequest();
            request.setTitle("T".repeat(31));
            request.setArtistId(artist.getId());
            request.setMusicType(MusicType.ORIGINAL);
            request.setYoutubeLink("https://youtube.com/test");

            // Act
            ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                    getBaseUrl() + PRSK_MUSIC_PATH,
                    request,
                    ErrorResponse.class
            );

            // Assert
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }

    // ========================================================================
    // PUT /prsk-music/{id} - Update PrskMusic
    // ========================================================================

    @Nested
    @DisplayName("PUT /prsk-music/{id}")
    class UpdatePrskMusic {

        @Test
        @DisplayName("Success - updates all fields")
        void updatePrskMusicSuccess() {
            // Arrange: Create prsk music
            ArtistResponse artist = createTestArtist();
            PrskMusicResponse created = createPrskMusic(uniqueTitle(), artist.getId(), MusicType.ORIGINAL, "https://youtube.com/old");

            ArtistResponse newArtist = createTestArtist();
            OptionalPrskMusicRequest updateRequest = new OptionalPrskMusicRequest();
            updateRequest.setTitle(uniqueTitle());
            updateRequest.setArtistId(newArtist.getId());
            updateRequest.setMusicType(MusicType.THREE_D_MV);
            updateRequest.setLyricsName("New Lyricist");
            updateRequest.setYoutubeLink("https://youtube.com/new");

            // Act
            ResponseEntity<PrskMusicResponse> response = restTemplate.exchange(
                    getBaseUrl() + PRSK_MUSIC_PATH + "/" + created.getId(),
                    HttpMethod.PUT,
                    new HttpEntity<>(updateRequest),
                    PrskMusicResponse.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(created.getId(), response.getBody().getId());
            assertEquals(updateRequest.getTitle(), response.getBody().getTitle());
            assertEquals(MusicType.THREE_D_MV, response.getBody().getMusicType());
            assertEquals("New Lyricist", response.getBody().getLyricsName());
        }

        @Test
        @DisplayName("Success - updates partial fields (title only)")
        void updatePrskMusicSuccess_partialUpdate() {
            // Arrange: Create prsk music
            ArtistResponse artist = createTestArtist();
            PrskMusicResponse created = createPrskMusic(uniqueTitle(), artist.getId(), MusicType.ORIGINAL, "https://youtube.com/original");

            OptionalPrskMusicRequest updateRequest = new OptionalPrskMusicRequest();
            updateRequest.setTitle(uniqueTitle());

            // Act
            ResponseEntity<PrskMusicResponse> response = restTemplate.exchange(
                    getBaseUrl() + PRSK_MUSIC_PATH + "/" + created.getId(),
                    HttpMethod.PUT,
                    new HttpEntity<>(updateRequest),
                    PrskMusicResponse.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(updateRequest.getTitle(), response.getBody().getTitle());
            // Original musicType should be preserved
            assertEquals(MusicType.ORIGINAL, response.getBody().getMusicType());
        }

        @Test
        @DisplayName("Error - 404 Not Found when ID does not exist")
        void updatePrskMusicError_withNotFound() {
            // Arrange
            OptionalPrskMusicRequest updateRequest = new OptionalPrskMusicRequest();
            updateRequest.setTitle(uniqueTitle());

            // Act
            ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                    getBaseUrl() + PRSK_MUSIC_PATH + "/" + Long.MAX_VALUE,
                    HttpMethod.PUT,
                    new HttpEntity<>(updateRequest),
                    ErrorResponse.class
            );

            // Assert
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(404, response.getBody().getStatusCode());
        }

        @Test
        @DisplayName("Error - 409 Conflict when title and musicType combination already exists")
        void updatePrskMusicError_withConflict() {
            // Arrange: Create two prsk music entries
            ArtistResponse artist = createTestArtist();
            String existingTitle = uniqueTitle();
            createPrskMusic(existingTitle, artist.getId(), MusicType.ORIGINAL, "https://youtube.com/existing");
            PrskMusicResponse toUpdate = createPrskMusic(uniqueTitle(), artist.getId(), MusicType.THREE_D_MV, "https://youtube.com/update");

            // Try to update second music with first music's title + musicType
            OptionalPrskMusicRequest updateRequest = new OptionalPrskMusicRequest();
            updateRequest.setTitle(existingTitle);
            updateRequest.setMusicType(MusicType.ORIGINAL);

            // Act
            ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                    getBaseUrl() + PRSK_MUSIC_PATH + "/" + toUpdate.getId(),
                    HttpMethod.PUT,
                    new HttpEntity<>(updateRequest),
                    ErrorResponse.class
            );

            // Assert
            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(409, response.getBody().getStatusCode());
        }

        @Test
        @DisplayName("Error - 400 Bad Request when title exceeds max length")
        void updatePrskMusicError_withBadRequest_titleTooLong() {
            // Arrange: Create a prsk music
            ArtistResponse artist = createTestArtist();
            PrskMusicResponse created = createPrskMusic(uniqueTitle(), artist.getId());

            OptionalPrskMusicRequest updateRequest = new OptionalPrskMusicRequest();
            updateRequest.setTitle("T".repeat(31));

            // Act
            ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                    getBaseUrl() + PRSK_MUSIC_PATH + "/" + created.getId(),
                    HttpMethod.PUT,
                    new HttpEntity<>(updateRequest),
                    ErrorResponse.class
            );

            // Assert
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }

    // ========================================================================
    // DELETE /prsk-music/{id} - Delete PrskMusic
    // ========================================================================

    @Nested
    @DisplayName("DELETE /prsk-music/{id}")
    class DeletePrskMusic {

        @Test
        @DisplayName("Success - deletes prsk music and returns 204")
        void deletePrskMusicSuccess() {
            // Arrange: Create a prsk music
            ArtistResponse artist = createTestArtist();
            PrskMusicResponse created = createPrskMusic(uniqueTitle(), artist.getId());

            // Act
            ResponseEntity<Void> response = restTemplate.exchange(
                    getBaseUrl() + PRSK_MUSIC_PATH + "/" + created.getId(),
                    HttpMethod.DELETE,
                    null,
                    Void.class
            );

            // Assert
            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

            // Verify: Try to delete again should return 404 (soft-deleted)
            ResponseEntity<ErrorResponse> verifyResponse = restTemplate.exchange(
                    getBaseUrl() + PRSK_MUSIC_PATH + "/" + created.getId(),
                    HttpMethod.DELETE,
                    null,
                    ErrorResponse.class
            );
            assertEquals(HttpStatus.NOT_FOUND, verifyResponse.getStatusCode());
        }

        @Test
        @DisplayName("Error - 404 Not Found when ID does not exist")
        void deletePrskMusicError_withNotFound() {
            // Act
            ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                    getBaseUrl() + PRSK_MUSIC_PATH + "/" + Long.MAX_VALUE,
                    HttpMethod.DELETE,
                    null,
                    ErrorResponse.class
            );

            // Assert
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(404, response.getBody().getStatusCode());
        }

        @Test
        @DisplayName("Success - deleted prsk music does not appear in list")
        void deletePrskMusicSuccess_notInList() {
            // Arrange: Create and delete a prsk music
            ArtistResponse artist = createTestArtist();
            PrskMusicResponse created = createPrskMusic(uniqueTitle(), artist.getId());

            restTemplate.exchange(
                    getBaseUrl() + PRSK_MUSIC_PATH + "/" + created.getId(),
                    HttpMethod.DELETE,
                    null,
                    Void.class
            );

            // Act: Get list
            ResponseEntity<PrskMusicListResponse> listResponse = restTemplate.getForEntity(
                    getBaseUrl() + PRSK_MUSIC_PATH,
                    PrskMusicListResponse.class
            );

            // Assert: Deleted prsk music should not be in the list
            assertEquals(HttpStatus.OK, listResponse.getStatusCode());
            assertNotNull(listResponse.getBody());

            boolean musicFound = listResponse.getBody().getItems().stream()
                    .anyMatch(m -> m.getId().equals(created.getId()));
            assertFalse(musicFound, "Deleted prsk music should not appear in list");
        }
    }
}
