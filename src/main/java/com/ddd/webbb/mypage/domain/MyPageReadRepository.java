package com.ddd.webbb.mypage.domain;

import com.ddd.webbb.comment.domain.Comment;
import com.ddd.webbb.monster.domain.Monster;
import com.ddd.webbb.post.domain.Post;
import com.ddd.webbb.post.domain.PostLike;
import com.ddd.webbb.user.domain.User;
import java.util.List;

public interface MyPageReadRepository {
    List<Post> findMyPosts(User user, Long cursor, int size);

    List<Comment> findMyComments(User user, Long cursor, int size);

    List<PostLike> findLikedPosts(User user, Long cursor, int size);

    List<Monster> findMonstersByPostIds(List<Long> postIds);

    List<Monster> findMonstersByUserId(Long userId);
}
