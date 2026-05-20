package com.ddd.webbb.emotion.application;

import com.ddd.webbb.emotion.domain.EmotionType;
import com.ddd.webbb.emotion.domain.PostEmotion;
import com.ddd.webbb.emotion.domain.PostEmotionRepository;
import com.ddd.webbb.post.domain.Post;
import com.ddd.webbb.user.domain.User;
import org.springframework.stereotype.Service;

@Service
public class PostEmotionService {

    private final PostEmotionRepository postEmotionRepository;

    public PostEmotionService(PostEmotionRepository postEmotionRepository) {
        this.postEmotionRepository = postEmotionRepository;
    }

    public PostEmotion addPostEmotion(Post post, EmotionType emotionType, User user) {
        return postEmotionRepository.save(PostEmotion.create(post, emotionType, user));
    }
}
