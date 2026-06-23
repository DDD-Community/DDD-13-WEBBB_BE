package com.ddd.webbb.mypage.application;

import com.ddd.webbb.comment.domain.Comment;
import com.ddd.webbb.emotion.domain.EmotionType;
import com.ddd.webbb.global.common.exception.AppException;
import com.ddd.webbb.global.common.exception.ErrorCode;
import com.ddd.webbb.monster.domain.Monster;
import com.ddd.webbb.monster.domain.MonsterStatus;
import com.ddd.webbb.mypage.domain.MyPageReadRepository;
import com.ddd.webbb.mypage.interfaces.dto.MonsterStatsResponse;
import com.ddd.webbb.mypage.interfaces.dto.MyCommentResponse;
import com.ddd.webbb.mypage.interfaces.dto.MyLikedPostResponse;
import com.ddd.webbb.mypage.interfaces.dto.MyPostResponse;
import com.ddd.webbb.post.domain.Post;
import com.ddd.webbb.post.domain.PostLike;
import com.ddd.webbb.user.application.UserService;
import com.ddd.webbb.user.domain.User;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserService userService;
    private final MyPageReadRepository myPageReadRepository;

    public MyPostResponse getMyPosts(UUID userPublicId, Long cursor, int size) {
        validateSize(size);
        User user = userService.getUserEntity(userPublicId);
        List<Post> fetched = myPageReadRepository.findMyPosts(user, cursor, size);
        boolean hasNext = fetched.size() > size;
        List<Post> posts = hasNext ? fetched.subList(0, size) : fetched;

        List<Long> postIds = posts.stream().map(Post::getId).toList();
        Map<Long, Monster> monsterByPostId =
                myPageReadRepository.findMonstersByPostIds(postIds).stream()
                        .collect(Collectors.toMap(m -> m.getPost().getId(), m -> m));

        List<MyPostResponse.MyPost> myPosts =
                posts.stream()
                        .map(
                                post ->
                                        MyPostResponse.MyPost.of(
                                                post, monsterByPostId.get(post.getId())))
                        .toList();

        Long nextCursor = hasNext ? posts.get(posts.size() - 1).getId() : null;
        return new MyPostResponse(myPosts, nextCursor);
    }

    public MyLikedPostResponse getLikedPosts(UUID userPublicId, Long cursor, int size) {
        validateSize(size);
        User user = userService.getUserEntity(userPublicId);
        List<PostLike> fetched = myPageReadRepository.findLikedPosts(user, cursor, size);

        boolean hasNext = fetched.size() > size;
        List<PostLike> likes = hasNext ? fetched.subList(0, size) : fetched;

        List<Long> postIds = likes.stream().map(pl -> pl.getPost().getId()).toList();
        Map<Long, Monster> monsterByPostId =
                myPageReadRepository.findMonstersByPostIds(postIds).stream()
                        .collect(Collectors.toMap(m -> m.getPost().getId(), m -> m));

        List<MyLikedPostResponse.LikedPost> posts =
                likes.stream()
                        .map(
                                pl ->
                                        MyLikedPostResponse.LikedPost.of(
                                                pl.getPost(),
                                                monsterByPostId.get(pl.getPost().getId())))
                        .toList();

        Long nextCursor = hasNext ? likes.get(likes.size() - 1).getId() : null;
        return new MyLikedPostResponse(posts, nextCursor);
    }

    public MyCommentResponse getMyComments(UUID userPublicId, Long cursor, int size) {
        validateSize(size);
        User user = userService.getUserEntity(userPublicId);
        List<Comment> fetched = myPageReadRepository.findMyComments(user, cursor, size);
        boolean hasNext = fetched.size() > size;
        List<Comment> comments = hasNext ? fetched.subList(0, size) : fetched;

        List<MyCommentResponse.MyComment> myComments =
                comments.stream().map(MyCommentResponse.MyComment::from).toList();

        Long nextCursor = hasNext ? comments.get(comments.size() - 1).getId() : null;
        return new MyCommentResponse(myComments, nextCursor);
    }

    public MonsterStatsResponse getMonsterStats(UUID publicId) {
        User user = userService.getUserEntity(publicId);
        List<Monster> monsters = myPageReadRepository.findMonstersByUserId(user.getId());

        int total = monsters.size();
        int defeated =
                (int) monsters.stream().filter(m -> m.getStatus() == MonsterStatus.DEAD).count();

        MonsterStatsResponse.MostFrequentEmotion mostFrequent = buildMostFrequent(monsters, total);
        return new MonsterStatsResponse(total, defeated, mostFrequent);
    }

    private MonsterStatsResponse.MostFrequentEmotion buildMostFrequent(
            List<Monster> monsters, int total) {
        if (monsters.isEmpty()) {
            return null;
        }
        Map<EmotionType, Long> freq =
                monsters.stream()
                        .collect(
                                Collectors.groupingBy(
                                        Monster::getEmotionType, Collectors.counting()));
        EmotionType top = Collections.max(freq.entrySet(), Map.Entry.comparingByValue()).getKey();
        int topCount = freq.get(top).intValue();
        int percentage = (int) Math.round(topCount * 100.0 / total);
        return new MonsterStatsResponse.MostFrequentEmotion(
                top.name(), top.getDisplayName(), topCount, percentage);
    }

    private void validateSize(int size) {
        if (size <= 0 || size > MAX_PAGE_SIZE) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
    }
}
