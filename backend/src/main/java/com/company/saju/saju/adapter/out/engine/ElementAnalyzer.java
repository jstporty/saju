package com.company.saju.saju.adapter.out.engine;

import com.company.saju.saju.application.dto.ElementsScore;
import com.company.saju.saju.application.dto.FourPillarsDto;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 사주 8자(천간4 + 지지4)에서 오행 점수를 계산한다.
 * 각 글자에 동일 가중치(1점)를 부여하고, 합계를 100점으로 정규화.
 */
class ElementAnalyzer {

    // 동점 시 우선순위: 목>화>토>금>수 (전통 오행 순서)
    private static final java.util.List<String> ELEMENT_PRIORITY =
            java.util.List.of("WOOD", "FIRE", "EARTH", "METAL", "WATER");

    private static final Map<String, String> STEM_ELEMENT = Map.of(
            "甲", "WOOD", "乙", "WOOD",
            "丙", "FIRE", "丁", "FIRE",
            "戊", "EARTH", "己", "EARTH",
            "庚", "METAL", "辛", "METAL",
            "壬", "WATER", "癸", "WATER"
    );

    private static final Map<String, String> BRANCH_ELEMENT = Map.ofEntries(
            Map.entry("子", "WATER"), Map.entry("丑", "EARTH"),
            Map.entry("寅", "WOOD"), Map.entry("卯", "WOOD"),
            Map.entry("辰", "EARTH"), Map.entry("巳", "FIRE"),
            Map.entry("午", "FIRE"), Map.entry("未", "EARTH"),
            Map.entry("申", "METAL"), Map.entry("酉", "METAL"),
            Map.entry("戌", "EARTH"), Map.entry("亥", "WATER")
    );

    ElementsScore analyze(FourPillarsDto pillars) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String e : ELEMENT_PRIORITY) counts.put(e, 0);

        addStem(counts, pillars.year());
        addBranch(counts, pillars.year());
        addStem(counts, pillars.month());
        addBranch(counts, pillars.month());
        addStem(counts, pillars.day());
        addBranch(counts, pillars.day());
        if (pillars.hour() != null) {
            addStem(counts, pillars.hour());
            addBranch(counts, pillars.hour());
        }

        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) total = 1;

        final int t = total;
        return ElementsScore.of(
                round(counts.get("WOOD"), t),
                round(counts.get("FIRE"), t),
                round(counts.get("EARTH"), t),
                round(counts.get("METAL"), t),
                round(counts.get("WATER"), t)
        );
    }

    /** 최대값 오행 반환. 동점 시 목>화>토>금>수 우선순위. */
    String dominant(ElementsScore score) {
        Map<String, Integer> map = Map.of(
                "WOOD", score.wood(), "FIRE", score.fire(), "EARTH", score.earth(),
                "METAL", score.metal(), "WATER", score.water());

        return ELEMENT_PRIORITY.stream()
                .max(Comparator.comparingInt(map::get))
                // stable max: 앞의 것이 우선이므로 max가 여러 개면 첫 번째가 선택됨
                .orElse("WOOD");
    }

    private void addStem(Map<String, Integer> counts, FourPillarsDto.PillarDto pillar) {
        if (pillar == null) return;
        String elem = STEM_ELEMENT.get(pillar.stem());
        if (elem != null) counts.merge(elem, 1, Integer::sum);
    }

    private void addBranch(Map<String, Integer> counts, FourPillarsDto.PillarDto pillar) {
        if (pillar == null) return;
        String elem = BRANCH_ELEMENT.get(pillar.branch());
        if (elem != null) counts.merge(elem, 1, Integer::sum);
    }

    private int round(int count, int total) {
        return (int) Math.round((double) count / total * 100);
    }
}
