package com.example.tsubuyaki.service;

import com.example.tsubuyaki.domain.Like;
import com.example.tsubuyaki.domain.Post;
import com.example.tsubuyaki.repository.LikeRepository;
import com.example.tsubuyaki.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private LikeService likeService;

    @Test
    @DisplayName("いいね切替_未いいね_いいねを保存して件数が1増える")
    void toggle_whenNotLiked_savesLikeAndIncrementsCount() {
        Post post = new Post("alice", "本文", LocalDateTime.of(2026, 5, 23, 10, 0));
        given(postRepository.findById(42L)).willReturn(Optional.of(post));
        given(likeRepository.findByPostIdAndClientHash(42L, "abc12345")).willReturn(Optional.empty());
        given(likeRepository.countByPostId(42L)).willReturn(1L);

        LikeSummary actual = likeService.toggle(42L, "abc12345");

        assertThat(actual.count()).isEqualTo(1L);
        assertThat(actual.liked()).isTrue();
        verify(likeRepository).save(argThat(like ->
                like.getPost().equals(post) && like.getClientHash().equals("abc12345")));
        verify(likeRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("いいね切替_同じclientHashがいいね済み_いいねを削除して件数が1減る")
    void toggle_whenAlreadyLiked_deletesLikeAndDecrementsCount() {
        Post post = new Post("alice", "本文", LocalDateTime.of(2026, 5, 23, 10, 0));
        Like like = new Like(post, "abc12345");
        given(likeRepository.findByPostIdAndClientHash(42L, "abc12345")).willReturn(Optional.of(like));
        given(likeRepository.countByPostId(42L)).willReturn(0L);

        LikeSummary actual = likeService.toggle(42L, "abc12345");

        assertThat(actual.count()).isZero();
        assertThat(actual.liked()).isFalse();
        verify(likeRepository).delete(like);
        verify(likeRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("いいね切替_異なるclientHash_それぞれ独立してカウントする")
    void toggle_whenDifferentClientHash_countsIndependently() {
        Post post = new Post("alice", "本文", LocalDateTime.of(2026, 5, 23, 10, 0));
        given(postRepository.findById(42L)).willReturn(Optional.of(post));
        given(likeRepository.findByPostIdAndClientHash(42L, "client-b2")).willReturn(Optional.empty());
        given(likeRepository.countByPostId(42L)).willReturn(2L);

        LikeSummary actual = likeService.toggle(42L, "client-b2");

        assertThat(actual.count()).isEqualTo(2L);
        assertThat(actual.liked()).isTrue();
        verify(likeRepository).save(argThat(like -> like.getClientHash().equals("client-b2")));
    }
}
