package com.example.untitled.artist;

import com.example.untitled.artist.dto.ArtistListResponse;
import com.example.untitled.artist.dto.ArtistRequest;
import com.example.untitled.artist.dto.ArtistResponse;
import com.example.untitled.artist.dto.OptionalArtistRequest;
import com.example.untitled.common.dto.ErrorDetails;
import com.example.untitled.common.exception.DuplicationResourceException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.example.untitled.common.util.EntityHelper.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtistService {

    private final ArtistRepository artistRepository;

    @Transactional(readOnly = true)
    public ArtistListResponse getAllArtists(int page, int size, String sortBy, String direction) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("DESC")
                ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<Artist> artistPage = artistRepository.findByIsDeleted(false, pageable);
        return ArtistListResponse.from(artistPage);
    }

    public ArtistResponse createArtist(ArtistRequest reqDto) {
        artistRepository.findByArtistNameAndIsDeleted(reqDto.getArtistName(), false)
                .ifPresent(artist -> {
                    throw new DuplicationResourceException(
                            "Conflict detected",
                            List.of(new ErrorDetails(
                                    "artistName",
                                    "Artist name already exist: " + reqDto.getArtistName()
                            ))
                    );
                });

        Artist artist = new Artist();
        artist.setArtistName(reqDto.getArtistName());
        artist.setUnitName(reqDto.getUnitName());
        artist.setContent(reqDto.getContent());

        return ArtistResponse.from(artistRepository.save(artist));
    }

    public ArtistResponse updateArtist(Long id, OptionalArtistRequest reqDto) {
        Artist artist = artistRepository.findByIdAndIsDeleted(id, false)
                .orElseThrow(() -> new EntityNotFoundException("Artist not found for id: " + id));

        if(reqDto.getArtistName() != null && !reqDto.getArtistName().equals(artist.getArtistName())) {
            artistRepository.findByArtistNameAndIsDeleted(reqDto.getArtistName(), false)
                    .ifPresent(existArtist -> {
                        throw new DuplicationResourceException(
                                "Conflict detected",
                                List.of(new ErrorDetails(
                                        "artistName",
                                        "Artist name already exist: " + reqDto.getArtistName()))
                        );
                    });
        }

        // Memo: artist::setArtistNameはラムダ式の簡略記法で(value) -> artist.setArtistName(value));と同じ
        updateIfNotNull(reqDto.getArtistName(), artist::setArtistName);
        updateIfNotNull(reqDto.getUnitName(), artist::setUnitName);
        updateIfNotNull(reqDto.getContent(), artist::setContent);

        return ArtistResponse.from(artistRepository.save(artist));
    }

    public void deleteArtist(Long id) {
        Artist artist = artistRepository.findByIdAndIsDeleted(id, false)
                .orElseThrow(() -> new EntityNotFoundException("Artist not found for id: " + id));

        artist.setDeleted(true);
        artistRepository.save(artist);
    }
}
