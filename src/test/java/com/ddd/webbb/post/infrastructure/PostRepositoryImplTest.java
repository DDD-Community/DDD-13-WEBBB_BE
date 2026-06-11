package com.ddd.webbb.post.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.ddd.webbb.category.domain.BoardCategory;
import com.ddd.webbb.global.config.JpaConfig;
import com.ddd.webbb.post.application.PostSearchCondition;
import com.ddd.webbb.post.domain.CommentTone;
import com.ddd.webbb.post.domain.Post;
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
                        new PostSearchCondition(List.of("PLANNING", "DESIGN"), List.of("YEAR_3")));

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
                        new PostSearchCondition(List.of("PLANNING"), List.of()));

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
}
