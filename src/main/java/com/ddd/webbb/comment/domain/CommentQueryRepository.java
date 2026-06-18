package com.ddd.webbb.comment.domain;

import com.ddd.webbb.user.domain.User;
import java.util.List;

public interface CommentQueryRepository {
    List<Comment> findRootCommentsByPostId(Long postId, Long cursor, int size);

    List<Comment> findRepliesByParentIds(List<Long> parentIds);

    List<Comment> findByUserWithCursor(User user, Long cursor, int size);
}
