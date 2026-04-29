package com.company.saju.saju.application.dto;

/**
 * 5가지 오행 에너지 점수 (합계 100).
 * ElementAnalyzer가 사주 8자에서 각 오행의 가중치를 계산하여 반환.
 */
public record ElementsScore(
        int wood,
        int fire,
        int earth,
        int metal,
        int water
) {
    public static ElementsScore of(int wood, int fire, int earth, int metal, int water) {
        return new ElementsScore(wood, fire, earth, metal, water);
    }
}
