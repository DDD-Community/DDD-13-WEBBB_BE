package com.ddd.webbb.global.common.moderation;

import java.util.Comparator;
import java.util.List;

public class ProfanityFilter {

    private static final String MASK_CHAR = "*";

    private final List<String> bannedWords;

    public ProfanityFilter(List<String> bannedWords) {
        this.bannedWords =
                bannedWords.stream()
                        .filter(word -> word != null && !word.isBlank())
                        .map(String::trim)
                        .distinct()
                        .sorted(Comparator.comparingInt(String::length).reversed())
                        .toList();
    }

    public String mask(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String masked = text;
        for (String word : bannedWords) {
            masked = masked.replace(word, MASK_CHAR.repeat(word.length()));
        }
        return masked;
    }
}
