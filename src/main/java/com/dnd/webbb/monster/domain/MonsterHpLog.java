package com.dnd.webbb.monster.domain;

import com.dnd.webbb.comment.domain.Comment;
import com.dnd.webbb.global.common.entity.BaseCreatedEntity;
import com.dnd.webbb.post.domain.Post;
import com.dnd.webbb.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "monster_hp_log")
public class MonsterHpLog extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monster_id", nullable = false)
    private Monster monster;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private Comment comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HpActionType actionType;

    @Column(nullable = false)
    private int hpDelta;

    @Column(nullable = false)
    private int beforeHp;

    @Column(nullable = false)
    private int afterHp;

    protected MonsterHpLog() {}

    public static MonsterHpLog create(
            Monster monster,
            User user,
            Post post,
            Comment comment,
            HpActionType actionType,
            int hpDelta,
            int beforeHp,
            int afterHp) {
        MonsterHpLog log = new MonsterHpLog();
        log.monster = monster;
        log.user = user;
        log.post = post;
        log.comment = comment;
        log.actionType = actionType;
        log.hpDelta = hpDelta;
        log.beforeHp = beforeHp;
        log.afterHp = afterHp;
        return log;
    }

    public Long getId() {
        return id;
    }

    public Monster getMonster() {
        return monster;
    }

    public User getUser() {
        return user;
    }

    public Post getPost() {
        return post;
    }

    public Comment getComment() {
        return comment;
    }

    public HpActionType getActionType() {
        return actionType;
    }

    public int getHpDelta() {
        return hpDelta;
    }

    public int getBeforeHp() {
        return beforeHp;
    }

    public int getAfterHp() {
        return afterHp;
    }
}
