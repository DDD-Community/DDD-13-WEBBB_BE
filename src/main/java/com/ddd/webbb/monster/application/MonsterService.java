package com.ddd.webbb.monster.application;

import com.ddd.webbb.emotion.domain.EmotionType;
import com.ddd.webbb.global.common.exception.AppException;
import com.ddd.webbb.global.common.exception.ErrorCode;
import com.ddd.webbb.monster.domain.Monster;
import com.ddd.webbb.monster.domain.MonsterRepository;
import com.ddd.webbb.post.domain.Post;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MonsterService {

    private static final int LOW_HP = 10;
    private static final int MID_HP = 20;
    private static final int HIGH_HP = 30;

    private final MonsterRepository monsterRepository;

    public MonsterService(MonsterRepository monsterRepository) {
        this.monsterRepository = monsterRepository;
    }

    @Transactional
    public Monster addMonster(Post post, EmotionType emotionType, int hp) {
        return monsterRepository.save(Monster.create(post, emotionType, normalizeHp(hp)));
    }

    public Monster findByPost(Long postId) {
        return monsterRepository
                .findByPost_Id(postId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    public List<Monster> findByPostIds(List<Long> postIds) {
        return monsterRepository.findByPost_IdIn(postIds);
    }

    @Transactional
    public Monster resetMonster(Long postId, EmotionType emotionType, int hp) {
        Monster monster = findByPost(postId);
        monster.reset(emotionType, normalizeHp(hp));
        return monster;
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
