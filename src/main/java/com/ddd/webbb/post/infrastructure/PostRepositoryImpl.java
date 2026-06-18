package com.ddd.webbb.post.infrastructure;

import com.ddd.webbb.post.domain.Post;
import com.ddd.webbb.post.domain.PostQueryRepository;
import com.ddd.webbb.post.domain.QPost;
import com.ddd.webbb.user.domain.User;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class PostRepositoryImpl implements PostQueryRepository {

    private final JPAQueryFactory queryFactory;

    public PostRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public List<Post> findByCursor(Long cursor, int size) {
        QPost post = QPost.post;
        return queryFactory
                .selectFrom(post)
                .where(post.isDeleted.isFalse(), cursor != null ? post.id.lt(cursor) : null)
                .orderBy(post.id.desc())
                .limit(size + 1L)
                .fetch();
    }

    @Override
    public List<Post> findByUserWithCursor(User user, Long cursor, int size) {
        QPost post = QPost.post;
        return queryFactory
                .selectFrom(post)
                .where(
                        post.user.eq(user),
                        post.isDeleted.isFalse(),
                        cursor != null ? post.id.lt(cursor) : null)
                .orderBy(post.id.desc())
                .limit(size + 1L)
                .fetch();
    }
}
