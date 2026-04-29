package com.company.saju.saju.application.port.in;

import com.company.saju.saju.adapter.in.web.dto.ChartResponse;
import com.company.saju.saju.adapter.in.web.dto.ChartSummaryDto;

import java.util.List;

public interface GetChartUseCase {
    ChartResponse getChart(String chartId, String userId);
    List<ChartSummaryDto> listCharts(String userId, int size, String cursor);
    void deleteChart(String chartId, String userId);
}
