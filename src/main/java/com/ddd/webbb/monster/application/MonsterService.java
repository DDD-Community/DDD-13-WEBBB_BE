package com.ddd.webbb.monster.application;

import com.ddd.webbb.comment.domain.Comment;
import com.ddd.webbb.emotion.domain.EmotionType;
import com.ddd.webbb.global.common.exception.AppException;
import com.ddd.webbb.global.common.exception.ErrorCode;
import com.ddd.webbb.monster.domain.HpActionType;
import com.ddd.webbb.monster.domain.Monster;
import com.ddd.webbb.monster.domain.MonsterHpLog;
import com.ddd.webbb.monster.domain.MonsterHpLogRepository;
import com.ddd.webbb.monster.domain.MonsterRepository;
import com.ddd.webbb.post.domain.Post;
import com.ddd.webbb.user.domain.User;
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
    private final MonsterHpLogRepository monsterHpLogRepository;

    public MonsterService(
            MonsterRepository monsterRepository, MonsterHpLogRepository monsterHpLogRepository) {
        this.monsterRepository = monsterRepository;
        this.monsterHpLogRepository = monsterHpLogRepository;
    }

    public Monster getMonsterByPostId(Long postId) {
        return monsterRepository
                .findByPostId(postId)
                .orElseThrow(() -> new AppException(ErrorCode.MONSTER_NOT_FOUND));
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
    public Monster addMonster(Post post, EmotionType emotionType, int hp) {
        return monsterRepository.save(Monster.create(post, emotionType, normalizeHp(hp)));
    }

    @Transactional
    public void decreaseMonsterHp(
            Monster monster,
            User user,
            Post post,
            Comment comment,
            HpActionType actionType,
            int delta) {
        int beforeHp = monster.getHp();
        monster.decreaseHp(delta);
        int afterHp = monster.getHp();
        monsterHpLogRepository.save(
                MonsterHpLog.create(
                        monster, user, post, comment, actionType, delta, beforeHp, afterHp));
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
