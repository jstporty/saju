package com.company.saju.saju.domain.model;

public enum Element {
    WOOD("목", "木"),
    FIRE("화", "火"),
    EARTH("토", "土"),
    METAL("금", "金"),
    WATER("수", "水");

    private final String korean;
    private final String chinese;

    Element(String korean, String chinese) {
        this.korean = korean;
        this.chinese = chinese;
    }

    public String getKorean() {
        return korean;
    }

    public String getChinese() {
        return chinese;
    }
}
