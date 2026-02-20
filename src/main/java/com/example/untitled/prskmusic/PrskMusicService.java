package com.example.untitled.prskmusic;

import com.example.untitled.artist.Artist;
import com.example.untitled.artist.ArtistRepository;
import com.example.untitled.common.dto.ErrorDetails;
import com.example.untitled.common.exception.DuplicationResourceException;
import com.example.untitled.prskmusic.dto.OptionalPrskMusicRequest;
import com.example.untitled.prskmusic.dto.PrskMusicRequest;
import com.example.untitled.prskmusic.enums.MusicType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.example.untitled.common.util.EntityHelper.updateIfNotNull;

@Service
@RequiredArgsConstructor
@Transactional
public class PrskMusicService {

    private final PrskMusicRepository prskMusicRepository;
    private final ArtistRepository artistRepository;

    @Transactional(readOnly = true)
    public Page<PrskMusic> getAllPrskMusic(int page, int size, String sortBy, String direction) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("DESC")
                ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        return prskMusicRepository.findByIsDeleted(false, pageable);
    }

    public PrskMusic createPrskMusic(PrskMusicRequest reqDto) {
        prskMusicRepository.findByTitleAndMusicTypeAndIsDeleted(
                reqDto.getTitle(),
                reqDto.getMusicType(),
                false
        ).ifPresent(prskMusic -> {
            throw new DuplicationResourceException(
                    "Conflict detected",
                    List.of(new ErrorDetails(
                            "Title and MusicType",
                            "Duplicate title and music type combination."
                    ))
            );
        });

        Artist artist = artistRepository.findByIdAndIsDeleted(reqDto.getArtistId(), false)
                .orElseThrow(() -> new EntityNotFoundException("Artist not found for id: " + reqDto.getArtistId()));

        PrskMusic prskMusic = new PrskMusic();
        prskMusic.setTitle(reqDto.getTitle());
        prskMusic.setArtist(artist);
        prskMusic.setMusicType(reqDto.getMusicType());
        prskMusic.setSpecially(reqDto.getSpecially());
        prskMusic.setLyricsName(reqDto.getLyricsName());
        prskMusic.setMusicName(reqDto.getMusicName());
        prskMusic.setFeaturing(reqDto.getFeaturing());
        prskMusic.setYoutubeLink(reqDto.getYoutubeLink());

        return prskMusicRepository.save(prskMusic);
    }

    public PrskMusic updatePrskMusic(Long id, OptionalPrskMusicRequest reqDto) {
        PrskMusic prskMusic = prskMusicRepository.findByIdAndIsDeleted(id, false)
                .orElseThrow(() -> new EntityNotFoundException("Prsk music not found for id: " + id));

        String newTitle = reqDto.getTitle() != null ? reqDto.getTitle() : prskMusic.getTitle();
        MusicType newMusicType = reqDto.getMusicType() != null ? reqDto.getMusicType() : prskMusic.getMusicType();

        boolean isTitleChanged = reqDto.getTitle() != null && !reqDto.getTitle().equals(prskMusic.getTitle());
        boolean isMusicTypeChanged = reqDto.getMusicType() != null && !reqDto.getMusicType().equals(prskMusic.getMusicType());

        if(isTitleChanged || isMusicTypeChanged) {
            prskMusicRepository.findByTitleAndMusicTypeAndIsDeleted(newTitle, newMusicType, false)
                    .ifPresent(existPrskMusic -> {
                        if(!existPrskMusic.getId().equals(id)) {
                            throw new DuplicationResourceException(
                                    "Conflict detected.",
                                    List.of(new ErrorDetails(
                                            "Title and MusicType",
                                            "Duplicate title and music type combination."
                                    ))
                            );
                        }
                    });
        }

        Artist artist = reqDto.getArtistId() != null
                ? artistRepository.findByIdAndIsDeleted(reqDto.getArtistId(), false)
                    .orElseThrow(() -> new EntityNotFoundException("Artist not found for id: " + reqDto.getArtistId()))
                : null;

        updateIfNotNull(reqDto.getTitle(), prskMusic::setTitle);
        updateIfNotNull(artist, prskMusic::setArtist);
        updateIfNotNull(reqDto.getMusicType(), prskMusic::setMusicType);
        updateIfNotNull(reqDto.getSpecially(), prskMusic::setSpecially);
        updateIfNotNull(reqDto.getLyricsName(), prskMusic::setLyricsName);
        updateIfNotNull(reqDto.getMusicName(), prskMusic::setMusicName);
        updateIfNotNull(reqDto.getFeaturing(), prskMusic::setFeaturing);
        updateIfNotNull(reqDto.getYoutubeLink(), prskMusic::setYoutubeLink);

        return prskMusicRepository.save(prskMusic);
    }

    public void deletePrskMusic(Long id) {
        PrskMusic prskMusic = prskMusicRepository.findByIdAndIsDeleted(id, false)
                .orElseThrow(() -> new EntityNotFoundException("Prsk music not found for id: " + id));

        prskMusic.setDeleted(true);
        prskMusicRepository.save(prskMusic);
    }
}
