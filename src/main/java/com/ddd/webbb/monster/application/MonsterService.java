package com.ddd.webbb.monster.application;

import com.ddd.webbb.emotion.domain.EmotionType;
import com.ddd.webbb.monster.domain.Monster;
import com.ddd.webbb.monster.domain.MonsterRepository;
import com.ddd.webbb.post.domain.Post;
import org.springframework.stereotype.Service;

@Service
public class MonsterService {

    private static final int LOW_HP = 10;
    private static final int MID_HP = 20;
    private static final int HIGH_HP = 30;

    private final MonsterRepository monsterRepository;

    public MonsterService(MonsterRepository monsterRepository) {
        this.monsterRepository = monsterRepository;
    }

    public Monster addMonster(Post post, EmotionType emotionType, int hp) {
        return monsterRepository.save(Monster.create(post, emotionType, normalizeHp(hp)));
    }

    private int normalizeHp(int hp) {
        if (hp <= LOW_HP) {
            return LOW_HP;
        }
        if (hp <= MID_HP) {
            return MID_HP;
        }
        return HIGH_HP;
    }
}
