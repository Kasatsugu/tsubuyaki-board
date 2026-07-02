package com.example.tsubuyaki.service;

import com.example.tsubuyaki.domain.Post;
import com.example.tsubuyaki.domain.Tag;
import com.example.tsubuyaki.repository.PostRepository;
import com.example.tsubuyaki.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class PostService {

    private static final Pattern TAG_PATTERN = Pattern.compile("#([\\p{L}\\p{N}_-]{1,50})");

    private final PostRepository repository;
    private final TagRepository tagRepository;

    public PostService(PostRepository repository, TagRepository tagRepository) {
        this.repository = repository;
        this.tagRepository = tagRepository;
    }

    public List<Post> latest() {
        return repository.findTop50ByDeletedAtIsNullAndParentIsNullOrderByCreatedAtDesc();
    }

    public List<Post> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return latest();
        }
        return repository.findTop50ByDeletedAtIsNullAndParentIsNullAndBodyContainingOrderByCreatedAtDesc(keyword);
    }

    public Optional<Post> findById(Long id) {
        return repository.findById(id);
    }

    public List<Post> findByTag(String tagName) {
        return repository.findDistinctTop50ByDeletedAtIsNullAndTagsNameOrderByCreatedAtDesc(tagName);
    }

    public List<Post> repliesFor(Long parentId) {
        return repository.findByParentIdAndDeletedAtIsNullAndParentDeletedAtIsNullOrderByCreatedAtDesc(parentId);
    }

    @Transactional
    public Post create(String author, String content) {
        return create(author, content, Post.DEFAULT_AVATAR_COLOR);
    }

    @Transactional
    public Post create(String author, String content, String avatarColor) {
        String color = avatarColor == null || avatarColor.isBlank() ? Post.DEFAULT_AVATAR_COLOR : avatarColor;
        Post post = new Post(author, content, color, LocalDateTime.now());
        post.replaceTags(resolveTags(content));
        return repository.save(post);
    }

    @Transactional
    public Optional<Post> createReply(Long parentId, String author, String content, String avatarColor) {
        Optional<Post> parent = repository.findById(parentId).filter(post -> post.getDeletedAt() == null);
        if (parent.isEmpty()) {
            return Optional.empty();
        }
        String color = avatarColor == null || avatarColor.isBlank() ? Post.DEFAULT_AVATAR_COLOR : avatarColor;
        Post reply = new Post(author, content, color, LocalDateTime.now());
        reply.setParent(parent.get());
        reply.replaceTags(resolveTags(content));
        return Optional.of(repository.save(reply));
    }

    @Transactional
    public void delete(Long id) {
        repository.findById(id).ifPresent(post -> {
            post.markDeleted(LocalDateTime.now());
            repository.save(post);
        });
    }

    private Set<Tag> resolveTags(String content) {
        Set<Tag> tags = new LinkedHashSet<>();
        if (content == null || content.isBlank()) {
            return tags;
        }
        Matcher matcher = TAG_PATTERN.matcher(content);
        while (matcher.find()) {
            String name = matcher.group(1);
            Tag tag = tagRepository.findByName(name).orElseGet(() -> tagRepository.save(new Tag(name)));
            tags.add(tag);
        }
        return tags;
    }
}
