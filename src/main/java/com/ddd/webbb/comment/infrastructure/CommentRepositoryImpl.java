package com.ddd.webbb.comment.infrastructure;

import com.ddd.webbb.comment.domain.Comment;
import com.ddd.webbb.comment.domain.CommentQueryRepository;
import com.ddd.webbb.comment.domain.QComment;
import com.ddd.webbb.user.domain.QUser;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class CommentRepositoryImpl implements CommentQueryRepository {

    private final JPAQueryFactory queryFactory;

    public CommentRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public List<Comment> findRootCommentsByPostId(Long postId, Long cursor, int size) {
        QComment comment = QComment.comment;
        QUser user = QUser.user;

        return queryFactory
                .selectFrom(comment)
                .join(comment.user, user)
                .fetchJoin()
                .where(
                        comment.post.id.eq(postId),
                        comment.parent.isNull(),
                        comment.isDeleted.isFalse(),
                        cursor != null ? comment.id.lt(cursor) : null)
                .orderBy(comment.id.desc())
                .limit(size)
                .fetch();
    }

    public List<Comment> findRepliesByParentIds(List<Long> parentIds) {
        QComment comment = QComment.comment;
        QUser user = QUser.user;

        return queryFactory
                .selectFrom(comment)
                .join(comment.user, user)
                .fetchJoin()
                .where(comment.parent.id.in(parentIds), comment.isDeleted.isFalse())
                .orderBy(comment.id.asc())
                .fetch();
    }
}
