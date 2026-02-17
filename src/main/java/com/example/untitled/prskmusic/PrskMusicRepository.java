package com.example.untitled.prskmusic;

import com.example.untitled.prskmusic.enums.MusicType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrskMusicRepository extends JpaRepository<PrskMusic, Long> {

    @Query(
            value = "SELECT p FROM PrskMusic p JOIN FETCH p.artist WHERE p.isDeleted = :isDeleted",
            countQuery = "SELECT count(p) FROM PrskMusic p WHERE p.isDeleted = :isDeleted"
    )
    Page<PrskMusic> findByIsDeleted(@Param("isDeleted") boolean isDeleted, Pageable pageable);

    Optional<PrskMusic> findByIdAndIsDeleted(Long id, boolean isDeleted);

    Optional<PrskMusic> findByTitleAndMusicTypeAndIsDeleted(String title, MusicType musicType, boolean isDeleted);
}