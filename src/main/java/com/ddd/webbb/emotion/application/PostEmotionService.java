package com.ddd.webbb.emotion.application;

import com.ddd.webbb.emotion.domain.EmotionType;
import com.ddd.webbb.emotion.domain.PostEmotion;
import com.ddd.webbb.emotion.domain.PostEmotionRepository;
import com.ddd.webbb.global.common.exception.AppException;
import com.ddd.webbb.global.common.exception.ErrorCode;
import com.ddd.webbb.post.domain.Post;
import com.ddd.webbb.user.domain.User;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PostEmotionService {

    private final PostEmotionRepository postEmotionRepository;

    public PostEmotionService(PostEmotionRepository postEmotionRepository) {
        this.postEmotionRepository = postEmotionRepository;
    }

    @Transactional
    public PostEmotion addPostEmotion(Post post, EmotionType emotionType, User user) {
        return postEmotionRepository.save(PostEmotion.create(post, emotionType, user));
    }

    public PostEmotion findByPost(Long postId) {
        return postEmotionRepository
                .findByPost_Id(postId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    public List<PostEmotion> findByPostIds(List<Long> postIds) {
        return postEmotionRepository.findByPost_IdIn(postIds);
    }

    @Transactional
    public void modifyPostEmotion(Long postId, EmotionType emotionType) {
        PostEmotion postEmotion = findByPost(postId);
        postEmotion.updateEmotionType(emotionType);
    }
}
