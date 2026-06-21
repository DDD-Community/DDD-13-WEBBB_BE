package com.ddd.webbb.post.domain;

import com.ddd.webbb.user.domain.User;
import java.util.List;

public interface PostQueryRepository {
    List<Post> findByUserWithCursor(User user, Long cursor, int size);
}
