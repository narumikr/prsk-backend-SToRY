package com.example.untitled.prskmusic;

import com.example.untitled.artist.Artist;
import com.example.untitled.common.dto.ErrorDetails;
import com.example.untitled.common.exception.DuplicationResourceException;
import com.example.untitled.prskmusic.dto.PrskMusicListResponse;
import com.example.untitled.prskmusic.dto.PrskMusicResponse;
import com.example.untitled.prskmusic.enums.MusicType;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static com.example.untitled.common.util.UtilsFunction.generateRandomString;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PrskMusicController.class)
public class PrskMusicControllerTest {

    @Autowired
    private MockMvc mvcMock;

    @MockitoBean
    private PrskMusicService prskMusicService;

    private PrskMusic createMockPrskMusic(Long id, String title, MusicType musicType, String youtubeLink) {
        Artist artist = new Artist();
        artist.setId(1L);
        artist.setArtistName("Test Artist");
        artist.setUnitName("Test Unit");
        artist.setContent("Test Content");

        PrskMusic prskMusic = new PrskMusic();
        prskMusic.setId(id);
        prskMusic.setTitle(title);
        prskMusic.setArtist(artist);
        prskMusic.setMusicType(musicType);
        prskMusic.setYoutubeLink(youtubeLink);
        return prskMusic;
    }

    /**
     * POST /prsk-music : Response success
     * プロセカ楽曲登録の正常系
     */
    @Test
    public void registerPrskMusicSuccess() throws Exception {
        PrskMusic mockPrskMusic = createMockPrskMusic(1L, "Test Title", MusicType.ORIGINAL, "https://youtube.com/test");
        mockPrskMusic.setLyricsName("Test Lyricist");
        mockPrskMusic.setMusicName("Test Composer");

        when(prskMusicService.createPrskMusic(any())).thenReturn(PrskMusicResponse.from(mockPrskMusic));

        String reqBody = """
                {
                    "title": "Test Title",
                    "artistId": 1,
                    "musicType": 0,
                    "lyricsName": "Test Lyricist",
                    "musicName": "Test Composer",
                    "youtubeLink": "https://youtube.com/test"
                }
                """;

        mvcMock.perform(post("/prsk-music")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Title"))
                .andExpect(jsonPath("$.musicType").value(0))
                .andExpect(jsonPath("$.lyricsName").value("Test Lyricist"))
                .andExpect(jsonPath("$.youtubeLink").value("https://youtube.com/test"));

        verify(prskMusicService, times(1)).createPrskMusic(any());
    }

    /**
     * POST /prsk-music : Response BadRequest
     * titleのNull不可チェック
     */
    @Test
    public void registerPrskMusicError_withBadRequest_TitleNull() throws Exception {
        String reqBody = """
                {
                    "title": null,
                    "artistId": 1,
                    "musicType": 0,
                    "youtubeLink": "https://youtube.com/test"
                }
                """;

        mvcMock.perform(post("/prsk-music")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").exists())
                .andExpect(jsonPath("$.details[0].field").value("title"));
    }

    /**
     * POST /prsk-music : Response BadRequest
     * バリデーションエラー（全フィールド文字数超過）
     */
    @Test
    public void registerPrskMusicError_withBadRequest_LengthOver() throws Exception {
        String ngTitle = generateRandomString(31);
        String ngLyricsName = generateRandomString(51);
        String ngMusicName = generateRandomString(51);
        String ngFeaturing = generateRandomString(11);
        String ngYoutubeLink = generateRandomString(101);

        String reqBody = """
                {
                    "title": "%s",
                    "artistId": 1,
                    "musicType": 0,
                    "lyricsName": "%s",
                    "musicName": "%s",
                    "featuring": "%s",
                    "youtubeLink": "%s"
                }
                """.formatted(ngTitle, ngLyricsName, ngMusicName, ngFeaturing, ngYoutubeLink);

        mvcMock.perform(post("/prsk-music")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").exists())
                .andExpect(jsonPath("$.details[*].field",
                        containsInAnyOrder("title", "lyricsName", "musicName", "featuring", "youtubeLink")));
    }

    /**
     * POST /prsk-music : Conflict
     * title + musicType の重複エラー
     */
    @Test
    public void registerPrskMusicError_withConflict_AlreadyExist() throws Exception {
        when(prskMusicService.createPrskMusic(any()))
                .thenThrow(new DuplicationResourceException(
                        "Conflict detected",
                        List.of(new ErrorDetails("Title and MusicType", "Duplicate title and music type combination."))
                ));

        String reqBody = """
                {
                    "title": "Duplicate Title",
                    "artistId": 1,
                    "musicType": 0,
                    "youtubeLink": "https://youtube.com/test"
                }
                """;

        mvcMock.perform(post("/prsk-music")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.details").exists())
                .andExpect(jsonPath("$.details[0].field").value("Title and MusicType"));
    }

    /**
     * GET /prsk-music : Response success
     * プロセカ楽曲一覧取得の正常系
     */
    @Test
    public void getPrskMusicListSuccess() throws Exception {
        PrskMusic music1 = createMockPrskMusic(1L, "Music 1", MusicType.ORIGINAL, "https://youtube.com/1");
        PrskMusic music2 = createMockPrskMusic(2L, "Music 2", MusicType.THREE_D_MV, "https://youtube.com/2");

        Page<PrskMusic> musicPage = new PageImpl<>(
                Arrays.asList(music1, music2),
                PageRequest.of(0, 20),
                2
        );

        when(prskMusicService.getAllPrskMusic(0, 20, "title", "ASC")).thenReturn(PrskMusicListResponse.from(musicPage));

        mvcMock.perform(get("/prsk-music"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Music 1"))
                .andExpect(jsonPath("$.items[1].id").value(2))
                .andExpect(jsonPath("$.items[1].title").value("Music 2"))
                .andExpect(jsonPath("$.meta.pageIndex").value(0))
                .andExpect(jsonPath("$.meta.totalPages").value(1))
                .andExpect(jsonPath("$.meta.totalItems").value(2))
                .andExpect(jsonPath("$.meta.limit").value(20));

        verify(prskMusicService, times(1)).getAllPrskMusic(0, 20, "title", "ASC");
    }

    /**
     * GET /prsk-music : Response success with pagination parameters
     * ページネーションパラメータを指定した一覧取得
     */
    @Test
    public void getPrskMusicListSuccess_WithPaginationParams() throws Exception {
        PrskMusic music1 = createMockPrskMusic(11L, "Music 11", MusicType.ORIGINAL, "https://youtube.com/11");
        PrskMusic music2 = createMockPrskMusic(12L, "Music 12", MusicType.ORIGINAL, "https://youtube.com/12");
        PrskMusic music3 = createMockPrskMusic(13L, "Music 13", MusicType.ORIGINAL, "https://youtube.com/13");
        PrskMusic music4 = createMockPrskMusic(14L, "Music 14", MusicType.ORIGINAL, "https://youtube.com/14");
        PrskMusic music5 = createMockPrskMusic(15L, "Music 15", MusicType.ORIGINAL, "https://youtube.com/15");

        Page<PrskMusic> musicPage = new PageImpl<>(
                List.of(music1, music2, music3, music4, music5),
                PageRequest.of(1, 10),
                15
        );

        when(prskMusicService.getAllPrskMusic(1, 10, "title", "ASC")).thenReturn(PrskMusicListResponse.from(musicPage));

        mvcMock.perform(get("/prsk-music")
                        .param("page", "2")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(5)))
                .andExpect(jsonPath("$.meta.pageIndex").value(1))
                .andExpect(jsonPath("$.meta.totalPages").value(2))
                .andExpect(jsonPath("$.meta.totalItems").value(15))
                .andExpect(jsonPath("$.meta.limit").value(10));

        verify(prskMusicService, times(1)).getAllPrskMusic(1, 10, "title", "ASC");
    }

    /**
     * GET /prsk-music : BadRequest
     * 不正なページネーションパラメータ（pageが0以下）
     */
    @Test
    public void getPrskMusicListError_withBadRequest_InvalidPage() throws Exception {
        mvcMock.perform(get("/prsk-music")
                        .param("page", "0")
                        .param("limit", "20"))
                .andExpect(status().isBadRequest());
    }

    /**
     * GET /prsk-music : BadRequest
     * 不正なページネーションパラメータ（limitが範囲外）
     */
    @Test
    public void getPrskMusicListError_withBadRequest_InvalidLimit() throws Exception {
        mvcMock.perform(get("/prsk-music")
                        .param("page", "1")
                        .param("limit", "101"))
                .andExpect(status().isBadRequest());

        mvcMock.perform(get("/prsk-music")
                        .param("page", "1")
                        .param("limit", "0"))
                .andExpect(status().isBadRequest());
    }

    /**
     * PUT /prsk-music/{id} : Response success
     * プロセカ楽曲情報更新の正常系
     */
    @Test
    public void updatePrskMusicSuccess() throws Exception {
        PrskMusic updatedMusic = createMockPrskMusic(1L, "Updated Title", MusicType.THREE_D_MV, "https://youtube.com/updated");
        updatedMusic.setLyricsName("Updated Lyricist");

        when(prskMusicService.updatePrskMusic(eq(1L), any())).thenReturn(PrskMusicResponse.from(updatedMusic));

        String reqBody = """
                {
                    "title": "Updated Title",
                    "musicType": 1,
                    "lyricsName": "Updated Lyricist",
                    "youtubeLink": "https://youtube.com/updated"
                }
                """;

        mvcMock.perform(put("/prsk-music/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.musicType").value(1))
                .andExpect(jsonPath("$.lyricsName").value("Updated Lyricist"));

        verify(prskMusicService, times(1)).updatePrskMusic(eq(1L), any());
    }

    /**
     * PUT /prsk-music/{id} : Response success with partial update
     * 一部のフィールドのみ更新
     */
    @Test
    public void updatePrskMusicSuccess_PartialUpdate() throws Exception {
        PrskMusic updatedMusic = createMockPrskMusic(1L, "Updated Title", MusicType.ORIGINAL, "https://youtube.com/original");

        when(prskMusicService.updatePrskMusic(eq(1L), any())).thenReturn(PrskMusicResponse.from(updatedMusic));

        String reqBody = """
                {
                    "title": "Updated Title"
                }
                """;

        mvcMock.perform(put("/prsk-music/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.musicType").value(0));

        verify(prskMusicService, times(1)).updatePrskMusic(eq(1L), any());
    }

    /**
     * PUT /prsk-music/{id} : NotFound
     * 存在しないプロセカ楽曲IDを指定した場合
     */
    @Test
    public void updatePrskMusicError_withNotFound() throws Exception {
        when(prskMusicService.updatePrskMusic(eq(999L), any()))
                .thenThrow(new EntityNotFoundException("Prsk music not found for id: 999"));

        String reqBody = """
                {
                    "title": "Updated Title"
                }
                """;

        mvcMock.perform(put("/prsk-music/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isNotFound());

        verify(prskMusicService, times(1)).updatePrskMusic(eq(999L), any());
    }

    /**
     * PUT /prsk-music/{id} : BadRequest
     * バリデーションエラー（文字数超過）
     */
    @Test
    public void updatePrskMusicError_withBadRequest_LengthOver() throws Exception {
        String ngTitle = generateRandomString(31);

        String reqBody = """
                {
                    "title": "%s"
                }
                """.formatted(ngTitle);

        mvcMock.perform(put("/prsk-music/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").exists())
                .andExpect(jsonPath("$.details[0].field").value("title"));

        verify(prskMusicService, never()).updatePrskMusic(anyLong(), any());
    }

    /**
     * PUT /prsk-music/{id} : Conflict
     * title + musicType の重複エラー
     */
    @Test
    public void updatePrskMusicError_withConflict_AlreadyExist() throws Exception {
        when(prskMusicService.updatePrskMusic(eq(1L), any()))
                .thenThrow(new DuplicationResourceException(
                        "Conflict detected.",
                        List.of(new ErrorDetails("Title and MusicType", "Duplicate title and music type combination."))
                ));

        String reqBody = """
                {
                    "title": "Duplicate Title",
                    "musicType": 0
                }
                """;

        mvcMock.perform(put("/prsk-music/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.details").exists())
                .andExpect(jsonPath("$.details[0].field").value("Title and MusicType"));

        verify(prskMusicService, times(1)).updatePrskMusic(eq(1L), any());
    }

    /**
     * PUT /prsk-music/{id} : BadRequest
     * 不正なIDパラメータ（0以下）
     */
    @Test
    public void updatePrskMusicError_withBadRequest_InvalidId() throws Exception {
        String reqBody = """
                {
                    "title": "Test Title"
                }
                """;

        mvcMock.perform(put("/prsk-music/0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isBadRequest());

        verify(prskMusicService, never()).updatePrskMusic(anyLong(), any());
    }

    /**
     * DELETE /prsk-music/{id} : Response success
     * プロセカ楽曲削除の正常系
     */
    @Test
    public void deletePrskMusicSuccess() throws Exception {
        doNothing().when(prskMusicService).deletePrskMusic(1L);

        mvcMock.perform(delete("/prsk-music/1"))
                .andExpect(status().isNoContent());

        verify(prskMusicService, times(1)).deletePrskMusic(1L);
    }

    /**
     * DELETE /prsk-music/{id} : NotFound
     * 存在しないプロセカ楽曲IDを指定した場合
     */
    @Test
    public void deletePrskMusicError_withNotFound() throws Exception {
        doThrow(new EntityNotFoundException("Prsk music not found for id: 999"))
                .when(prskMusicService).deletePrskMusic(999L);

        mvcMock.perform(delete("/prsk-music/999"))
                .andExpect(status().isNotFound());

        verify(prskMusicService, times(1)).deletePrskMusic(999L);
    }
}
