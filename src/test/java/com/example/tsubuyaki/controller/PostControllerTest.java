package com.example.tsubuyaki.controller;

import com.example.tsubuyaki.domain.Post;
import com.example.tsubuyaki.service.ClientHashGenerator;
import com.example.tsubuyaki.service.ClientIpResolver;
import com.example.tsubuyaki.service.LikeService;
import com.example.tsubuyaki.service.LikeSummary;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @MockitoBean
    private LikeService likeService;

    @MockitoBean
    private ClientHashGenerator clientHashGenerator;

    @MockitoBean
    private ClientIpResolver clientIpResolver;

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
    @DisplayName("投稿一覧_登録フォーム_投稿者必須とカラーピッカーを表示する")
    void list_rendersCreateFormWithRequiredAuthorAndColorPicker() throws Exception {
        given(postService.latest()).willReturn(Collections.emptyList());

        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("postForm"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/posts/create\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"author\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("required")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("type=\"color\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"#888888\"")));
    }

    @Test
    @DisplayName("投稿一覧_投稿あり_投稿者内容投稿日の順に表示する")
    void list_whenPostsExist_rendersAuthorBodyCreatedAtInOrder() throws Exception {
        given(postService.latest()).willReturn(List.of(
                new Post("alice", "今日は社内LT会です", "#FF5733", LocalDateTime.of(2026, 5, 23, 10, 15))));

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
        assertThat(html).contains("background-color: #FF5733");
    }

    @Test
    @DisplayName("投稿一覧_検索フォーム_GETメソッドでキーワード入力欄を表示する")
    void list_rendersSearchForm() throws Exception {
        given(postService.latest()).willReturn(Collections.emptyList());

        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("method=\"get\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"q\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("検索")));
    }

    @Test
    @DisplayName("投稿一覧_キーワードあり_Serviceで検索して検索窓にキーワードを保持する")
    void list_whenKeywordExists_searchesAndKeepsKeyword() throws Exception {
        Post newerPost = new Post("alice", "朝会メモの新しい投稿", LocalDateTime.of(2026, 5, 23, 11, 0));
        Post olderPost = new Post("bob", "朝会メモの古い投稿", LocalDateTime.of(2026, 5, 23, 10, 0));
        given(postService.search("朝会")).willReturn(List.of(newerPost, olderPost));

        MvcResult result = mockMvc.perform(get("/posts").param("q", "朝会"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/list"))
                .andExpect(model().attribute("posts", List.of(newerPost, olderPost)))
                .andExpect(model().attribute("searchQuery", "朝会"))
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertThat(html).contains("value=\"朝会\"");
        assertThat(html.indexOf("朝会メモの新しい投稿")).isLessThan(html.indexOf("朝会メモの古い投稿"));
        verify(postService).search("朝会");
        verify(postService, never()).latest();
    }

    @Test
    @DisplayName("投稿一覧_存在しないキーワード_検索結果なしメッセージを表示する")
    void list_whenKeywordDoesNotExist_showsNoSearchResultsMessage() throws Exception {
        given(postService.search("存在しない")).willReturn(Collections.emptyList());

        mockMvc.perform(get("/posts").param("q", "存在しない"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("posts", Collections.emptyList()))
                .andExpect(model().attribute("searchQuery", "存在しない"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("検索結果が見つかりませんでした")));
    }

    @Test
    @DisplayName("投稿一覧_空キーワード_全投稿の新着50件を表示する")
    void list_whenKeywordBlank_returnsLatestPosts() throws Exception {
        List<Post> latestPosts = List.of(
                new Post("alice", "全件表示対象", LocalDateTime.of(2026, 5, 23, 10, 15)));
        given(postService.latest()).willReturn(latestPosts);

        mockMvc.perform(get("/posts").param("q", ""))
                .andExpect(status().isOk())
                .andExpect(model().attribute("posts", latestPosts))
                .andExpect(model().attribute("searchQuery", ""));

        verify(postService).latest();
        verify(postService, never()).search(org.mockito.ArgumentMatchers.any());
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
    @DisplayName("投稿一覧_投稿カード_カード全体が詳細リンクになる")
    void list_postCard_rendersWholeCardAsDetailLink() throws Exception {
        Post post = new Post("alice", "カード全体で開きたい投稿", LocalDateTime.of(2026, 5, 23, 10, 15));
        ReflectionTestUtils.setField(post, "id", 42L);
        given(postService.latest()).willReturn(List.of(post));

        MvcResult result = mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertThat(html).contains("<a class=\"post-card\" href=\"/posts/42\">");
        assertThat(html.indexOf("<a class=\"post-card\" href=\"/posts/42\">"))
                .isLessThan(html.indexOf("<article class=\"post\">"));
    }

    @Test
    @DisplayName("投稿詳細_存在するID_投稿詳細を表示する")
    void detail_existingId_rendersPostDetail() throws Exception {
        Post post = new Post("alice", "全文を表示します", LocalDateTime.of(2026, 5, 23, 10, 15, 30));
        ReflectionTestUtils.setField(post, "id", 42L);
        given(postService.findById(42L)).willReturn(Optional.of(post));
        given(clientHashGenerator.generate("203.0.113.10", "Agent A")).willReturn("hash0001");
        given(clientIpResolver.resolve(org.mockito.ArgumentMatchers.any())).willReturn("203.0.113.10");
        given(likeService.summary(42L, "hash0001")).willReturn(new LikeSummary(7L, true));

        mockMvc.perform(get("/posts/42")
                        .with(request -> {
                            request.setRemoteAddr("203.0.113.10");
                            return request;
                        })
                        .header("User-Agent", "Agent A"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/detail"))
                .andExpect(model().attribute("post", post))
                .andExpect(model().attribute("likeSummary", new LikeSummary(7L, true)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("alice")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("全文を表示します")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("background-color: #888888")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("2026/05/23 10:15:30")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("いいね済み")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("7")))
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
    @DisplayName("いいね切替_POST_いいねを切り替えて詳細画面へリダイレクトする")
    void toggleLike_postsToServiceAndRedirectsToDetail() throws Exception {
        given(clientHashGenerator.generate("203.0.113.10", "Agent A")).willReturn("hash0001");
        given(clientIpResolver.resolve(org.mockito.ArgumentMatchers.any())).willReturn("203.0.113.10");
        given(likeService.toggle(42L, "hash0001")).willReturn(new LikeSummary(1L, true));

        mockMvc.perform(post("/posts/42/likes")
                        .with(request -> {
                            request.setRemoteAddr("203.0.113.10");
                            return request;
                        })
                        .header("User-Agent", "Agent A"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts/42"));

        verify(likeService).toggle(42L, "hash0001");
    }

    @Test
    @DisplayName("いいね切替_POST_XForwardedForあり_転送元IPでclientHashを生成する")
    void toggleLike_whenXForwardedForExists_usesForwardedClientIp() throws Exception {
        given(clientIpResolver.resolve(org.mockito.ArgumentMatchers.any())).willReturn("198.51.100.50");
        given(clientHashGenerator.generate("198.51.100.50", "Agent B")).willReturn("hash0002");
        given(likeService.toggle(42L, "hash0002")).willReturn(new LikeSummary(1L, true));

        mockMvc.perform(post("/posts/42/likes")
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        })
                        .header("X-Forwarded-For", "198.51.100.50, 10.0.0.2")
                        .header("User-Agent", "Agent B"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts/42"));

        verify(clientHashGenerator).generate("198.51.100.50", "Agent B");
        verify(likeService).toggle(42L, "hash0002");
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
