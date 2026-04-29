package com.company.saju.saju.application.port.in;

import com.company.saju.saju.adapter.in.web.dto.ChartCreateRequest;
import com.company.saju.saju.adapter.in.web.dto.ChartResponse;

public interface CreateChartUseCase {
    ChartResponse createChart(ChartCreateRequest request, String userId);
}
