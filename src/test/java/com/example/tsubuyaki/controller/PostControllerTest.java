package com.example.tsubuyaki.controller;

import com.example.tsubuyaki.domain.Post;
import com.example.tsubuyaki.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(PostController.class)
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostService postService;

    @Test
    @DisplayName("投稿一覧_DB空のとき_空メッセージを表示する")
    void list_whenNoPosts_showsEmptyMessage() throws Exception {
        given(postService.latest()).willReturn(Collections.emptyList());

        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/list"))
                .andExpect(model().attribute("posts", Collections.emptyList()))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("まだ投稿はありません")));
    }

    @Test
    @DisplayName("投稿一覧_新規投稿リンク_フォーム画面へ遷移でき一覧に投稿フォームを表示しない")
    void list_newPostLink_navigatesToFormWithoutRenderingCreateForm() throws Exception {
        given(postService.latest()).willReturn(Collections.emptyList());

        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/posts/new\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("action=\"/posts/create\""))));
    }

    @Test
    @DisplayName("投稿一覧_投稿あり_投稿者内容投稿日の順に表示する")
    void list_whenPostsExist_rendersAuthorBodyCreatedAtInOrder() throws Exception {
        given(postService.latest()).willReturn(List.of(
                new Post("alice", "今日は社内LT会です", LocalDateTime.of(2026, 5, 23, 10, 15))));

        MvcResult result = mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        int authorIndex = html.indexOf("alice");
        int bodyIndex = html.indexOf("今日は社内LT会です");
        int createdAtIndex = html.indexOf("2026-05-23 10:15");

        assertThat(authorIndex).isGreaterThanOrEqualTo(0);
        assertThat(bodyIndex).isGreaterThan(authorIndex);
        assertThat(createdAtIndex).isGreaterThan(bodyIndex);
    }

    @Test
    @DisplayName("投稿一覧_投稿内容リンク_詳細画面へのリンクを表示する")
    void list_postBodyLink_rendersDetailLink() throws Exception {
        Post post = new Post("alice", "詳細で読みたい投稿", LocalDateTime.of(2026, 5, 23, 10, 15));
        ReflectionTestUtils.setField(post, "id", 42L);
        given(postService.latest()).willReturn(List.of(post));

        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/posts/42\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("詳細で読みたい投稿")));
    }

    @Test
    @DisplayName("投稿詳細_存在するID_投稿詳細を表示する")
    void detail_existingId_rendersPostDetail() throws Exception {
        Post post = new Post("alice", "全文を表示します", LocalDateTime.of(2026, 5, 23, 10, 15, 30));
        ReflectionTestUtils.setField(post, "id", 42L);
        given(postService.findById(42L)).willReturn(Optional.of(post));

        mockMvc.perform(get("/posts/42"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/detail"))
                .andExpect(model().attribute("post", post))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("alice")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("全文を表示します")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("2026/05/23 10:15:30")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/posts\"")));
    }

    @Test
    @DisplayName("投稿詳細_存在しないID_一覧へリダイレクトしてエラーメッセージを渡す")
    void detail_missingId_redirectsToListWithErrorMessage() throws Exception {
        given(postService.findById(404L)).willReturn(Optional.empty());

        mockMvc.perform(get("/posts/404"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts"))
                .andExpect(flash().attribute(
                        "errorMessage",
                        "指定された投稿は見つかりませんでした"));
    }

    @Test
    @DisplayName("新規投稿フォーム_GET_/posts/new_空フォームをビューに渡す")
    void newForm_rendersEmptyPostForm() throws Exception {
        mockMvc.perform(get("/posts/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/form"))
                .andExpect(model().attributeExists("postForm"));
    }
}
