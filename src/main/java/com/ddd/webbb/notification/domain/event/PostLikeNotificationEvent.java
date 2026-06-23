package com.ddd.webbb.notification.domain.event;

import com.ddd.webbb.post.domain.Post;
import com.ddd.webbb.user.domain.User;

public record PostLikeNotificationEvent(User postOwner, User actor, Post post) {}
