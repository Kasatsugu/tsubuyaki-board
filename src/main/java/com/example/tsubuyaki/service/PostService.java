package com.example.tsubuyaki.service;

import com.example.tsubuyaki.domain.Post;
import com.example.tsubuyaki.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository repository;

    public PostService(PostRepository repository) {
        this.repository = repository;
    }

    public List<Post> latest() {
        return repository.findTop50ByOrderByCreatedAtDesc();
    }

    public List<Post> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return latest();
        }
        return repository.findTop50ByBodyContainingOrderByCreatedAtDesc(keyword);
    }

    public Optional<Post> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public Post create(String author, String content) {
        return create(author, content, Post.DEFAULT_AVATAR_COLOR);
    }

    @Transactional
    public Post create(String author, String content, String avatarColor) {
        String color = avatarColor == null || avatarColor.isBlank() ? Post.DEFAULT_AVATAR_COLOR : avatarColor;
        return repository.save(new Post(author, content, color, LocalDateTime.now()));
    }
}
