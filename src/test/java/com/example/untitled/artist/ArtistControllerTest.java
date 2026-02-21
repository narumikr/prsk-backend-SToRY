package com.example.untitled.artist;

import com.example.untitled.artist.dto.ArtistListResponse;
import com.example.untitled.artist.dto.ArtistResponse;
import com.example.untitled.common.dto.ErrorDetails;
import com.example.untitled.common.dto.MetaInfo;
import com.example.untitled.common.exception.DuplicationResourceException;
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

@WebMvcTest(ArtistController.class)
public class ArtistControllerTest {

    @Autowired
    private MockMvc mvcMock;

    @MockitoBean
    private ArtistService artistService;

    /**
     * POST /artists : Response success
     */
    @Test
    public void createArtistSuccess() throws Exception {
        String expectedArtistName = "Test artist";
        String expectedUnitName = "Test unit name";
        String expectedContent = "Test content";

        ArtistResponse mockArtistResponse = ArtistResponse.builder()
                .id(1L)
                .artistName(expectedArtistName)
                .unitName(expectedUnitName)
                .content(expectedContent)
                .build();

        when(artistService.createArtist(argThat(request ->
                request.getArtistName().equals(expectedArtistName) &&
                        request.getUnitName().equals(expectedUnitName) &&
                        request.getContent().equals(expectedContent)
        ))).thenReturn(mockArtistResponse);

        String reqBody = """
                {
                    "artistName": "%s",
                    "unitName": "%s",
                    "content": "%s"
                }
                """.formatted(expectedArtistName, expectedUnitName, expectedContent);

        mvcMock.perform(post("/artists")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reqBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.artistName").value("Test artist"))
                .andExpect(jsonPath("$.unitName").value("Test unit name"))
                .andExpect(jsonPath("$.content").value("Test content"));
    }

    /**
     * POST /artists : Response BadRequest
     * artistNameのNull不可チェック
     */
    @Test
    public void createArtistError_withBadRequest_ArtistNameNull() throws Exception {
        String reqBody = """
                {
                    "artistName": null,
                    "unitName": "Test unit name",
                    "content": "Test content"
                }
                """;

        mvcMock.perform(post("/artists")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reqBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").exists())
                .andExpect(jsonPath("$.details[0].field").value("artistName"));
    }

    /**
     * POST /artists : Response BadRequest
     * Request body validation error
     */
    @Test
    public void createArtistError_withBadRequest_LengthOver() throws Exception {
        String ngArtistName = generateRandomString(51);
        String ngUnitName = generateRandomString(26);
        String ngContent = generateRandomString(21);

        String reqBody = """
                {
                    "artistName": "%s",
                    "unitName": "%s",
                    "content": "%s"
                }
                """.formatted(ngArtistName, ngUnitName, ngContent);

        mvcMock.perform(post("/artists")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reqBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").exists())
                .andExpect(jsonPath("$.details[*].field", containsInAnyOrder("artistName", "unitName", "content")));
    }

    /**
     * POST /artists : Conflict
     * 登録アーティスト名の重複エラー
     */
    @Test
    public void createArtistError_withConflict_AlreadyExist() throws Exception {
        when(artistService.createArtist(any()))
                .thenThrow(new DuplicationResourceException(
                        "Conflict detected",
                        List.of(new ErrorDetails("artistName", "Artist name already exist"))
                ));

        String reqBody = """
            {
                "artistName": "Test artist name",
                "unitName": "Test unit name",
                "content": "Test content"
            }
            """;

        mvcMock.perform(post("/artists")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reqBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.details").exists())
                .andExpect(jsonPath("$.details[0].field").value("artistName"));
    }

    /**
     * GET /artists : Response success
     * アーティスト一覧取得の正常系
     */
    @Test
    public void getArtistsListSuccess() throws Exception {
        Artist artist1 = new Artist();
        artist1.setId(1L);
        artist1.setArtistName("Artist 1");
        artist1.setUnitName("Unit 1");
        artist1.setContent("Content 1");

        Artist artist2 = new Artist();
        artist2.setId(2L);
        artist2.setArtistName("Artist 2");
        artist2.setUnitName("Unit 2");
        artist2.setContent("Content 2");

        Page<Artist> artistPage = new PageImpl<>(
                Arrays.asList(artist1, artist2),
                PageRequest.of(0, 20),
                2
        );

        when(artistService.getAllArtists(0, 20, "artistName", "ASC")).thenReturn(ArtistListResponse.from(artistPage));

        mvcMock.perform(get("/artists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[0].artistName").value("Artist 1"))
                .andExpect(jsonPath("$.items[1].id").value(2))
                .andExpect(jsonPath("$.items[1].artistName").value("Artist 2"))
                .andExpect(jsonPath("$.meta.pageIndex").value(0))
                .andExpect(jsonPath("$.meta.totalPages").value(1))
                .andExpect(jsonPath("$.meta.totalItems").value(2))
                .andExpect(jsonPath("$.meta.limit").value(20));

        verify(artistService, times(1)).getAllArtists(0, 20, "artistName", "ASC");
    }

    /**
     * GET /artists : Response success with pagination parameters
     * ページネーションパラメータを指定した一覧取得
     */
    @Test
    public void getArtistsListSuccess_WithPaginationParams() throws Exception {
        Artist artist1 = new Artist();
        artist1.setId(11L);
        artist1.setArtistName("Artist 11");

        Artist artist2 = new Artist();
        artist2.setId(12L);
        artist2.setArtistName("Artist 12");

        Artist artist3 = new Artist();
        artist3.setId(13L);
        artist3.setArtistName("Artist 13");

        Artist artist4 = new Artist();
        artist4.setId(14L);
        artist4.setArtistName("Artist 14");

        Artist artist5 = new Artist();
        artist5.setId(15L);
        artist5.setArtistName("Artist 15");

        Page<Artist> artistPage = new PageImpl<>(
                List.of(artist1, artist2, artist3, artist4, artist5),
                PageRequest.of(1, 10),
                15
        );

        when(artistService.getAllArtists(1, 10, "artistName", "ASC")).thenReturn(ArtistListResponse.from(artistPage));

        mvcMock.perform(get("/artists")
                        .param("page", "2")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(5)))
                .andExpect(jsonPath("$.meta.pageIndex").value(1))
                .andExpect(jsonPath("$.meta.totalPages").value(2))
                .andExpect(jsonPath("$.meta.totalItems").value(15))
                .andExpect(jsonPath("$.meta.limit").value(10));

        verify(artistService, times(1)).getAllArtists(1, 10, "artistName", "ASC");
    }

    /**
     * GET /artists : BadRequest
     * 不正なページネーションパラメータ（pageが0以下）
     */
    @Test
    public void getArtistsListError_withBadRequest_InvalidPage() throws Exception {
        mvcMock.perform(get("/artists")
                        .param("page", "0")
                        .param("limit", "20"))
                .andExpect(status().isBadRequest());
    }

    /**
     * GET /artists : BadRequest
     * 不正なページネーションパラメータ（limitが範囲外）
     */
    @Test
    public void getArtistsListError_withBadRequest_InvalidLimit() throws Exception {
        mvcMock.perform(get("/artists")
                        .param("page", "1")
                        .param("limit", "101"))
                .andExpect(status().isBadRequest());

        mvcMock.perform(get("/artists")
                        .param("page", "1")
                        .param("limit", "0"))
                .andExpect(status().isBadRequest());
    }

    /**
     * PUT /artists/{id} : Response success
     * アーティスト情報更新の正常系
     */
    @Test
    public void updateArtistSuccess() throws Exception {
        ArtistResponse updatedArtistResponse = ArtistResponse.builder()
                .id(1L)
                .artistName("Updated Artist")
                .unitName("Updated Unit")
                .content("Updated Content")
                .build();

        when(artistService.updateArtist(eq(1L), any())).thenReturn(updatedArtistResponse);

        String reqBody = """
                {
                    "artistName": "Updated Artist",
                    "unitName": "Updated Unit",
                    "content": "Updated Content"
                }
                """;

        mvcMock.perform(put("/artists/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.artistName").value("Updated Artist"))
                .andExpect(jsonPath("$.unitName").value("Updated Unit"))
                .andExpect(jsonPath("$.content").value("Updated Content"));

        verify(artistService, times(1)).updateArtist(eq(1L), any());
    }

    /**
     * PUT /artists/{id} : Response success with partial update
     * 一部のフィールドのみ更新
     */
    @Test
    public void updateArtistSuccess_PartialUpdate() throws Exception {
        ArtistResponse updatedArtistResponse = ArtistResponse.builder()
                .id(1L)
                .artistName("Updated Artist")
                .unitName("Original Unit")
                .content("Original Content")
                .build();

        when(artistService.updateArtist(eq(1L), any())).thenReturn(updatedArtistResponse);

        String reqBody = """
                {
                    "artistName": "Updated Artist"
                }
                """;

        mvcMock.perform(put("/artists/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.artistName").value("Updated Artist"));

        verify(artistService, times(1)).updateArtist(eq(1L), any());
    }

    /**
     * PUT /artists/{id} : NotFound
     * 存在しないアーティストIDを指定した場合
     */
    @Test
    public void updateArtistError_withNotFound() throws Exception {
        when(artistService.updateArtist(eq(999L), any()))
                .thenThrow(new EntityNotFoundException("Artist not found for id: 999"));

        String reqBody = """
                {
                    "artistName": "Updated Artist"
                }
                """;

        mvcMock.perform(put("/artists/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isNotFound());

        verify(artistService, times(1)).updateArtist(eq(999L), any());
    }

    /**
     * PUT /artists/{id} : BadRequest
     * バリデーションエラー（文字数超過）
     */
    @Test
    public void updateArtistError_withBadRequest_LengthOver() throws Exception {
        String ngArtistName = generateRandomString(51);

        String reqBody = """
                {
                    "artistName": "%s"
                }
                """.formatted(ngArtistName);

        mvcMock.perform(put("/artists/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").exists())
                .andExpect(jsonPath("$.details[0].field").value("artistName"));

        verify(artistService, never()).updateArtist(anyLong(), any());
    }

    /**
     * PUT /artists/{id} : Conflict
     * アーティスト名の重複エラー
     */
    @Test
    public void updateArtistError_withConflict_AlreadyExist() throws Exception {
        when(artistService.updateArtist(eq(1L), any()))
                .thenThrow(new DuplicationResourceException(
                        "Conflict detected",
                        List.of(new ErrorDetails("artistName", "Artist name already exist"))
                ));

        String reqBody = """
                {
                    "artistName": "Duplicate Artist Name"
                }
                """;

        mvcMock.perform(put("/artists/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.details").exists())
                .andExpect(jsonPath("$.details[0].field").value("artistName"));

        verify(artistService, times(1)).updateArtist(eq(1L), any());
    }

    /**
     * PUT /artists/{id} : BadRequest
     * 不正なIDパラメータ（0以下）
     */
    @Test
    public void updateArtistError_withBadRequest_InvalidId() throws Exception {
        String reqBody = """
                {
                    "artistName": "Test Artist"
                }
                """;

        mvcMock.perform(put("/artists/0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isBadRequest());

        verify(artistService, never()).updateArtist(anyLong(), any());
    }

    /**
     * DELETE /artists/{id} : Response success
     * アーティスト削除の正常系
     */
    @Test
    public void deleteArtistSuccess() throws Exception {
        doNothing().when(artistService).deleteArtist(1L);

        mvcMock.perform(delete("/artists/1"))
                .andExpect(status().isNoContent());

        verify(artistService, times(1)).deleteArtist(1L);
    }

    /**
     * DELETE /artists/{id} : NotFound
     * 存在しないアーティストIDを指定した場合
     */
    @Test
    public void deleteArtistError_withNotFound() throws Exception {
        doThrow(new EntityNotFoundException("Artist not found for id: 999"))
                .when(artistService).deleteArtist(999L);

        mvcMock.perform(delete("/artists/999"))
                .andExpect(status().isNotFound());

        verify(artistService, times(1)).deleteArtist(999L);
    }
}
