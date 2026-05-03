package com.dnd.webbb.monster.domain;

import com.dnd.webbb.emotion.domain.EmotionType;
import com.dnd.webbb.global.common.entity.BaseEntity;
import com.dnd.webbb.post.domain.Post;
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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "monster",
        uniqueConstraints = @UniqueConstraint(columnNames = {"post_id"}))
public class Monster extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmotionType emotionType;

    @Column(nullable = false)
    private int hp;

    @Column(nullable = false)
    private int maxHp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MonsterStatus status = MonsterStatus.ALIVE;

    protected Monster() {}

    public static Monster create(Post post, EmotionType emotionType, int maxHp) {
        Monster monster = new Monster();
        monster.post = post;
        monster.emotionType = emotionType;
        monster.maxHp = maxHp;
        monster.hp = maxHp;
        monster.status = MonsterStatus.ALIVE;
        return monster;
    }

    public void decreaseHp(int delta) {
        this.hp = Math.max(0, this.hp - delta);
        if (this.hp == 0) {
            this.status = MonsterStatus.DEAD;
        }
    }

    public Long getId() {
        return id;
    }

    public Post getPost() {
        return post;
    }

    public EmotionType getEmotionType() {
        return emotionType;
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public MonsterStatus getStatus() {
        return status;
    }
}
