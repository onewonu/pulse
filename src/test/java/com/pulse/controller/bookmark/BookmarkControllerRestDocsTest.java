package com.pulse.controller.bookmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pulse.dto.bookmark.BookmarkCreateRequest;
import com.pulse.dto.bookmark.BookmarkReorderRequest;
import com.pulse.dto.bookmark.BookmarkResponse;
import com.pulse.service.bookmark.BookmarkService;
import com.pulse.support.RestDocsSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookmarkControllerRestDocsTest extends RestDocsSupport {

    private final BookmarkService bookmarkService = mock(BookmarkService.class);
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    protected Object controller() {
        return new BookmarkController(bookmarkService);
    }

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(1L, null, List.of()));
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private BookmarkResponse sampleBookmarkResponse() {
        return new BookmarkResponse(
                1L,
                "강남 → 홍대입구",
                "228",
                "150",
                "강남",
                "홍대입구",
                "2호선",
                "#00A84D",
                "2호선",
                "#00A84D",
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                1,
                LocalDateTime.of(2026, 3, 21, 9, 0),
                LocalDateTime.of(2026, 3, 21, 9, 0)
        );
    }

    @Test
    @DisplayName("북마크 생성")
    void createBookmark() throws Exception {
        // given
        BookmarkCreateRequest request = new BookmarkCreateRequest(
                "강남 → 홍대입구", "228", "150", LocalTime.of(9, 0), LocalTime.of(10, 0)
        );
        given(bookmarkService.createBookmark(eq(1L), any())).willReturn(sampleBookmarkResponse());

        // when & then
        mockMvc.perform(post("/bookmarks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andDo(restDocs.document(
                        requestFields(
                                fieldWithPath("name").description("북마크 이름"),
                                fieldWithPath("departureStationId").description("출발역 ID (숫자)"),
                                fieldWithPath("arrivalStationId").description("도착역 ID (숫자)"),
                                fieldWithPath("startTime").description("출발 시간 (HH:mm:ss)"),
                                fieldWithPath("endTime").description("도착 시간 (HH:mm:ss)")
                        ),
                        responseFields(
                                fieldWithPath("id").description("북마크 ID"),
                                fieldWithPath("name").description("북마크 이름"),
                                fieldWithPath("departureStationId").description("출발역 ID"),
                                fieldWithPath("arrivalStationId").description("도착역 ID"),
                                fieldWithPath("departureStationName").description("출발역 이름"),
                                fieldWithPath("arrivalStationName").description("도착역 이름"),
                                fieldWithPath("departureLineName").description("출발역 노선 이름"),
                                fieldWithPath("departureLineColor").description("출발역 노선 색상"),
                                fieldWithPath("arrivalLineName").description("도착역 노선 이름"),
                                fieldWithPath("arrivalLineColor").description("도착역 노선 색상"),
                                fieldWithPath("startTime").description("출발 시간 (HH:mm)"),
                                fieldWithPath("endTime").description("도착 시간 (HH:mm)"),
                                fieldWithPath("displayOrder").description("정렬 순서"),
                                fieldWithPath("createdAt").description("생성 일시"),
                                fieldWithPath("updatedAt").description("수정 일시")
                        )
                ));
    }

    @Test
    @DisplayName("북마크 수정")
    void updateBookmark() throws Exception {
        // given
        String requestJson = """
                {
                    "name": "강남 → 신촌",
                    "departureStationId": "228",
                    "arrivalStationId": "240",
                    "startTime": "09:00:00",
                    "endTime": "10:00:00"
                }
                """;
        BookmarkResponse updated = new BookmarkResponse(
                1L, "강남 → 신촌", "228", "240", "강남", "신촌",
                "2호선", "#00A84D", "2호선", "#00A84D",
                LocalTime.of(9, 0), LocalTime.of(10, 0), 1,
                LocalDateTime.of(2026, 3, 21, 9, 0), LocalDateTime.of(2026, 3, 21, 10, 0)
        );
        given(bookmarkService.updateBookmark(eq(1L), eq(1L), any())).willReturn(updated);

        // when & then
        mockMvc.perform(patch("/bookmarks/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andDo(restDocs.document(
                        pathParameters(
                                parameterWithName("id").description("수정할 북마크 ID")
                        ),
                        requestFields(
                                fieldWithPath("name").description("변경할 북마크 이름").optional(),
                                fieldWithPath("departureStationId").description("변경할 출발역 ID").optional(),
                                fieldWithPath("arrivalStationId").description("변경할 도착역 ID").optional(),
                                fieldWithPath("startTime").description("변경할 출발 시간").optional(),
                                fieldWithPath("endTime").description("변경할 도착 시간").optional()
                        ),
                        responseFields(
                                fieldWithPath("id").description("북마크 ID"),
                                fieldWithPath("name").description("북마크 이름"),
                                fieldWithPath("departureStationId").description("출발역 ID"),
                                fieldWithPath("arrivalStationId").description("도착역 ID"),
                                fieldWithPath("departureStationName").description("출발역 이름"),
                                fieldWithPath("arrivalStationName").description("도착역 이름"),
                                fieldWithPath("departureLineName").description("출발역 노선 이름"),
                                fieldWithPath("departureLineColor").description("출발역 노선 색상"),
                                fieldWithPath("arrivalLineName").description("도착역 노선 이름"),
                                fieldWithPath("arrivalLineColor").description("도착역 노선 색상"),
                                fieldWithPath("startTime").description("출발 시간 (HH:mm)"),
                                fieldWithPath("endTime").description("도착 시간 (HH:mm)"),
                                fieldWithPath("displayOrder").description("정렬 순서"),
                                fieldWithPath("createdAt").description("생성 일시"),
                                fieldWithPath("updatedAt").description("수정 일시")
                        )
                ));
    }

    @Test
    @DisplayName("북마크 순서 변경")
    void reorderBookmarks() throws Exception {
        // given
        BookmarkReorderRequest request = new BookmarkReorderRequest(
                List.of(
                        new BookmarkReorderRequest.ReorderItem(1L, 0),
                        new BookmarkReorderRequest.ReorderItem(2L, 1)
                )
        );
        willDoNothing().given(bookmarkService).reorderBookmarks(eq(1L), any());

        // when & then
        mockMvc.perform(put("/bookmarks/reorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(restDocs.document(
                        requestFields(
                                fieldWithPath("items").description("순서 변경 항목 목록"),
                                fieldWithPath("items[].bookmarkId").description("북마크 ID"),
                                fieldWithPath("items[].newDisplayOrder").description("변경할 순서 (0부터 시작)")
                        )
                ));
    }

    @Test
    @DisplayName("북마크 목록 조회")
    void getBookmarks() throws Exception {
        // given
        List<BookmarkResponse> responses = List.of(sampleBookmarkResponse());
        given(bookmarkService.getBookmarks(1L)).willReturn(responses);

        // when & then
        mockMvc.perform(get("/bookmarks"))
                .andExpect(status().isOk())
                .andDo(restDocs.document(
                        responseFields(
                                fieldWithPath("[].id").description("북마크 ID"),
                                fieldWithPath("[].name").description("북마크 이름"),
                                fieldWithPath("[].departureStationId").description("출발역 ID"),
                                fieldWithPath("[].arrivalStationId").description("도착역 ID"),
                                fieldWithPath("[].departureStationName").description("출발역 이름"),
                                fieldWithPath("[].arrivalStationName").description("도착역 이름"),
                                fieldWithPath("[].departureLineName").description("출발역 노선 이름"),
                                fieldWithPath("[].departureLineColor").description("출발역 노선 색상"),
                                fieldWithPath("[].arrivalLineName").description("도착역 노선 이름"),
                                fieldWithPath("[].arrivalLineColor").description("도착역 노선 색상"),
                                fieldWithPath("[].startTime").description("출발 시간 (HH:mm)"),
                                fieldWithPath("[].endTime").description("도착 시간 (HH:mm)"),
                                fieldWithPath("[].displayOrder").description("정렬 순서"),
                                fieldWithPath("[].createdAt").description("생성 일시"),
                                fieldWithPath("[].updatedAt").description("수정 일시")
                        )
                ));
    }

    @Test
    @DisplayName("북마크 단건 조회")
    void getBookmark() throws Exception {
        // given
        given(bookmarkService.getBookmark(1L, 1L)).willReturn(sampleBookmarkResponse());

        // when & then
        mockMvc.perform(get("/bookmarks/{id}", 1L))
                .andExpect(status().isOk())
                .andDo(restDocs.document(
                        pathParameters(
                                parameterWithName("id").description("조회할 북마크 ID")
                        ),
                        responseFields(
                                fieldWithPath("id").description("북마크 ID"),
                                fieldWithPath("name").description("북마크 이름"),
                                fieldWithPath("departureStationId").description("출발역 ID"),
                                fieldWithPath("arrivalStationId").description("도착역 ID"),
                                fieldWithPath("departureStationName").description("출발역 이름"),
                                fieldWithPath("arrivalStationName").description("도착역 이름"),
                                fieldWithPath("departureLineName").description("출발역 노선 이름"),
                                fieldWithPath("departureLineColor").description("출발역 노선 색상"),
                                fieldWithPath("arrivalLineName").description("도착역 노선 이름"),
                                fieldWithPath("arrivalLineColor").description("도착역 노선 색상"),
                                fieldWithPath("startTime").description("출발 시간 (HH:mm)"),
                                fieldWithPath("endTime").description("도착 시간 (HH:mm)"),
                                fieldWithPath("displayOrder").description("정렬 순서"),
                                fieldWithPath("createdAt").description("생성 일시"),
                                fieldWithPath("updatedAt").description("수정 일시")
                        )
                ));
    }

    @Test
    @DisplayName("전체 북마크 삭제")
    void deleteAllBookmarks() throws Exception {
        // given
        willDoNothing().given(bookmarkService).deleteAllBookmarks(1L);

        // when & then
        mockMvc.perform(delete("/bookmarks"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("북마크 단건 삭제")
    void deleteBookmark() throws Exception {
        // given
        willDoNothing().given(bookmarkService).deleteBookmark(1L, 1L);

        // when & then
        mockMvc.perform(delete("/bookmarks/{id}", 1L))
                .andExpect(status().isOk())
                .andDo(restDocs.document(
                        pathParameters(
                                parameterWithName("id").description("삭제할 북마크 ID")
                        )
                ));
    }
}
