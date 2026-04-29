package com.company.saju.saju.adapter.out.engine;

import com.company.saju.saju.application.dto.DayPillar;
import com.nlf.calendar.Solar;
import com.nlf.calendar.Lunar;

import java.time.LocalDate;

/**
 * 임의 날짜의 일진(천간/지지) 계산.
 * daily_fortune_template 룩업 키 (day_stem × daily_stem × daily_branch).
 */
class DailyStemBranchProvider {

    DayPillar of(LocalDate date) {
        Solar solar = Solar.fromYmd(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
        Lunar lunar = solar.getLunar();
        String dayInGanZhi = lunar.getDayInGanZhi();
        // getDayInGanZhi() 반환: "甲子" 형식 (2글자)
        if (dayInGanZhi == null || dayInGanZhi.length() < 2) {
            return new DayPillar("甲", "子"); // fallback
        }
        String stem = String.valueOf(dayInGanZhi.charAt(0));
        String branch = String.valueOf(dayInGanZhi.charAt(1));
        return new DayPillar(stem, branch);
    }
}
