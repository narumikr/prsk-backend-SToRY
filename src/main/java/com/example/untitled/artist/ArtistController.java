package com.example.untitled.artist;

import com.example.untitled.artist.dto.ArtistListResponse;
import com.example.untitled.artist.dto.ArtistRequest;
import com.example.untitled.artist.dto.ArtistResponse;
import com.example.untitled.artist.dto.OptionalArtistRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/artists")
@Validated
public class ArtistController {

    private final ArtistService artistService;

    // GET /artists : アーティスト一覧取得 - Get artists list
    @GetMapping
    public ResponseEntity<ArtistListResponse> getArtistsList(
            @RequestParam(required = false, defaultValue = "1")
            @Min(value = 1, message = "Page must be 1 or greater") Integer page,
            @RequestParam(required = false, defaultValue = "20")
            @Min(value = 1, message = "Limit must be at least 1")
            @Max(value = 100, message = "Limit must not exceed 100") Integer limit
    ) {
        Page<Artist> artistPage = artistService.getAllArtists(page - 1, limit, "artistName", "ASC");
        ArtistListResponse response = ArtistListResponse.from(artistPage);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // POST /artists : アーティスト情報の登録 - Register artist information
    @PostMapping
    public ResponseEntity<ArtistResponse> registerArtist(
            @Valid @RequestBody ArtistRequest request
    ) {
        Artist artist = artistService.createArtist(request);
        ArtistResponse response = ArtistResponse.from(artist);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // PUT /artists/{id} : アーティスト情報の更新 - Update artist information
    @PutMapping("/{id}")
    public ResponseEntity<ArtistResponse> updateArtist(
            @PathVariable @Min(value = 1, message = "ID must be 1 or greater.") Long id,
            @Valid @RequestBody OptionalArtistRequest request
    ) {
        Artist artist = artistService.updateArtist(id, request);
        ArtistResponse response = ArtistResponse.from(artist);
        return ResponseEntity.ok(response);
    }

    // DELETE /artists/{id} : アーティスト情報の削除 - Delete artist information
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArtist(
            @PathVariable @Min(value = 1, message = "ID must be 1 or greater.") Long id
    ) {
        artistService.deleteArtist(id);
        return ResponseEntity.noContent().build();
    }
}
