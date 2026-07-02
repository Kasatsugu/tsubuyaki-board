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

import java.time.LocalDateTime;
import java.util.List;

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
                        .param("content", "今日の共有です")
                        .param("avatarColor", "#FF5733"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts"));

        assertThat(postRepository.count()).isEqualTo(beforeCount + 1);
        Post savedPost = postRepository.findTop50ByDeletedAtIsNullAndParentIsNullOrderByCreatedAtDesc().get(0);
        assertThat(savedPost.getAuthor()).isEqualTo("alice");
        assertThat(savedPost.getBody()).isEqualTo("今日の共有です");
        assertThat(savedPost.getAvatarColor()).isEqualTo("#FF5733");
        assertThat(savedPost.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("投稿登録_投稿者未入力_バリデーションエラーで保存しない")
    void create_whenAuthorBlank_showsValidationErrorAndDoesNotSave() throws Exception {
        mockMvc.perform(post("/posts/create")
                        .param("author", "")
                        .param("content", "本文だけあります")
                        .param("avatarColor", "#888888"))
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
                        .param("content", "")
                        .param("avatarColor", "#888888"))
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
        postRepository.save(new Post("bob", "古い投稿", LocalDateTime.of(2026, 5, 23, 0, 0)));

        mockMvc.perform(post("/posts/create")
                        .param("author", "alice")
                        .param("content", "今登録した内容")
                        .param("avatarColor", "#3366CC"))
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
        assertThat(html).contains("background-color: #3366CC");
    }

    @Test
    @DisplayName("投稿一覧_論理削除済みの投稿_モデルにも画面にも表示しない")
    void list_whenDeletedPostExists_excludesDeletedPostFromModelAndHtml() throws Exception {
        Post visiblePost = postRepository.save(
                new Post("alice", "表示される投稿", LocalDateTime.of(2026, 5, 23, 10, 0)));
        Post deletedPost = new Post("bob", "表示されない投稿", LocalDateTime.of(2026, 5, 23, 11, 0));
        deletedPost.markDeleted(LocalDateTime.of(2026, 5, 23, 12, 0));
        postRepository.save(deletedPost);

        MvcResult result = mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("posts", List.of(visiblePost)))
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertThat(html).contains("表示される投稿");
        assertThat(html).doesNotContain("表示されない投稿");
    }

    @Test
    @DisplayName("投稿削除_POST_deletedAtを保存して一覧へリダイレクトする")
    void delete_whenPostExists_setsDeletedAtAndRedirectsToList() throws Exception {
        Post post = postRepository.save(
                new Post("alice", "削除する投稿", LocalDateTime.of(2026, 5, 23, 10, 0)));

        mockMvc.perform(post("/posts/" + post.getId() + "/delete"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/posts"));

        Post deletedPost = postRepository.findById(post.getId()).orElseThrow();
        assertThat(deletedPost.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("投稿登録_アバター色未指定_デフォルトのグレーで保存する")
    void create_whenAvatarColorMissing_savesDefaultGray() throws Exception {
        mockMvc.perform(post("/posts/create")
                        .param("author", "alice")
                        .param("content", "色未指定の投稿"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts"));

        Post savedPost = postRepository.findTop50ByDeletedAtIsNullAndParentIsNullOrderByCreatedAtDesc().get(0);
        assertThat(savedPost.getAvatarColor()).isEqualTo("#888888");
    }

    @Test
    @DisplayName("投稿一覧_返信投稿あり_通常投稿だけをモデルと画面に表示する")
    void list_whenRepliesExist_excludesRepliesFromModelAndHtml() throws Exception {
        Post parent = postRepository.save(
                new Post("alice", "通常投稿", LocalDateTime.of(2026, 5, 23, 10, 0)));
        Post reply = new Post("bob", "一覧に出さない返信", LocalDateTime.of(2026, 5, 23, 11, 0));
        reply.setParent(parent);
        postRepository.save(reply);

        MvcResult result = mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        List<Post> posts = (List<Post>) result.getModelAndView().getModel().get("posts");
        String html = result.getResponse().getContentAsString();

        assertThat(posts).extracting(Post::getBody).containsExactly("通常投稿");
        assertThat(html).contains("通常投稿");
        assertThat(html).doesNotContain("一覧に出さない返信");
    }
}
