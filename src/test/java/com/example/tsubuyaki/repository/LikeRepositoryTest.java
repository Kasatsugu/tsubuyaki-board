package com.example.tsubuyaki.repository;

import com.example.tsubuyaki.domain.Like;
import com.example.tsubuyaki.domain.Post;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("h2")
class LikeRepositoryTest {

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private PostRepository postRepository;

    @Test
    @DisplayName("いいね検索_postIdとclientHash一致_対象いいねを取得する")
    void findByPostIdAndClientHash_whenExists_returnsLike() {
        Post post = postRepository.save(new Post(
                "alice", "本文", LocalDateTime.of(2026, 5, 23, 10, 0)));
        likeRepository.save(new Like(post, "abc12345"));

        assertThat(likeRepository.findByPostIdAndClientHash(post.getId(), "abc12345")).isPresent();
        assertThat(likeRepository.countByPostId(post.getId())).isEqualTo(1L);
        assertThat(likeRepository.existsByPostIdAndClientHash(post.getId(), "abc12345")).isTrue();
    }
}
