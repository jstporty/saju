package com.company.saju.saju.adapter.in.web.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CategoryFortunesResponse {
    private List<CategoryEntry> categories;

    @Getter
    @Builder
    public static class CategoryEntry {
        private String category;
        private String label;
        private String icon;
        private String message;
    }
}
