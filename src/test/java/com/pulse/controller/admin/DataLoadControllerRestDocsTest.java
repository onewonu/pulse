package com.pulse.controller.admin;

import com.pulse.dto.dataload.DataLoadResponse;
import com.pulse.service.dataload.subway.SubwayMasterDataLoadService;
import com.pulse.service.dataload.subway.SubwayPassengerHourlyService;
import com.pulse.service.dataload.subway.SubwayStatisticsDataLoadService;
import com.pulse.service.dataload.subway.TrainScheduleDataLoadService;
import com.pulse.support.RestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DataLoadControllerRestDocsTest extends RestDocsSupport {

    private final SubwayMasterDataLoadService subwayMasterDataLoadService = mock(SubwayMasterDataLoadService.class);
    private final SubwayStatisticsDataLoadService subwayStatisticsDataLoadService = mock(SubwayStatisticsDataLoadService.class);
    private final SubwayPassengerHourlyService subwayPassengerHourlyService = mock(SubwayPassengerHourlyService.class);
    private final TrainScheduleDataLoadService trainScheduleDataLoadService = mock(TrainScheduleDataLoadService.class);

    @Override
    protected Object controller() {
        return new DataLoadController(subwayMasterDataLoadService, subwayStatisticsDataLoadService, subwayPassengerHourlyService, trainScheduleDataLoadService);
    }

    private DataLoadResponse sampleSuccess(String category, int count) {
        return new DataLoadResponse(true, category, count, count + " Loading completed", LocalDateTime.of(2026, 3, 21, 9, 0));
    }

    @Test
    @DisplayName("지하철 마스터 데이터 로드")
    void loadSubwayMasterData() throws Exception {
        // given
        given(subwayMasterDataLoadService.loadMasterDataFromJson()).willReturn(sampleSuccess("subwayMaster", 300));

        // when & then
        mockMvc.perform(post("/admin/data-load/subway/master"))
                .andExpect(status().isOk())
                .andDo(restDocs.document(
                        responseFields(
                                fieldWithPath("success").description("처리 성공 여부"),
                                fieldWithPath("dataCategory").description("데이터 카테고리"),
                                fieldWithPath("totalCount").description("처리된 데이터 수"),
                                fieldWithPath("message").description("처리 결과 메시지"),
                                fieldWithPath("loadedAt").description("처리 완료 일시")
                        )
                ));
    }

    @Test
    @DisplayName("지하철 통계 데이터 로드")
    void loadSubwayStatistics() throws Exception {
        // given
        given(subwayStatisticsDataLoadService.loadSubwayStatisticsData(any()))
                .willReturn(sampleSuccess("subwayStatistics", 1500));

        // when & then
        mockMvc.perform(post("/admin/data-load/subway/statistics?yearMonth=202603"))
                .andExpect(status().isOk())
                .andDo(restDocs.document(
                        queryParameters(
                                parameterWithName("yearMonth").description("조회 연월 (yyyyMM 형식, 6자리)")
                        ),
                        responseFields(
                                fieldWithPath("success").description("처리 성공 여부"),
                                fieldWithPath("dataCategory").description("데이터 카테고리"),
                                fieldWithPath("totalCount").description("처리된 데이터 수"),
                                fieldWithPath("message").description("처리 결과 메시지"),
                                fieldWithPath("loadedAt").description("처리 완료 일시")
                        )
                ));
    }

    @Test
    @DisplayName("전체 데이터 로드")
    void loadAllData() throws Exception {
        // given
        given(subwayMasterDataLoadService.loadMasterDataFromJson()).willReturn(sampleSuccess("subwayMaster", 300));
        given(subwayStatisticsDataLoadService.loadSubwayStatisticsData(any())).willReturn(sampleSuccess("subwayStatistics", 1500));

        // when & then
        mockMvc.perform(post("/admin/data-load/all?yearMonth=202603"))
                .andExpect(status().isOk())
                .andDo(restDocs.document(
                        queryParameters(
                                parameterWithName("yearMonth").description("조회 연월 (yyyyMM 형식, 6자리)")
                        ),
                        responseFields(
                                fieldWithPath("subwayMaster.success").description("마스터 데이터 처리 성공 여부"),
                                fieldWithPath("subwayMaster.dataCategory").description("마스터 데이터 카테고리"),
                                fieldWithPath("subwayMaster.totalCount").description("마스터 데이터 처리 수"),
                                fieldWithPath("subwayMaster.message").description("마스터 데이터 처리 메시지"),
                                fieldWithPath("subwayMaster.loadedAt").description("마스터 데이터 처리 완료 일시"),
                                fieldWithPath("subwayStatistics.success").description("통계 데이터 처리 성공 여부"),
                                fieldWithPath("subwayStatistics.dataCategory").description("통계 데이터 카테고리"),
                                fieldWithPath("subwayStatistics.totalCount").description("통계 데이터 처리 수"),
                                fieldWithPath("subwayStatistics.message").description("통계 데이터 처리 메시지"),
                                fieldWithPath("subwayStatistics.loadedAt").description("통계 데이터 처리 완료 일시")
                        )
                ));
    }

    @Test
    @DisplayName("지하철 통계 데이터 삭제")
    void deleteSubwayStatistics() throws Exception {
        // given
        given(subwayPassengerHourlyService.deleteByYearMonth(any()))
                .willReturn(sampleSuccess("subwayStatistics", 1500));

        // when & then
        mockMvc.perform(delete("/admin/data-load/subway/statistics?yearMonth=202603"))
                .andExpect(status().isOk())
                .andDo(restDocs.document(
                        queryParameters(
                                parameterWithName("yearMonth").description("삭제할 연월 (yyyyMM 형식, 6자리)")
                        ),
                        responseFields(
                                fieldWithPath("success").description("처리 성공 여부"),
                                fieldWithPath("dataCategory").description("데이터 카테고리"),
                                fieldWithPath("totalCount").description("삭제된 데이터 수"),
                                fieldWithPath("message").description("처리 결과 메시지"),
                                fieldWithPath("loadedAt").description("처리 완료 일시")
                        )
                ));
    }

    @Test
    @DisplayName("전체 열차 스케줄 로드")
    void loadAllTrainSchedules() throws Exception {
        // given
        given(trainScheduleDataLoadService.loadTrainSchedules(any()))
                .willReturn(sampleSuccess("trainSchedule", 500));

        // when & then
        mockMvc.perform(post("/admin/data-load/train-schedule/all"))
                .andExpect(status().isOk())
                .andDo(restDocs.document(
                        responseFields(
                                fieldWithPath("평일.success").description("평일 스케줄 처리 성공 여부"),
                                fieldWithPath("평일.dataCategory").description("평일 스케줄 카테고리"),
                                fieldWithPath("평일.totalCount").description("평일 스케줄 처리 수"),
                                fieldWithPath("평일.message").description("평일 스케줄 처리 메시지"),
                                fieldWithPath("평일.loadedAt").description("평일 스케줄 처리 완료 일시"),
                                fieldWithPath("주말.success").description("주말 스케줄 처리 성공 여부"),
                                fieldWithPath("주말.dataCategory").description("주말 스케줄 카테고리"),
                                fieldWithPath("주말.totalCount").description("주말 스케줄 처리 수"),
                                fieldWithPath("주말.message").description("주말 스케줄 처리 메시지"),
                                fieldWithPath("주말.loadedAt").description("주말 스케줄 처리 완료 일시")
                        )
                ));
    }

    @Test
    @DisplayName("전체 열차 스케줄 삭제")
    void deleteAllTrainSchedules() throws Exception {
        // given
        given(trainScheduleDataLoadService.deleteAllTrainSchedules())
                .willReturn(sampleSuccess("trainSchedule", 1000));

        // when & then
        mockMvc.perform(delete("/admin/data-load/train-schedule/all"))
                .andExpect(status().isOk())
                .andDo(restDocs.document(
                        responseFields(
                                fieldWithPath("success").description("처리 성공 여부"),
                                fieldWithPath("dataCategory").description("데이터 카테고리"),
                                fieldWithPath("totalCount").description("삭제된 데이터 수"),
                                fieldWithPath("message").description("처리 결과 메시지"),
                                fieldWithPath("loadedAt").description("처리 완료 일시")
                        )
                ));
    }
}
