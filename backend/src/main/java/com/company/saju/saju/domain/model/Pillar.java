package com.company.saju.saju.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class Pillar {
    private final GanJi ganJi;
    private final String pillarType;  // "year", "month", "day", "time"

    @Override
    public String toString() {
        return pillarType + "주: " + ganJi.toString();
    }
}
