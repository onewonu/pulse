package com.pulse.controller.web;

import com.pulse.dto.StationSearchResult;
import com.pulse.dto.TimeRecommendationResult;
import com.pulse.dto.CongestionLevel;
import com.pulse.service.search.StationSearchService;
import com.pulse.service.search.TimeRecommendationService;
import com.pulse.support.RestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StationSearchControllerRestDocsTest extends RestDocsSupport {

    private final StationSearchService stationSearchService = mock(StationSearchService.class);
    private final TimeRecommendationService timeRecommendationService = mock(TimeRecommendationService.class);

    @Override
    protected Object controller() {
        return new StationSearchController(stationSearchService, timeRecommendationService);
    }

    @Test
    @DisplayName("역 이름으로 역 검색")
    void searchStation() throws Exception {
        // given
        StationSearchResult result = new StationSearchResult(
                2,
                List.of(
                        new StationSearchResult.StationItem("강남", "228", 127.0276, 37.4979, "2호선", "#00A84D"),
                        new StationSearchResult.StationItem("강남구청", "734", 127.0435, 37.5172, "7호선", "#747F00")
                )
        );
        given(stationSearchService.searchStation("강남")).willReturn(result);

        // when & then
        mockMvc.perform(get("/search/station")
                        .param("stationName", "강남"))
                .andExpect(status().isOk())
                .andDo(restDocs.document(
                        queryParameters(
                                parameterWithName("stationName").description("검색할 역 이름 (최소 2자 이상)")
                        ),
                        responseFields(
                                fieldWithPath("totalCount").description("검색된 역 총 개수"),
                                fieldWithPath("stations").description("역 목록"),
                                fieldWithPath("stations[].stationName").description("역 이름"),
                                fieldWithPath("stations[].stationID").description("역 ID"),
                                fieldWithPath("stations[].x").description("경도"),
                                fieldWithPath("stations[].y").description("위도"),
                                fieldWithPath("stations[].laneName").description("노선 이름"),
                                fieldWithPath("stations[].lineColor").description("노선 색상 (hex)")
                        )
                ));
    }

    @Test
    @DisplayName("출발/도착역과 시간대로 추천 시간 조회")
    void recommendTimes() throws Exception {
        // given
        TimeRecommendationResult result = new TimeRecommendationResult(
                "228",
                "150",
                "강남",
                "홍대입구",
                LocalDate.of(2026, 3, 21),
                "평일",
                List.of(
                        new TimeRecommendationResult.TimeRecommendation(
                                LocalTime.of(9, 0),
                                LocalTime.of(9, 45),
                                45,
                                1,
                                2500,
                                CongestionLevel.LOW,
                                List.of(
                                        new TimeRecommendationResult.StationCongestion(
                                                "228", "강남", "2호선", "#00A84D",
                                                LocalTime.of(9, 0), LocalTime.of(9, 2),
                                                1200, 800, 5000
                                        )
                                )
                        )
                ),
                "추천 시간대입니다."
        );
        given(timeRecommendationService.recommendTimes(any())).willReturn(result);

        // when & then
        mockMvc.perform(get("/search/route")
                        .param("departureStationId", "228")
                        .param("arrivalStationId", "150")
                        .param("searchDate", "2026-03-21")
                        .param("startTime", "09:00")
                        .param("endTime", "11:00"))
                .andExpect(status().isOk())
                .andDo(restDocs.document(
                        queryParameters(
                                parameterWithName("departureStationId").description("출발역 ID (숫자)"),
                                parameterWithName("arrivalStationId").description("도착역 ID (숫자)"),
                                parameterWithName("searchDate").description("조회 날짜 (yyyy-MM-dd)"),
                                parameterWithName("startTime").description("조회 시작 시간 (HH:mm)"),
                                parameterWithName("endTime").description("조회 종료 시간 (HH:mm)")
                        ),
                        responseFields(
                                fieldWithPath("departureStationId").description("출발역 ID"),
                                fieldWithPath("arrivalStationId").description("도착역 ID"),
                                fieldWithPath("departureStationName").description("출발역 이름"),
                                fieldWithPath("arrivalStationName").description("도착역 이름"),
                                fieldWithPath("travelDate").description("이동 날짜"),
                                fieldWithPath("dayType").description("요일 유형 (평일/주말)"),
                                fieldWithPath("message").description("결과 메시지"),
                                fieldWithPath("recommendations").description("추천 시간대 목록"),
                                fieldWithPath("recommendations[].departureTime").description("출발 시간 (HH:mm)"),
                                fieldWithPath("recommendations[].arrivalTime").description("도착 시간 (HH:mm)"),
                                fieldWithPath("recommendations[].totalTime").description("총 소요 시간 (분)"),
                                fieldWithPath("recommendations[].transferCount").description("환승 횟수"),
                                fieldWithPath("recommendations[].congestionScore").description("혼잡도 점수"),
                                fieldWithPath("recommendations[].congestionLevel").description("혼잡도 수준 (LOW/MEDIUM/HIGH)"),
                                fieldWithPath("recommendations[].stationCongestions").description("경유 역별 혼잡도 목록"),
                                fieldWithPath("recommendations[].stationCongestions[].stationId").description("역 ID"),
                                fieldWithPath("recommendations[].stationCongestions[].stationName").description("역 이름"),
                                fieldWithPath("recommendations[].stationCongestions[].lineName").description("노선 이름"),
                                fieldWithPath("recommendations[].stationCongestions[].lineColor").description("노선 색상"),
                                fieldWithPath("recommendations[].stationCongestions[].arrivalTime").description("도착 시간 (HH:mm)"),
                                fieldWithPath("recommendations[].stationCongestions[].departureTime").description("출발 시간 (HH:mm)"),
                                fieldWithPath("recommendations[].stationCongestions[].boardingCount").description("승차 인원"),
                                fieldWithPath("recommendations[].stationCongestions[].alightingCount").description("하차 인원"),
                                fieldWithPath("recommendations[].stationCongestions[].totalPassengers").description("총 승객 수")
                        )
                ));
    }
}
