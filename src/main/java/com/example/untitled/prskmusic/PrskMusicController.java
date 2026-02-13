package com.example.untitled.prskmusic;

import com.example.untitled.prskmusic.dto.OptionalPrskMusicRequest;
import com.example.untitled.prskmusic.dto.PrskMusicListResponse;
import com.example.untitled.prskmusic.dto.PrskMusicRequest;
import com.example.untitled.prskmusic.dto.PrskMusicResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/prsk-music")
@Validated
public class PrskMusicController {

    private final PrskMusicService prskMusicService;

    public PrskMusicController(PrskMusicService prskMusicService) {
        this.prskMusicService = prskMusicService;
    }

    // GET /prsk-music : プロセカ楽曲一覧取得 - Get prsk music list
    @GetMapping
    public ResponseEntity<PrskMusicListResponse> getPrskMusicList(
            @RequestParam(required = false, defaultValue = "1")
            @Min(value = 1, message = "Page must be 1 or greater") Integer page,
            @RequestParam(required = false, defaultValue = "20")
            @Min(value = 1, message = "Limit must be at least 1")
            @Max(value = 100, message = "Limit must not exceed 100") Integer limit
    ) {
        Page<PrskMusic> prskMusicPage = prskMusicService.getAllPrskMusic(page - 1, limit, "title", "ASC");
        PrskMusicListResponse response = PrskMusicListResponse.from(prskMusicPage);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // POST /prsk-music : プロセカ楽曲情報の登録 - Register prsk music information
    @PostMapping
    public ResponseEntity<PrskMusicResponse> registerPrskMusic(
            @Valid @RequestBody PrskMusicRequest request
    ) {
        PrskMusic prskMusic = prskMusicService.createPrskMusic(request);
        PrskMusicResponse response = PrskMusicResponse.from(prskMusic);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // PUT /prsk-music/{id} : プロセカ楽曲情報の更新 - Update prsk music information
    @PutMapping("/{id}")
    public ResponseEntity<PrskMusicResponse> updatePrskMusic(
            @PathVariable @Min(value = 1, message = "ID must be 1 or greater.") Long id,
            @Valid @RequestBody OptionalPrskMusicRequest request
    ) {
        PrskMusic prskMusic = prskMusicService.updatePrskMusic(id, request);
        PrskMusicResponse response = PrskMusicResponse.from(prskMusic);
        return ResponseEntity.ok(response);
    }

    // Delete /prsk-music/{id} : プロセカ楽曲情報の削除 - Delete prsk music information
    @DeleteMapping("/{id}")
    public ResponseEntity deletePrskMusic(
            @PathVariable @Min(value = 1, message = "ID must be 1 or greater.") Long id
    ) {
        prskMusicService.deletePrskMusic(id);
        return ResponseEntity.noContent().build();
    }
}
