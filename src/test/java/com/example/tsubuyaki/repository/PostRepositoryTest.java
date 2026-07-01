package com.example.tsubuyaki.repository;

import com.example.tsubuyaki.domain.Post;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("h2")
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Test
    @DisplayName("投稿一覧_51件以上の投稿_新着50件だけを返す")
    void findLatest_whenMoreThanFifty_returnsOnlyLatestFifty() {
        LocalDateTime base = LocalDateTime.of(2026, 5, 23, 0, 0);
        IntStream.rangeClosed(1, 51)
                .mapToObj(index -> new Post("user" + index, "body" + index, base.plusSeconds(index)))
                .forEach(postRepository::save);

        List<Post> posts = postRepository.findTop50ByOrderByCreatedAtDesc();

        assertThat(posts).hasSize(50);
        assertThat(posts.get(0).getBody()).isEqualTo("body51");
        assertThat(posts.get(49).getBody()).isEqualTo("body2");
        assertThat(posts).isSortedAccordingTo((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()));
    }
}
