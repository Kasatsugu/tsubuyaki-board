package com.example.tsubuyaki.service;

import com.example.tsubuyaki.domain.Like;
import com.example.tsubuyaki.domain.Post;
import com.example.tsubuyaki.repository.LikeRepository;
import com.example.tsubuyaki.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LikeService {

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;

    public LikeService(LikeRepository likeRepository, PostRepository postRepository) {
        this.likeRepository = likeRepository;
        this.postRepository = postRepository;
    }

    public LikeSummary summary(Long postId, String clientHash) {
        return new LikeSummary(
                likeRepository.countByPostId(postId),
                likeRepository.existsByPostIdAndClientHash(postId, clientHash));
    }

    @Transactional
    public LikeSummary toggle(Long postId, String clientHash) {
        return likeRepository.findByPostIdAndClientHash(postId, clientHash)
                .map(like -> unlike(postId, like))
                .orElseGet(() -> like(postId, clientHash));
    }

    private LikeSummary like(Long postId, String clientHash) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("投稿が見つかりません: " + postId));
        likeRepository.save(new Like(post, clientHash));
        return new LikeSummary(likeRepository.countByPostId(postId), true);
    }

    private LikeSummary unlike(Long postId, Like like) {
        likeRepository.delete(like);
        return new LikeSummary(likeRepository.countByPostId(postId), false);
    }
}
