package com.ddd.webbb.mypage.interfaces.dto;

public record MonsterStatsResponse(
        int totalMonsterCount, int defeatedMonsterCount, MostFrequentEmotion mostFrequentEmotion) {

    public record MostFrequentEmotion(String type, String displayName, int count) {}
}
