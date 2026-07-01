package com.example.tsubuyaki.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class PostForm {

    public static final String DEFAULT_AVATAR_COLOR = "#888888";

    @NotBlank(message = "投稿者名を入力してください")
    @Size(max = 30, message = "投稿者名は 30 文字以内で入力してください")
    private String author;

    @NotBlank(message = "本文を入力してください")
    @Size(max = 280, message = "本文は 280 文字以内で入力してください")
    private String content;

    @NotBlank(message = "アバター色を選択してください")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "アバター色は #RRGGBB 形式で入力してください")
    private String avatarColor = DEFAULT_AVATAR_COLOR;

    public PostForm() {
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAvatarColor() {
        return avatarColor == null || avatarColor.isBlank() ? DEFAULT_AVATAR_COLOR : avatarColor;
    }

    public void setAvatarColor(String avatarColor) {
        this.avatarColor = avatarColor == null || avatarColor.isBlank() ? DEFAULT_AVATAR_COLOR : avatarColor;
    }
}
