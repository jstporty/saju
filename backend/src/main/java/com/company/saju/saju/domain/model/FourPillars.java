package com.company.saju.saju.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class FourPillars {
    private final Pillar yearPillar;
    private final Pillar monthPillar;
    private final Pillar dayPillar;
    private final Pillar timePillar;

    public List<GanJi> getAllGanJi() {
        List<GanJi> allGanJi = new ArrayList<>();
        allGanJi.add(yearPillar.getGanJi());
        allGanJi.add(monthPillar.getGanJi());
        allGanJi.add(dayPillar.getGanJi());
        allGanJi.add(timePillar.getGanJi());
        return allGanJi;
    }

    public GanJi getDayGan() {
        return dayPillar.getGanJi();
    }

    @Override
    public String toString() {
        return String.format("%s %s %s %s",
            yearPillar.getGanJi().getGanJi(),
            monthPillar.getGanJi().getGanJi(),
            dayPillar.getGanJi().getGanJi(),
            timePillar.getGanJi().getGanJi()
        );
    }
}
