package com.example.untitled.prskmusic;

import com.example.untitled.artist.Artist;
import com.example.untitled.artist.ArtistRepository;
import com.example.untitled.common.exception.DuplicationResourceException;
import com.example.untitled.prskmusic.dto.OptionalPrskMusicRequest;
import com.example.untitled.prskmusic.dto.PrskMusicRequest;
import com.example.untitled.prskmusic.enums.MusicType;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PrskMusicServiceTest {

    @Mock
    private PrskMusicRepository prskMusicRepository;

    @Mock
    private ArtistRepository artistRepository;

    @InjectMocks
    private PrskMusicService prskMusicService;

    private Artist createArtist(Long id, String artistName) {
        Artist artist = new Artist();
        artist.setId(id);
        artist.setArtistName(artistName);
        artist.setUnitName("Test Unit");
        artist.setContent("Test Content");
        return artist;
    }

    private PrskMusic createPrskMusic(Long id, String title, MusicType musicType, Artist artist) {
        PrskMusic prskMusic = new PrskMusic();
        prskMusic.setId(id);
        prskMusic.setTitle(title);
        prskMusic.setArtist(artist);
        prskMusic.setMusicType(musicType);
        prskMusic.setYoutubeLink("https://youtube.com/test");
        return prskMusic;
    }

    /**
     * createPrskMusic : 正常系 - プロセカ楽曲が正常に作成される
     */
    @Test
    public void createPrskMusicSuccess() {
        Artist artist = createArtist(1L, "Test Artist");

        PrskMusicRequest request = new PrskMusicRequest();
        request.setTitle("Test Title");
        request.setArtistId(1L);
        request.setMusicType(MusicType.ORIGINAL);
        request.setYoutubeLink("https://youtube.com/test");

        PrskMusic createdMusic = createPrskMusic(1L, "Test Title", MusicType.ORIGINAL, artist);

        when(prskMusicRepository.findByTitleAndMusicTypeAndIsDeleted("Test Title", MusicType.ORIGINAL, false))
                .thenReturn(Optional.empty());
        when(artistRepository.findByIdAndIsDeleted(1L, false)).thenReturn(Optional.of(artist));
        when(prskMusicRepository.save(any(PrskMusic.class))).thenReturn(createdMusic);

        PrskMusic result = prskMusicService.createPrskMusic(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Title", result.getTitle());
        assertEquals(MusicType.ORIGINAL, result.getMusicType());

        verify(prskMusicRepository, times(1)).findByTitleAndMusicTypeAndIsDeleted("Test Title", MusicType.ORIGINAL, false);
        verify(artistRepository, times(1)).findByIdAndIsDeleted(1L, false);
        verify(prskMusicRepository, times(1)).save(any(PrskMusic.class));
    }

    /**
     * createPrskMusic : 異常系 - title + musicType が重複しており、例外をスローする
     */
    @Test
    public void createPrskMusicError_withDuplication() {
        Artist artist = createArtist(1L, "Test Artist");
        PrskMusic existingMusic = createPrskMusic(1L, "Duplicate Title", MusicType.ORIGINAL, artist);

        PrskMusicRequest request = new PrskMusicRequest();
        request.setTitle("Duplicate Title");
        request.setArtistId(1L);
        request.setMusicType(MusicType.ORIGINAL);
        request.setYoutubeLink("https://youtube.com/test");

        when(prskMusicRepository.findByTitleAndMusicTypeAndIsDeleted("Duplicate Title", MusicType.ORIGINAL, false))
                .thenReturn(Optional.of(existingMusic));

        DuplicationResourceException exception = assertThrows(
                DuplicationResourceException.class,
                () -> prskMusicService.createPrskMusic(request)
        );

        assertNotNull(exception.getDetails());
        assertEquals("Title and MusicType", exception.getDetails().get(0).getField());

        verify(prskMusicRepository, times(1)).findByTitleAndMusicTypeAndIsDeleted("Duplicate Title", MusicType.ORIGINAL, false);
        verify(prskMusicRepository, never()).save(any(PrskMusic.class));
    }

    /**
     * createPrskMusic : 異常系 - アーティストが存在しない
     */
    @Test
    public void createPrskMusicError_withArtistNotFound() {
        PrskMusicRequest request = new PrskMusicRequest();
        request.setTitle("Test Title");
        request.setArtistId(999L);
        request.setMusicType(MusicType.ORIGINAL);
        request.setYoutubeLink("https://youtube.com/test");

        when(prskMusicRepository.findByTitleAndMusicTypeAndIsDeleted("Test Title", MusicType.ORIGINAL, false))
                .thenReturn(Optional.empty());
        when(artistRepository.findByIdAndIsDeleted(999L, false)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> prskMusicService.createPrskMusic(request)
        );

        assertEquals("Artist not found for id: 999", exception.getMessage());

        verify(prskMusicRepository, never()).save(any(PrskMusic.class));
    }

    /**
     * getAllPrskMusic : 正常系 - プロセカ楽曲一覧を取得（ASC順）
     */
    @Test
    public void getAllPrskMusicSuccess_ASC() {
        Artist artist = createArtist(1L, "Test Artist");
        PrskMusic music1 = createPrskMusic(1L, "Music A", MusicType.ORIGINAL, artist);
        PrskMusic music2 = createPrskMusic(2L, "Music B", MusicType.THREE_D_MV, artist);

        Page<PrskMusic> musicPage = new PageImpl<>(
                Arrays.asList(music1, music2),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "title")),
                2
        );

        when(prskMusicRepository.findByIsDeleted(eq(false), any(Pageable.class))).thenReturn(musicPage);

        Page<PrskMusic> result = prskMusicService.getAllPrskMusic(0, 20, "title", "ASC");

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertEquals("Music A", result.getContent().get(0).getTitle());
        assertEquals("Music B", result.getContent().get(1).getTitle());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(prskMusicRepository, times(1)).findByIsDeleted(eq(false), pageableCaptor.capture());

        Pageable capturedPageable = pageableCaptor.getValue();
        assertEquals(0, capturedPageable.getPageNumber());
        assertEquals(20, capturedPageable.getPageSize());
        assertEquals(Sort.Direction.ASC, capturedPageable.getSort().getOrderFor("title").getDirection());
    }

    /**
     * getAllPrskMusic : 正常系 - プロセカ楽曲一覧を取得（DESC順）
     */
    @Test
    public void getAllPrskMusicSuccess_DESC() {
        Artist artist = createArtist(1L, "Test Artist");
        PrskMusic music1 = createPrskMusic(1L, "Music B", MusicType.ORIGINAL, artist);
        PrskMusic music2 = createPrskMusic(2L, "Music A", MusicType.ORIGINAL, artist);

        Page<PrskMusic> musicPage = new PageImpl<>(
                Arrays.asList(music1, music2),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "title")),
                2
        );

        when(prskMusicRepository.findByIsDeleted(eq(false), any(Pageable.class))).thenReturn(musicPage);

        Page<PrskMusic> result = prskMusicService.getAllPrskMusic(0, 20, "title", "DESC");

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals("Music B", result.getContent().get(0).getTitle());
        assertEquals("Music A", result.getContent().get(1).getTitle());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(prskMusicRepository, times(1)).findByIsDeleted(eq(false), pageableCaptor.capture());

        Pageable capturedPageable = pageableCaptor.getValue();
        assertEquals(Sort.Direction.DESC, capturedPageable.getSort().getOrderFor("title").getDirection());
    }

    /**
     * getAllPrskMusic : 正常系 - 空のリストを返す
     */
    @Test
    public void getAllPrskMusicSuccess_EmptyList() {
        Page<PrskMusic> emptyPage = new PageImpl<>(
                List.of(),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "title")),
                0
        );

        when(prskMusicRepository.findByIsDeleted(eq(false), any(Pageable.class))).thenReturn(emptyPage);

        Page<PrskMusic> result = prskMusicService.getAllPrskMusic(0, 20, "title", "ASC");

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());

        verify(prskMusicRepository, times(1)).findByIsDeleted(eq(false), any(Pageable.class));
    }

    /**
     * updatePrskMusic : 正常系 - 全フィールド更新
     */
    @Test
    public void updatePrskMusicSuccess_AllFields() {
        Artist originalArtist = createArtist(1L, "Original Artist");
        Artist newArtist = createArtist(2L, "New Artist");
        PrskMusic existingMusic = createPrskMusic(1L, "Original Title", MusicType.ORIGINAL, originalArtist);

        OptionalPrskMusicRequest request = new OptionalPrskMusicRequest();
        request.setTitle("Updated Title");
        request.setArtistId(2L);
        request.setMusicType(MusicType.THREE_D_MV);
        request.setLyricsName("New Lyricist");
        request.setYoutubeLink("https://youtube.com/updated");

        when(prskMusicRepository.findByIdAndIsDeleted(1L, false)).thenReturn(Optional.of(existingMusic));
        when(prskMusicRepository.findByTitleAndMusicTypeAndIsDeleted("Updated Title", MusicType.THREE_D_MV, false))
                .thenReturn(Optional.empty());
        when(artistRepository.findByIdAndIsDeleted(2L, false)).thenReturn(Optional.of(newArtist));
        when(prskMusicRepository.save(any(PrskMusic.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PrskMusic result = prskMusicService.updatePrskMusic(1L, request);

        assertNotNull(result);
        assertEquals("Updated Title", result.getTitle());
        assertEquals(MusicType.THREE_D_MV, result.getMusicType());
        assertEquals("New Artist", result.getArtist().getArtistName());
        assertEquals("New Lyricist", result.getLyricsName());

        verify(prskMusicRepository, times(1)).findByIdAndIsDeleted(1L, false);
        verify(prskMusicRepository, times(1)).findByTitleAndMusicTypeAndIsDeleted("Updated Title", MusicType.THREE_D_MV, false);
        verify(artistRepository, times(1)).findByIdAndIsDeleted(2L, false);
        verify(prskMusicRepository, times(1)).save(any(PrskMusic.class));
    }

    /**
     * updatePrskMusic : 正常系 - 一部フィールド更新（titleのみ）
     */
    @Test
    public void updatePrskMusicSuccess_PartialFields() {
        Artist artist = createArtist(1L, "Test Artist");
        PrskMusic existingMusic = createPrskMusic(1L, "Original Title", MusicType.ORIGINAL, artist);
        existingMusic.setLyricsName("Original Lyricist");

        OptionalPrskMusicRequest request = new OptionalPrskMusicRequest();
        request.setTitle("Updated Title");

        when(prskMusicRepository.findByIdAndIsDeleted(1L, false)).thenReturn(Optional.of(existingMusic));
        when(prskMusicRepository.findByTitleAndMusicTypeAndIsDeleted("Updated Title", MusicType.ORIGINAL, false))
                .thenReturn(Optional.empty());
        when(prskMusicRepository.save(any(PrskMusic.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PrskMusic result = prskMusicService.updatePrskMusic(1L, request);

        assertNotNull(result);
        assertEquals("Updated Title", result.getTitle());
        assertEquals(MusicType.ORIGINAL, result.getMusicType());
        assertEquals("Original Lyricist", result.getLyricsName());

        verify(prskMusicRepository, times(1)).findByIdAndIsDeleted(1L, false);
        verify(prskMusicRepository, times(1)).findByTitleAndMusicTypeAndIsDeleted("Updated Title", MusicType.ORIGINAL, false);
        verify(prskMusicRepository, times(1)).save(any(PrskMusic.class));
    }

    /**
     * updatePrskMusic : 正常系 - title と musicType が変更されない場合（重複チェックがスキップされる）
     */
    @Test
    public void updatePrskMusicSuccess_SameTitleAndMusicType() {
        Artist artist = createArtist(1L, "Test Artist");
        PrskMusic existingMusic = createPrskMusic(1L, "Test Title", MusicType.ORIGINAL, artist);
        existingMusic.setLyricsName("Original Lyricist");

        OptionalPrskMusicRequest request = new OptionalPrskMusicRequest();
        request.setTitle("Test Title");
        request.setMusicType(MusicType.ORIGINAL);
        request.setLyricsName("Updated Lyricist");

        when(prskMusicRepository.findByIdAndIsDeleted(1L, false)).thenReturn(Optional.of(existingMusic));
        when(prskMusicRepository.save(any(PrskMusic.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PrskMusic result = prskMusicService.updatePrskMusic(1L, request);

        assertNotNull(result);
        assertEquals("Test Title", result.getTitle());
        assertEquals(MusicType.ORIGINAL, result.getMusicType());
        assertEquals("Updated Lyricist", result.getLyricsName());

        verify(prskMusicRepository, times(1)).findByIdAndIsDeleted(1L, false);
        verify(prskMusicRepository, never()).findByTitleAndMusicTypeAndIsDeleted(anyString(), any(MusicType.class), eq(false));
        verify(prskMusicRepository, times(1)).save(any(PrskMusic.class));
    }

    /**
     * updatePrskMusic : 正常系 - 全フィールドが null の場合（元の値が維持される）
     */
    @Test
    public void updatePrskMusicSuccess_AllFieldsNull() {
        Artist artist = createArtist(1L, "Test Artist");
        PrskMusic existingMusic = createPrskMusic(1L, "Original Title", MusicType.ORIGINAL, artist);
        existingMusic.setLyricsName("Original Lyricist");

        OptionalPrskMusicRequest request = new OptionalPrskMusicRequest();

        when(prskMusicRepository.findByIdAndIsDeleted(1L, false)).thenReturn(Optional.of(existingMusic));
        when(prskMusicRepository.save(any(PrskMusic.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PrskMusic result = prskMusicService.updatePrskMusic(1L, request);

        assertNotNull(result);
        assertEquals("Original Title", result.getTitle());
        assertEquals(MusicType.ORIGINAL, result.getMusicType());
        assertEquals("Original Lyricist", result.getLyricsName());

        verify(prskMusicRepository, times(1)).findByIdAndIsDeleted(1L, false);
        verify(prskMusicRepository, never()).findByTitleAndMusicTypeAndIsDeleted(anyString(), any(MusicType.class), eq(false));
        verify(prskMusicRepository, times(1)).save(any(PrskMusic.class));
    }

    /**
     * updatePrskMusic : 異常系 - プロセカ楽曲が見つからない
     */
    @Test
    public void updatePrskMusicError_NotFound() {
        OptionalPrskMusicRequest request = new OptionalPrskMusicRequest();
        request.setTitle("Updated Title");

        when(prskMusicRepository.findByIdAndIsDeleted(999L, false)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> prskMusicService.updatePrskMusic(999L, request)
        );

        assertEquals("Prsk music not found for id: 999", exception.getMessage());

        verify(prskMusicRepository, times(1)).findByIdAndIsDeleted(999L, false);
        verify(prskMusicRepository, never()).save(any(PrskMusic.class));
    }

    /**
     * updatePrskMusic : 異常系 - title + musicType が重複
     */
    @Test
    public void updatePrskMusicError_DuplicateTitleAndMusicType() {
        Artist artist = createArtist(1L, "Test Artist");
        PrskMusic existingMusic = createPrskMusic(1L, "Original Title", MusicType.ORIGINAL, artist);
        PrskMusic duplicateMusic = createPrskMusic(2L, "Duplicate Title", MusicType.THREE_D_MV, artist);

        OptionalPrskMusicRequest request = new OptionalPrskMusicRequest();
        request.setTitle("Duplicate Title");
        request.setMusicType(MusicType.THREE_D_MV);

        when(prskMusicRepository.findByIdAndIsDeleted(1L, false)).thenReturn(Optional.of(existingMusic));
        when(prskMusicRepository.findByTitleAndMusicTypeAndIsDeleted("Duplicate Title", MusicType.THREE_D_MV, false))
                .thenReturn(Optional.of(duplicateMusic));

        DuplicationResourceException exception = assertThrows(
                DuplicationResourceException.class,
                () -> prskMusicService.updatePrskMusic(1L, request)
        );

        assertNotNull(exception.getDetails());
        assertEquals("Title and MusicType", exception.getDetails().get(0).getField());

        verify(prskMusicRepository, times(1)).findByIdAndIsDeleted(1L, false);
        verify(prskMusicRepository, times(1)).findByTitleAndMusicTypeAndIsDeleted("Duplicate Title", MusicType.THREE_D_MV, false);
        verify(prskMusicRepository, never()).save(any(PrskMusic.class));
    }

    /**
     * deletePrskMusic : 正常系 - プロセカ楽曲を論理削除
     */
    @Test
    public void deletePrskMusicSuccess() {
        Artist artist = createArtist(1L, "Test Artist");
        PrskMusic existingMusic = createPrskMusic(1L, "Test Title", MusicType.ORIGINAL, artist);
        existingMusic.setDeleted(false);

        when(prskMusicRepository.findByIdAndIsDeleted(1L, false)).thenReturn(Optional.of(existingMusic));
        when(prskMusicRepository.save(any(PrskMusic.class))).thenAnswer(invocation -> invocation.getArgument(0));

        prskMusicService.deletePrskMusic(1L);

        assertTrue(existingMusic.isDeleted());

        verify(prskMusicRepository, times(1)).findByIdAndIsDeleted(1L, false);
        verify(prskMusicRepository, times(1)).save(existingMusic);
    }

    /**
     * deletePrskMusic : 異常系 - プロセカ楽曲が見つからない
     */
    @Test
    public void deletePrskMusicError_NotFound() {
        when(prskMusicRepository.findByIdAndIsDeleted(999L, false)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> prskMusicService.deletePrskMusic(999L)
        );

        assertEquals("Prsk music not found for id: 999", exception.getMessage());

        verify(prskMusicRepository, times(1)).findByIdAndIsDeleted(999L, false);
        verify(prskMusicRepository, never()).save(any(PrskMusic.class));
    }
}
