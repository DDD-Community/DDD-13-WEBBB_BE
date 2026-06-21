package com.ddd.webbb.mypage.infrastructure;

import com.ddd.webbb.comment.domain.Comment;
import com.ddd.webbb.comment.domain.CommentQueryRepository;
import com.ddd.webbb.monster.domain.Monster;
import com.ddd.webbb.monster.domain.MonsterRepository;
import com.ddd.webbb.mypage.domain.MyPageReadRepository;
import com.ddd.webbb.post.domain.Post;
import com.ddd.webbb.post.domain.PostQueryRepository;
import com.ddd.webbb.user.domain.User;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class MyPageReadRepositoryImpl implements MyPageReadRepository {

    private final PostQueryRepository postQueryRepository;
    private final CommentQueryRepository commentQueryRepository;
    private final MonsterRepository monsterRepository;

    public MyPageReadRepositoryImpl(
            PostQueryRepository postQueryRepository,
            CommentQueryRepository commentQueryRepository,
            MonsterRepository monsterRepository) {
        this.postQueryRepository = postQueryRepository;
        this.commentQueryRepository = commentQueryRepository;
        this.monsterRepository = monsterRepository;
    }

    @Override
    public List<Post> findMyPosts(User user, Long cursor, int size) {
        return postQueryRepository.findByUserWithCursor(user, cursor, size);
    }

    @Override
    public List<Comment> findMyComments(User user, Long cursor, int size) {
        return commentQueryRepository.findByUserWithCursor(user, cursor, size);
    }

    @Override
    public List<Monster> findMonstersByPostIds(List<Long> postIds) {
        return postIds.isEmpty() ? List.of() : monsterRepository.findByPost_IdIn(postIds);
    }

    @Override
    public List<Monster> findMonstersByUserId(Long userId) {
        return monsterRepository.findByPost_UserIdAndPost_IsDeletedFalse(userId);
    }
}
