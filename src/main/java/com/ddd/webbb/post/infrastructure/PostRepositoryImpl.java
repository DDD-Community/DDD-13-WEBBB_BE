package com.ddd.webbb.post.infrastructure;

import com.ddd.webbb.post.application.PostSearchCondition;
import com.ddd.webbb.post.domain.Post;
import com.ddd.webbb.post.domain.PostQueryRepository;
import com.ddd.webbb.post.domain.QPost;
import com.ddd.webbb.user.domain.QUser;
import com.ddd.webbb.user.domain.User;
import com.querydsl.core.types.dsl.BooleanExpression;
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
        return findByCursor(cursor, size, PostSearchCondition.empty());
    }

    public List<Post> findByCursor(Long cursor, int size, PostSearchCondition condition) {
        QPost post = QPost.post;
        QUser user = QUser.user;
        return queryFactory
                .selectFrom(post)
                .join(post.user, user)
                .fetchJoin()
                .where(
                        post.isDeleted.isFalse(),
                        cursorCondition(post, cursor),
                        jobRoleCondition(user, condition),
                        careerYearCondition(user, condition))
                .orderBy(post.id.desc())
                .limit(size + 1L)
                .fetch();
    }

    @Override
    public List<Post> findByUserWithCursor(User user, Long cursor, int size) {
        QPost post = QPost.post;
        QUser queryUser = QUser.user;
        return queryFactory
                .selectFrom(post)
                .join(post.user, queryUser)
                .fetchJoin()
                .where(post.user.eq(user), post.isDeleted.isFalse(), cursorCondition(post, cursor))
                .orderBy(post.id.desc())
                .limit(size + 1L)
                .fetch();
    }

    private BooleanExpression cursorCondition(QPost post, Long cursor) {
        return cursor != null ? post.id.lt(cursor) : null;
    }

    private BooleanExpression jobRoleCondition(QUser user, PostSearchCondition condition) {
        return condition.jobRoles().isEmpty() ? null : user.jobType.in(condition.jobRoles());
    }

    private BooleanExpression careerYearCondition(QUser user, PostSearchCondition condition) {
        return condition.careerYears().isEmpty()
                ? null
                : user.careerLevel.in(condition.careerYears());
    }
}
