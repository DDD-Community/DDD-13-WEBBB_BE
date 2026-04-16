package com.dnd.webbb.user.infrastructure;

import com.dnd.webbb.user.domain.QUser;
import com.dnd.webbb.user.domain.User;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepositoryImpl {

    private final JPAQueryFactory queryFactory;

    public UserRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public List<User> findByCursor(Long cursor, int size) {
        QUser user = QUser.user;

        return queryFactory
                .selectFrom(user)
                .where(user.deletedAt.isNull(), cursor != null ? user.id.lt(cursor) : null)
                .orderBy(user.id.desc())
                .limit(size)
                .fetch();
    }
}
