package com.ddd.webbb.post.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.ddd.webbb.category.domain.BoardCategory;
import com.ddd.webbb.global.config.JpaConfig;
import com.ddd.webbb.post.domain.CommentTone;
import com.ddd.webbb.post.domain.Post;
import com.ddd.webbb.post.domain.PostOrder;
import com.ddd.webbb.post.domain.PostSearchCondition;
import com.ddd.webbb.user.domain.User;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import({JpaConfig.class, PostRepositoryImpl.class})
@ActiveProfiles("test")
class PostRepositoryImplTest {

    @Autowired private jakarta.persistence.EntityManager entityManager;
    @Autowired private PostRepositoryImpl postRepositoryImpl;

    @Test
    void 직군과_경력_필터를_조합해_게시글을_조회한다() {
        BoardCategory category = persistCategory();
        Post planningYear3 =
                persistPost("planning3@test.com", "기획3", "PLANNING", "YEAR_3", category);
        Post designYear3 = persistPost("design3@test.com", "디자인3", "DESIGN", "YEAR_3", category);
        persistPost("developmentYear3@test.com", "개발3", "DEVELOPMENT", "YEAR_3", category);
        persistPost("planningYear1@test.com", "기획1", "PLANNING", "YEAR_1", category);
        flushAndClear();

        List<Post> posts =
                postRepositoryImpl.findByCursor(
                        null,
                        10,
                        new PostSearchCondition(
                                List.of("PLANNING", "DESIGN"),
                                List.of("YEAR_3"),
                                PostOrder.LATEST));

        assertThat(posts)
                .extracting(Post::getId)
                .containsExactly(designYear3.getId(), planningYear3.getId());
    }

    @Test
    void 커서는_필터링된_결과_안에서_적용된다() {
        BoardCategory category = persistCategory();
        Post first = persistPost("first@test.com", "첫번째", "PLANNING", "YEAR_3", category);
        Post second = persistPost("second@test.com", "두번째", "PLANNING", "YEAR_3", category);
        persistPost("third@test.com", "세번째", "DESIGN", "YEAR_3", category);
        flushAndClear();

        List<Post> posts =
                postRepositoryImpl.findByCursor(
                        second.getId(),
                        10,
                        new PostSearchCondition(List.of("PLANNING"), List.of(), PostOrder.LATEST));

        assertThat(posts).extracting(Post::getId).containsExactly(first.getId());
    }

    @Test
    void 필터가_없으면_삭제되지_않은_게시글을_최신순으로_조회한다() {
        BoardCategory category = persistCategory();
        Post first = persistPost("all1@test.com", "전체1", "PLANNING", "YEAR_1", category);
        Post second = persistPost("all2@test.com", "전체2", "DESIGN", "YEAR_3", category);
        flushAndClear();

        List<Post> posts = postRepositoryImpl.findByCursor(null, 10, PostSearchCondition.empty());

        assertThat(posts).extracting(Post::getId).containsExactly(second.getId(), first.getId());
    }

    @Test
    void 인기순으로_좋아요수와_id_기준_정렬한다() {
        BoardCategory category = persistCategory();
        Post low = persistPost("low@test.com", "낮음", "PLANNING", "YEAR_1", category);
        Post middle = persistPost("middle@test.com", "중간", "DESIGN", "YEAR_3", category);
        Post high = persistPost("high@test.com", "높음", "DEVELOPMENT", "YEAR_5", category);
        incrementLikes(low, 1);
        incrementLikes(middle, 3);
        incrementLikes(high, 3);
        flushAndClear();

        List<Post> posts =
                postRepositoryImpl.findByCursor(
                        null,
                        null,
                        10,
                        new PostSearchCondition(List.of(), List.of(), PostOrder.POPULAR));

        assertThat(posts)
                .extracting(Post::getId)
                .containsExactly(high.getId(), middle.getId(), low.getId());
    }

    @Test
    void 인기순_커서는_좋아요수와_id를_함께_사용한다() {
        BoardCategory category = persistCategory();
        Post highest = persistPost("highest@test.com", "최상", "PLANNING", "YEAR_1", category);
        Post middleOlder = persistPost("middle-old@test.com", "중간옛날", "DESIGN", "YEAR_3", category);
        Post middleNewer =
                persistPost("middle-new@test.com", "중간최신", "DEVELOPMENT", "YEAR_5", category);
        Post low = persistPost("low2@test.com", "낮음2", "MARKETING", "YEAR_2", category);
        incrementLikes(highest, 5);
        incrementLikes(middleOlder, 3);
        incrementLikes(middleNewer, 3);
        incrementLikes(low, 1);
        flushAndClear();

        List<Post> posts =
                postRepositoryImpl.findByCursor(
                        middleNewer.getId(),
                        3,
                        10,
                        new PostSearchCondition(List.of(), List.of(), PostOrder.POPULAR));

        assertThat(posts).extracting(Post::getId).containsExactly(middleOlder.getId(), low.getId());
    }

    private BoardCategory persistCategory() {
        BoardCategory category = BoardCategory.create("멘탈케어", "기본 카테고리", 0);
        entityManager.persist(category);
        return category;
    }

    private Post persistPost(
            String email,
            String nickname,
            String jobType,
            String careerLevel,
            BoardCategory category) {
        User user = User.createOAuthUser(email, nickname, jobType, careerLevel);
        entityManager.persist(user);
        Post post =
                Post.create(
                        user, category, nickname + " 제목", nickname + " 내용", CommentTone.COMFORT_ME);
        entityManager.persist(post);
        return post;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private void incrementLikes(Post post, int count) {
        for (int i = 0; i < count; i++) {
            post.incrementLikeCount();
        }
    }
}
