package com.company.saju.saju.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class GanJi {
    private final String gan;      // 천간 (甲, 乙, ...)
    private final String ji;       // 지지 (子, 丑, ...)
    private final Element element; // 오행
    private final boolean isYang;  // 음양 (true: 양, false: 음)

    public String getGanJi() {
        return gan + ji;
    }

    @Override
    public String toString() {
        return gan + ji + "(" + element.getKorean() + ")";
    }
}
