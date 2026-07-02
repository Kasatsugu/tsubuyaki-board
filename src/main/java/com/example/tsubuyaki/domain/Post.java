package com.example.tsubuyaki.domain;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "posts")
public class Post {

    public static final String DEFAULT_AVATAR_COLOR = "#888888";

    @Id
    @SequenceGenerator(name = "posts_seq_gen", sequenceName = "posts_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "posts_seq_gen")
    private Long id;

    @Column(name = "author", length = 30, nullable = false)
    @NotBlank(message = "投稿者名を入力してください")
    @Size(max = 30, message = "投稿者名は 30 文字以内で入力してください")
    private String author;

    @Column(name = "body", length = 280, nullable = false)
    @NotBlank(message = "本文を入力してください")
    @Size(max = 280, message = "本文は 280 文字以内で入力してください")
    private String body;

    @Column(name = "avatar_color", length = 7, nullable = false)
    @NotBlank(message = "アバター色を選択してください")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "アバター色は #RRGGBB 形式で入力してください")
    private String avatarColor = DEFAULT_AVATAR_COLOR;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Post parent;

    @OneToMany(mappedBy = "parent")
    private List<Post> replies = new ArrayList<>();

    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(name = "post_tags",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new LinkedHashSet<>();

    protected Post() {
        // JPA
    }

    public Post(String author, String body, LocalDateTime createdAt) {
        this(author, body, DEFAULT_AVATAR_COLOR, createdAt);
    }

    public Post(String author, String body, String avatarColor, LocalDateTime createdAt) {
        this.author = author;
        this.body = body;
        this.avatarColor = avatarColor == null || avatarColor.isBlank() ? DEFAULT_AVATAR_COLOR : avatarColor;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getAuthor() {
        return author;
    }

    public String getBody() {
        return body;
    }

    public String getAvatarColor() {
        return avatarColor == null || avatarColor.isBlank() ? DEFAULT_AVATAR_COLOR : avatarColor;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public Post getParent() {
        return parent;
    }

    public void setParent(Post parent) {
        this.parent = parent;
    }

    public List<Post> getReplies() {
        return List.copyOf(replies);
    }

    public Set<Tag> getTags() {
        return Set.copyOf(tags);
    }

    public void replaceTags(Set<Tag> tags) {
        this.tags.clear();
        this.tags.addAll(tags);
    }

    public void markDeleted(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Post other)) {
            return false;
        }
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
