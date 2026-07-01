package com.example.tsubuyaki.service;

import com.example.tsubuyaki.domain.Post;
import com.example.tsubuyaki.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostService postService;

    @Test
    @DisplayName("投稿一覧_最新投稿取得_Repositoryの新着50件取得結果を返す")
    void latest_returnsLatestFiftyFromRepository() {
        List<Post> latestPosts = List.of(
                new Post("alice", "新しい投稿", LocalDateTime.of(2026, 5, 23, 10, 0)));
        given(postRepository.findTop50ByOrderByCreatedAtDesc()).willReturn(latestPosts);

        List<Post> actual = postService.latest();

        assertThat(actual).isEqualTo(latestPosts);
        verify(postRepository).findTop50ByOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("投稿検索_キーワードあり_Repositoryの本文検索結果を返す")
    void search_whenKeywordExists_returnsMatchedPostsFromRepository() {
        List<Post> matchedPosts = List.of(
                new Post("alice", "朝会メモ", LocalDateTime.of(2026, 5, 23, 10, 0)));
        given(postRepository.findTop50ByBodyContainingOrderByCreatedAtDesc("朝会")).willReturn(matchedPosts);

        List<Post> actual = postService.search("朝会");

        assertThat(actual).isEqualTo(matchedPosts);
        verify(postRepository).findTop50ByBodyContainingOrderByCreatedAtDesc("朝会");
        verify(postRepository, never()).findTop50ByOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("投稿検索_キーワード空白_Repositoryの新着50件取得結果を返す")
    void search_whenKeywordBlank_returnsLatestPostsFromRepository() {
        List<Post> latestPosts = List.of(
                new Post("alice", "新しい投稿", LocalDateTime.of(2026, 5, 23, 10, 0)));
        given(postRepository.findTop50ByOrderByCreatedAtDesc()).willReturn(latestPosts);

        List<Post> actual = postService.search("  ");

        assertThat(actual).isEqualTo(latestPosts);
        verify(postRepository).findTop50ByOrderByCreatedAtDesc();
        verify(postRepository, never()).findTop50ByBodyContainingOrderByCreatedAtDesc(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("投稿詳細_ID指定_Repositoryの検索結果を返す")
    void findById_returnsRepositoryResult() {
        Post post = new Post("alice", "詳細本文", LocalDateTime.of(2026, 5, 23, 10, 0));
        given(postRepository.findById(42L)).willReturn(Optional.of(post));

        Optional<Post> actual = postService.findById(42L);

        assertThat(actual).contains(post);
        verify(postRepository).findById(42L);
    }
}
