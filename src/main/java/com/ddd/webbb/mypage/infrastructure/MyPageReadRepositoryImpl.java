package com.ddd.webbb.mypage.infrastructure;

import com.ddd.webbb.comment.domain.Comment;
import com.ddd.webbb.comment.domain.CommentQueryRepository;
import com.ddd.webbb.monster.domain.Monster;
import com.ddd.webbb.monster.domain.MonsterRepository;
import com.ddd.webbb.mypage.domain.MyPageReadRepository;
import com.ddd.webbb.post.domain.Post;
import com.ddd.webbb.post.domain.PostLike;
import com.ddd.webbb.post.domain.PostQueryRepository;
import com.ddd.webbb.post.domain.QPost;
import com.ddd.webbb.post.domain.QPostLike;
import com.ddd.webbb.user.domain.QUser;
import com.ddd.webbb.user.domain.User;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class MyPageReadRepositoryImpl implements MyPageReadRepository {

    private final PostQueryRepository postQueryRepository;
    private final CommentQueryRepository commentQueryRepository;
    private final MonsterRepository monsterRepository;
    private final JPAQueryFactory queryFactory;

    public MyPageReadRepositoryImpl(
            PostQueryRepository postQueryRepository,
            CommentQueryRepository commentQueryRepository,
            MonsterRepository monsterRepository,
            JPAQueryFactory queryFactory) {
        this.postQueryRepository = postQueryRepository;
        this.commentQueryRepository = commentQueryRepository;
        this.monsterRepository = monsterRepository;
        this.queryFactory = queryFactory;
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
    public List<PostLike> findLikedPosts(User user, Long cursor, int size) {
        QPostLike postLike = QPostLike.postLike;
        QPost post = QPost.post;
        QUser queryUser = QUser.user;

        return queryFactory
                .selectFrom(postLike)
                .join(postLike.post, post)
                .fetchJoin()
                .join(post.user, queryUser)
                .fetchJoin()
                .where(
                        postLike.user.eq(user),
                        post.isDeleted.isFalse(),
                        ltPostLikeId(postLike, cursor))
                .orderBy(postLike.id.desc())
                .limit(size + 1L)
                .fetch();
    }

    @Override
    public List<Monster> findMonstersByPostIds(List<Long> postIds) {
        return postIds.isEmpty() ? List.of() : monsterRepository.findByPost_IdIn(postIds);
    }

    @Override
    public List<Monster> findMonstersByUserId(Long userId) {
        return monsterRepository.findByPost_UserIdAndPost_IsDeletedFalse(userId);
    }

    private BooleanExpression ltPostLikeId(QPostLike postLike, Long cursor) {
        return cursor != null ? postLike.id.lt(cursor) : null;
    }
}
