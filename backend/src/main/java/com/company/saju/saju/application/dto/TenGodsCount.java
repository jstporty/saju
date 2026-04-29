package com.company.saju.saju.application.dto;

import java.util.List;

/**
 * 십신 등장 횟수 카운트 결과.
 * count 내림차순 상위 5개 + 일주 카드(항상 포함) = 6장.
 */
public record TenGodsCount(List<TenGodEntry> entries) {

    public record TenGodEntry(String tenGod, int count, String keyword) {}

    public static TenGodsCount of(List<TenGodEntry> entries) {
        return new TenGodsCount(List.copyOf(entries));
    }
}
