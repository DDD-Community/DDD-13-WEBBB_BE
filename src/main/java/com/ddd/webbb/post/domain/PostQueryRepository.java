package com.ddd.webbb.post.domain;

import com.ddd.webbb.user.domain.User;
import java.util.List;

public interface PostQueryRepository {
    List<Post> findByCursor(Long cursor, int size, PostSearchCondition condition);

    List<Post> findByCursor(
            Long cursor, Integer cursorLikeCount, int size, PostSearchCondition condition);

    List<Post> findByUserWithCursor(User user, Long cursor, int size);
}
