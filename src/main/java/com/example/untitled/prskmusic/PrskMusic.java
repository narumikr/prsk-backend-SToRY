package com.example.untitled.prskmusic;

import com.example.untitled.artist.Artist;
import com.example.untitled.common.entity.BaseEntity;
import com.example.untitled.prskmusic.enums.MusicType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "m_prsk_music", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"title", "music_type"})
})
@Data
@EqualsAndHashCode(callSuper = true)
public class PrskMusic extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "m_prsk_music_seq")
    @SequenceGenerator(name = "m_prsk_music_seq", sequenceName = "m_prsk_music_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, length = 30)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false)
    private Artist artist;

    @Column(name = "music_type", nullable = false)
    private MusicType musicType;

    @Column()
    private Boolean specially;

    @Column(length = 50)
    private String lyricsName;

    @Column(length = 50)
    private String musicName;

    @Column(length = 10)
    private String featuring;

    @Column(nullable = false, length = 100)
    private String youtubeLink;
}
