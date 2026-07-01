package com.example.tsubuyaki.controller;

import com.example.tsubuyaki.domain.Post;
import com.example.tsubuyaki.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class PostCreationFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostRepository postRepository;

    @BeforeEach
    void setUp() {
        postRepository.deleteAll();
    }

    @Test
    @DisplayName("投稿登録_投稿者と内容が正しい_1件保存して一覧へリダイレクトする")
    void create_whenValid_savesOnePostAndRedirectsToList() throws Exception {
        long beforeCount = postRepository.count();

        mockMvc.perform(post("/posts/create")
                        .param("author", "alice")
                        .param("content", "今日の共有です"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts"));

        assertThat(postRepository.count()).isEqualTo(beforeCount + 1);
        Post savedPost = postRepository.findTop50ByOrderByCreatedAtDesc().get(0);
        assertThat(savedPost.getAuthor()).isEqualTo("alice");
        assertThat(savedPost.getBody()).isEqualTo("今日の共有です");
        assertThat(savedPost.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("投稿登録_投稿者未入力_バリデーションエラーで保存しない")
    void create_whenAuthorBlank_showsValidationErrorAndDoesNotSave() throws Exception {
        mockMvc.perform(post("/posts/create")
                        .param("author", "")
                        .param("content", "本文だけあります"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/form"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("postForm", "author"))
                .andExpect(content().string(containsString("投稿者名を入力してください")));

        assertThat(postRepository.count()).isZero();
    }

    @Test
    @DisplayName("投稿登録_内容未入力_バリデーションエラーで保存しない")
    void create_whenContentBlank_showsValidationErrorAndDoesNotSave() throws Exception {
        mockMvc.perform(post("/posts/create")
                        .param("author", "alice")
                        .param("content", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/form"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("postForm", "content"))
                .andExpect(content().string(containsString("本文を入力してください")));

        assertThat(postRepository.count()).isZero();
    }

    @Test
    @DisplayName("投稿登録_登録後に一覧表示_登録した投稿が新着順の先頭に表示される")
    void create_thenList_showsCreatedPostAtTop() throws Exception {
        postRepository.save(new Post("bob", "古い投稿", Instant.parse("2026-05-23T00:00:00Z")));

        mockMvc.perform(post("/posts/create")
                        .param("author", "alice")
                        .param("content", "今登録した内容"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts"));

        MvcResult result = mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        int newPostIndex = html.indexOf("今登録した内容");
        int oldPostIndex = html.indexOf("古い投稿");

        assertThat(newPostIndex).isGreaterThanOrEqualTo(0);
        assertThat(oldPostIndex).isGreaterThan(newPostIndex);
    }
}
