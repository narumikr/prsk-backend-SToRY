package com.example.untitled.artist;

import com.example.untitled.artist.dto.ArtistListResponse;
import com.example.untitled.artist.dto.ArtistRequest;
import com.example.untitled.artist.dto.ArtistResponse;
import com.example.untitled.artist.dto.OptionalArtistRequest;
import com.example.untitled.common.exception.DuplicationResourceException;
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
public class ArtistServiceTest {

    @Mock
    private ArtistRepository artistRepository;

    @InjectMocks
    private ArtistService artistService;

    /**
     * createArtists : 正常系 - アーティストが正常に作成される
     */
    @Test
    public void createArtistSuccess() {
        ArtistRequest request = new ArtistRequest();
        request.setArtistName("Test artist name");
        request.setUnitName("Test unit name");
        request.setContent("Test content");

        Artist createdArtist = new Artist();
        createdArtist.setId(1L);
        createdArtist.setArtistName("Test artist name");
        createdArtist.setUnitName("Test unit name");
        createdArtist.setContent("Test content");

        when(artistRepository.findByArtistNameAndIsDeleted("Test artist name", false)).thenReturn(Optional.empty());
        when(artistRepository.save(any(Artist.class))).thenReturn(createdArtist);

        ArtistResponse result = artistService.createArtist(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test artist name", result.getArtistName());
        assertEquals("Test unit name", result.getUnitName());
        assertEquals("Test content", result.getContent());

        verify(artistRepository, times(1)).findByArtistNameAndIsDeleted("Test artist name", false);
        verify(artistRepository, times(1)).save(any(Artist.class));
    }

    /**
     * createArtists : 異常系 - アーティスト名が重複しており、例外をスルーする
     */
    @Test
    public void createArtistsError_withDuplication() {
        ArtistRequest request = new ArtistRequest();
        request.setArtistName("Test artist name");
        request.setUnitName("Test unit name");
        request.setContent("Test content");

        Artist createdArtist = new Artist();
        createdArtist.setId(1L);
        createdArtist.setArtistName("Test artist name");
        createdArtist.setUnitName("Test unit name");
        createdArtist.setContent("Test content");

        when(artistRepository.findByArtistNameAndIsDeleted("Test artist name", false)).thenReturn(Optional.of(createdArtist));

        DuplicationResourceException exception = assertThrows(
                DuplicationResourceException.class,
                () -> artistService.createArtist(request)
        );

        assertNotNull(exception.getDetails());
        assertEquals("artistName", exception.getDetails().get(0).getField());

        verify(artistRepository, times(1)).findByArtistNameAndIsDeleted("Test artist name", false);
        verify(artistRepository, never()).save(any(Artist.class));
    }

    /**
     * getAllArtists : 正常系 - アーティスト一覧を取得（ASC順）
     */
    @Test
    public void getAllArtistsSuccess_ASC() {
        Artist artist1 = new Artist();
        artist1.setId(1L);
        artist1.setArtistName("Artist A");

        Artist artist2 = new Artist();
        artist2.setId(2L);
        artist2.setArtistName("Artist B");

        Page<Artist> artistPage = new PageImpl<>(
                Arrays.asList(artist1, artist2),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "artistName")),
                2
        );

        when(artistRepository.findByIsDeleted(eq(false), any(Pageable.class))).thenReturn(artistPage);

        ArtistListResponse result = artistService.getAllArtists(0, 20, "artistName", "ASC");

        assertNotNull(result);
        assertEquals(2, result.getMeta().getTotalItems());
        assertEquals(2, result.getItems().size());
        assertEquals("Artist A", result.getItems().get(0).getArtistName());
        assertEquals("Artist B", result.getItems().get(1).getArtistName());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(artistRepository, times(1)).findByIsDeleted(eq(false), pageableCaptor.capture());

        Pageable capturedPageable = pageableCaptor.getValue();
        assertEquals(0, capturedPageable.getPageNumber());
        assertEquals(20, capturedPageable.getPageSize());
        assertEquals(Sort.Direction.ASC, capturedPageable.getSort().getOrderFor("artistName").getDirection());
    }

    /**
     * getAllArtists : 正常系 - アーティスト一覧を取得（DESC順）
     */
    @Test
    public void getAllArtistsSuccess_DESC() {
        Artist artist1 = new Artist();
        artist1.setId(1L);
        artist1.setArtistName("Artist B");

        Artist artist2 = new Artist();
        artist2.setId(2L);
        artist2.setArtistName("Artist A");

        Page<Artist> artistPage = new PageImpl<>(
                Arrays.asList(artist1, artist2),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "artistName")),
                2
        );

        when(artistRepository.findByIsDeleted(eq(false), any(Pageable.class))).thenReturn(artistPage);

        ArtistListResponse result = artistService.getAllArtists(0, 20, "artistName", "DESC");

        assertNotNull(result);
        assertEquals(2, result.getMeta().getTotalItems());
        assertEquals("Artist B", result.getItems().get(0).getArtistName());
        assertEquals("Artist A", result.getItems().get(1).getArtistName());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(artistRepository, times(1)).findByIsDeleted(eq(false), pageableCaptor.capture());

        Pageable capturedPageable = pageableCaptor.getValue();
        assertEquals(Sort.Direction.DESC, capturedPageable.getSort().getOrderFor("artistName").getDirection());
    }

    /**
     * getAllArtists : 正常系 - 空のリストを返す
     */
    @Test
    public void getAllArtistsSuccess_EmptyList() {
        Page<Artist> emptyPage = new PageImpl<>(
                List.of(),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "artistName")),
                0
        );

        when(artistRepository.findByIsDeleted(eq(false), any(Pageable.class))).thenReturn(emptyPage);

        ArtistListResponse result = artistService.getAllArtists(0, 20, "artistName", "ASC");

        assertNotNull(result);
        assertEquals(0, result.getMeta().getTotalItems());
        assertTrue(result.getItems().isEmpty());

        verify(artistRepository, times(1)).findByIsDeleted(eq(false), any(Pageable.class));
    }

    /**
     * updateArtist : 正常系 - アーティスト情報を更新（全フィールド）
     */
    @Test
    public void updateArtistSuccess_AllFields() {
        Artist existingArtist = new Artist();
        existingArtist.setId(1L);
        existingArtist.setArtistName("Original Artist");
        existingArtist.setUnitName("Original Unit");
        existingArtist.setContent("Original Content");

        OptionalArtistRequest request = new OptionalArtistRequest();
        request.setArtistName("Updated Artist");
        request.setUnitName("Updated Unit");
        request.setContent("Updated Content");

        when(artistRepository.findByIdAndIsDeleted(1L, false)).thenReturn(Optional.of(existingArtist));
        when(artistRepository.findByArtistNameAndIsDeleted("Updated Artist", false)).thenReturn(Optional.empty());
        when(artistRepository.save(any(Artist.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ArtistResponse result = artistService.updateArtist(1L, request);

        assertNotNull(result);
        assertEquals("Updated Artist", result.getArtistName());
        assertEquals("Updated Unit", result.getUnitName());
        assertEquals("Updated Content", result.getContent());

        verify(artistRepository, times(1)).findByIdAndIsDeleted(1L, false);
        verify(artistRepository, times(1)).findByArtistNameAndIsDeleted("Updated Artist", false);
        verify(artistRepository, times(1)).save(any(Artist.class));
    }

    /**
     * updateArtist : 正常系 - アーティスト情報を部分更新
     */
    @Test
    public void updateArtistSuccess_PartialFields() {
        Artist existingArtist = new Artist();
        existingArtist.setId(1L);
        existingArtist.setArtistName("Original Artist");
        existingArtist.setUnitName("Original Unit");
        existingArtist.setContent("Original Content");

        OptionalArtistRequest request = new OptionalArtistRequest();
        request.setArtistName("Updated Artist");

        when(artistRepository.findByIdAndIsDeleted(1L, false)).thenReturn(Optional.of(existingArtist));
        when(artistRepository.findByArtistNameAndIsDeleted("Updated Artist", false)).thenReturn(Optional.empty());
        when(artistRepository.save(any(Artist.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ArtistResponse result = artistService.updateArtist(1L, request);

        assertNotNull(result);
        assertEquals("Updated Artist", result.getArtistName());
        assertEquals("Original Unit", result.getUnitName());
        assertEquals("Original Content", result.getContent());

        verify(artistRepository, times(1)).findByIdAndIsDeleted(1L, false);
        verify(artistRepository, times(1)).findByArtistNameAndIsDeleted("Updated Artist", false);
        verify(artistRepository, times(1)).save(any(Artist.class));
    }

    /**
     * updateArtist : 正常系 - アーティスト名が変更されない場合
     */
    @Test
    public void updateArtistSuccess_SameArtistName() {
        Artist existingArtist = new Artist();
        existingArtist.setId(1L);
        existingArtist.setArtistName("Test Artist");
        existingArtist.setUnitName("Original Unit");
        existingArtist.setContent("Original Content");

        OptionalArtistRequest request = new OptionalArtistRequest();
        request.setArtistName("Test Artist");
        request.setUnitName("Updated Unit");

        when(artistRepository.findByIdAndIsDeleted(1L, false)).thenReturn(Optional.of(existingArtist));
        when(artistRepository.save(any(Artist.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ArtistResponse result = artistService.updateArtist(1L, request);

        assertNotNull(result);
        assertEquals("Test Artist", result.getArtistName());
        assertEquals("Updated Unit", result.getUnitName());

        verify(artistRepository, times(1)).findByIdAndIsDeleted(1L, false);
        verify(artistRepository, never()).findByArtistNameAndIsDeleted(anyString(), eq(false));
        verify(artistRepository, times(1)).save(any(Artist.class));
    }

    /**
     * updateArtist : 正常系 - 全フィールドがnullの場合
     */
    @Test
    public void updateArtistSuccess_AllFieldsNull() {
        Artist existingArtist = new Artist();
        existingArtist.setId(1L);
        existingArtist.setArtistName("Original Artist");
        existingArtist.setUnitName("Original Unit");
        existingArtist.setContent("Original Content");

        OptionalArtistRequest request = new OptionalArtistRequest();

        when(artistRepository.findByIdAndIsDeleted(1L, false)).thenReturn(Optional.of(existingArtist));
        when(artistRepository.save(any(Artist.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ArtistResponse result = artistService.updateArtist(1L, request);

        assertNotNull(result);
        assertEquals("Original Artist", result.getArtistName());
        assertEquals("Original Unit", result.getUnitName());
        assertEquals("Original Content", result.getContent());

        verify(artistRepository, times(1)).findByIdAndIsDeleted(1L, false);
        verify(artistRepository, times(1)).save(any(Artist.class));
    }

    /**
     * updateArtist : 異常系 - アーティストが見つからない
     */
    @Test
    public void updateArtistError_NotFound() {
        OptionalArtistRequest request = new OptionalArtistRequest();
        request.setArtistName("Updated Artist");

        when(artistRepository.findByIdAndIsDeleted(999L, false)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> artistService.updateArtist(999L, request)
        );

        assertEquals("Artist not found for id: 999", exception.getMessage());

        verify(artistRepository, times(1)).findByIdAndIsDeleted(999L, false);
        verify(artistRepository, never()).save(any(Artist.class));
    }

    /**
     * updateArtist : 異常系 - アーティスト名が重複
     */
    @Test
    public void updateArtistError_DuplicateArtistName() {
        Artist existingArtist = new Artist();
        existingArtist.setId(1L);
        existingArtist.setArtistName("Original Artist");

        Artist duplicateArtist = new Artist();
        duplicateArtist.setId(2L);
        duplicateArtist.setArtistName("Duplicate Artist");

        OptionalArtistRequest request = new OptionalArtistRequest();
        request.setArtistName("Duplicate Artist");

        when(artistRepository.findByIdAndIsDeleted(1L, false)).thenReturn(Optional.of(existingArtist));
        when(artistRepository.findByArtistNameAndIsDeleted("Duplicate Artist", false))
                .thenReturn(Optional.of(duplicateArtist));

        DuplicationResourceException exception = assertThrows(
                DuplicationResourceException.class,
                () -> artistService.updateArtist(1L, request)
        );

        assertNotNull(exception.getDetails());
        assertEquals("artistName", exception.getDetails().get(0).getField());

        verify(artistRepository, times(1)).findByIdAndIsDeleted(1L, false);
        verify(artistRepository, times(1)).findByArtistNameAndIsDeleted("Duplicate Artist", false);
        verify(artistRepository, never()).save(any(Artist.class));
    }

    /**
     * deleteArtist : 正常系 - アーティストを論理削除
     */
    @Test
    public void deleteArtistSuccess() {
        Artist existingArtist = new Artist();
        existingArtist.setId(1L);
        existingArtist.setArtistName("Test Artist");
        existingArtist.setDeleted(false);

        when(artistRepository.findByIdAndIsDeleted(1L, false)).thenReturn(Optional.of(existingArtist));
        when(artistRepository.save(any(Artist.class))).thenAnswer(invocation -> invocation.getArgument(0));

        artistService.deleteArtist(1L);

        assertTrue(existingArtist.isDeleted());

        verify(artistRepository, times(1)).findByIdAndIsDeleted(1L, false);
        verify(artistRepository, times(1)).save(existingArtist);
    }

    /**
     * deleteArtist : 異常系 - アーティストが見つからない
     */
    @Test
    public void deleteArtistError_NotFound() {
        when(artistRepository.findByIdAndIsDeleted(999L, false)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> artistService.deleteArtist(999L)
        );

        assertEquals("Artist not found for id: 999", exception.getMessage());

        verify(artistRepository, times(1)).findByIdAndIsDeleted(999L, false);
        verify(artistRepository, never()).save(any(Artist.class));
    }
}
