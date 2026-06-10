package com.ddd.webbb.mypage.application;

import com.ddd.webbb.emotion.domain.EmotionType;
import com.ddd.webbb.monster.application.MonsterService;
import com.ddd.webbb.monster.domain.Monster;
import com.ddd.webbb.monster.domain.MonsterStatus;
import com.ddd.webbb.mypage.interfaces.dto.MonsterStatsResponse;
import com.ddd.webbb.user.application.UserService;
import com.ddd.webbb.user.domain.User;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final MonsterService monsterService;
    private final UserService userService;

    public MonsterStatsResponse getMonsterStats(UUID publicId) {
        User user = userService.getUserEntity(publicId);
        List<Monster> monsters = monsterService.findByUserId(user.getId());

        int total = monsters.size();
        int defeated =
                (int) monsters.stream().filter(m -> m.getStatus() == MonsterStatus.DEAD).count();

        MonsterStatsResponse.MostFrequentEmotion mostFrequent = buildMostFrequent(monsters, total);
        return new MonsterStatsResponse(total, defeated, mostFrequent);
    }

    private MonsterStatsResponse.MostFrequentEmotion buildMostFrequent(
            List<Monster> monsters, int total) {
        if (monsters.isEmpty()) {
            return null;
        }
        Map<EmotionType, Long> freq =
                monsters.stream()
                        .collect(
                                Collectors.groupingBy(
                                        Monster::getEmotionType, Collectors.counting()));
        EmotionType top = Collections.max(freq.entrySet(), Map.Entry.comparingByValue()).getKey();
        int topCount = freq.get(top).intValue();
        int percentage = (int) Math.round(topCount * 100.0 / total);
        return new MonsterStatsResponse.MostFrequentEmotion(
                top.name(), top.getDisplayName(), topCount, percentage);
    }
}
