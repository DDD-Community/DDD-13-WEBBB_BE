package com.ddd.webbb.comment.domain;

import java.util.List;

public interface CommentQueryRepository {
    List<Comment> findRootCommentsByPostId(Long postId, Long cursor, int size);

    List<Comment> findRepliesByParentIds(List<Long> parentIds);
}
