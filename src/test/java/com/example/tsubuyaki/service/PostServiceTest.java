package com.example.tsubuyaki.service;

import com.example.tsubuyaki.domain.Post;
import com.example.tsubuyaki.domain.Tag;
import com.example.tsubuyaki.repository.PostRepository;
import com.example.tsubuyaki.repository.TagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private PostService postService;

    @Test
    @DisplayName("投稿一覧_最新投稿取得_Repositoryの新着50件取得結果を返す")
    void latest_returnsLatestFiftyFromRepository() {
        List<Post> latestPosts = List.of(
                new Post("alice", "新しい投稿", LocalDateTime.of(2026, 5, 23, 10, 0)));
        given(postRepository.findTop50ByDeletedAtIsNullAndParentIsNullOrderByCreatedAtDesc()).willReturn(latestPosts);

        List<Post> actual = postService.latest();

        assertThat(actual).isEqualTo(latestPosts);
        verify(postRepository).findTop50ByDeletedAtIsNullAndParentIsNullOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("投稿検索_キーワードあり_Repositoryの本文検索結果を返す")
    void search_whenKeywordExists_returnsMatchedPostsFromRepository() {
        List<Post> matchedPosts = List.of(
                new Post("alice", "朝会メモ", LocalDateTime.of(2026, 5, 23, 10, 0)));
        given(postRepository.findTop50ByDeletedAtIsNullAndParentIsNullAndBodyContainingOrderByCreatedAtDesc("朝会"))
                .willReturn(matchedPosts);

        List<Post> actual = postService.search("朝会");

        assertThat(actual).isEqualTo(matchedPosts);
        verify(postRepository).findTop50ByDeletedAtIsNullAndParentIsNullAndBodyContainingOrderByCreatedAtDesc("朝会");
        verify(postRepository, never()).findTop50ByDeletedAtIsNullAndParentIsNullOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("投稿検索_キーワード空白_Repositoryの新着50件取得結果を返す")
    void search_whenKeywordBlank_returnsLatestPostsFromRepository() {
        List<Post> latestPosts = List.of(
                new Post("alice", "新しい投稿", LocalDateTime.of(2026, 5, 23, 10, 0)));
        given(postRepository.findTop50ByDeletedAtIsNullAndParentIsNullOrderByCreatedAtDesc()).willReturn(latestPosts);

        List<Post> actual = postService.search("  ");

        assertThat(actual).isEqualTo(latestPosts);
        verify(postRepository).findTop50ByDeletedAtIsNullAndParentIsNullOrderByCreatedAtDesc();
        verify(postRepository, never())
                .findTop50ByDeletedAtIsNullAndParentIsNullAndBodyContainingOrderByCreatedAtDesc(any());
    }

    @Test
    @DisplayName("投稿詳細_ID指定_Repositoryの検索結果を返す")
    void findById_returnsRepositoryResult() {
        Post post = new Post("alice", "詳細本文", LocalDateTime.of(2026, 5, 23, 10, 0));
        given(postRepository.findById(42L)).willReturn(Optional.of(post));

        Optional<Post> actual = postService.findById(42L);

        assertThat(actual).contains(post);
        verify(postRepository).findById(42L);
    }

    @Test
    @DisplayName("タグ自動解析_本文にタグあり_Tagを保存して投稿に紐付ける")
    void create_whenBodyHasTags_savesTagsAndLinksToPost() {
        given(tagRepository.findByName("Greeting")).willReturn(Optional.empty());
        given(tagRepository.findByName("Spring")).willReturn(Optional.empty());
        given(tagRepository.save(any(Tag.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(postRepository.save(any(Post.class))).willAnswer(invocation -> invocation.getArgument(0));

        Post created = postService.create("alice", "こんにちは #Greeting #Spring");

        assertThat(created.getTags()).extracting(Tag::getName).containsExactlyInAnyOrder("Greeting", "Spring");
        ArgumentCaptor<Tag> tagCaptor = ArgumentCaptor.forClass(Tag.class);
        verify(tagRepository, org.mockito.Mockito.times(2)).save(tagCaptor.capture());
        assertThat(tagCaptor.getAllValues()).extracting(Tag::getName).containsExactlyInAnyOrder("Greeting", "Spring");
        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(postCaptor.capture());
        assertThat(postCaptor.getValue().getTags()).extracting(Tag::getName)
                .containsExactlyInAnyOrder("Greeting", "Spring");
    }

    @Test
    @DisplayName("返信作成_親投稿あり_parentを設定して保存する")
    void createReply_whenParentExists_savesReplyWithParent() {
        Post parent = new Post("alice", "親投稿", LocalDateTime.of(2026, 5, 23, 10, 0));
        given(postRepository.findById(1L)).willReturn(Optional.of(parent));
        given(postRepository.save(any(Post.class))).willAnswer(invocation -> invocation.getArgument(0));

        Optional<Post> created = postService.createReply(1L, "bob", "返信本文", "#3366CC");

        assertThat(created).isPresent();
        assertThat(created.get().getParent()).isEqualTo(parent);
        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(postCaptor.capture());
        assertThat(postCaptor.getValue().getParent()).isEqualTo(parent);
        assertThat(postCaptor.getValue().getBody()).isEqualTo("返信本文");
    }

    @Test
    @DisplayName("返信作成_親投稿が論理削除済み_保存しない")
    void createReply_whenParentDeleted_doesNotSave() {
        Post parent = new Post("alice", "削除済み親投稿", LocalDateTime.of(2026, 5, 23, 10, 0));
        parent.markDeleted(LocalDateTime.of(2026, 5, 23, 11, 0));
        given(postRepository.findById(1L)).willReturn(Optional.of(parent));

        Optional<Post> created = postService.createReply(1L, "bob", "返信本文", "#3366CC");

        assertThat(created).isEmpty();
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("返信一覧_親投稿ID指定_Repositoryの返信一覧を返す")
    void repliesFor_returnsRepositoryResult() {
        List<Post> replies = List.of(new Post("bob", "返信", LocalDateTime.of(2026, 5, 23, 10, 1)));
        given(postRepository.findByParentIdAndDeletedAtIsNullAndParentDeletedAtIsNullOrderByCreatedAtDesc(1L))
                .willReturn(replies);

        List<Post> actual = postService.repliesFor(1L);

        assertThat(actual).isEqualTo(replies);
        verify(postRepository).findByParentIdAndDeletedAtIsNullAndParentDeletedAtIsNullOrderByCreatedAtDesc(1L);
    }

    @Test
    @DisplayName("タグ別一覧_タグ名指定_Repositoryのタグ別投稿を返す")
    void findByTag_returnsRepositoryResult() {
        List<Post> posts = List.of(new Post("alice", "#Spring", LocalDateTime.of(2026, 5, 23, 10, 0)));
        given(postRepository.findDistinctTop50ByDeletedAtIsNullAndTagsNameOrderByCreatedAtDesc("Spring"))
                .willReturn(posts);

        List<Post> actual = postService.findByTag("Spring");

        assertThat(actual).isEqualTo(posts);
        verify(postRepository).findDistinctTop50ByDeletedAtIsNullAndTagsNameOrderByCreatedAtDesc("Spring");
    }

    @Test
    @DisplayName("投稿削除_存在するID_deletedAtに現在時刻をセットして保存する")
    void delete_whenPostExists_setsDeletedAtAndSaves() {
        Post post = new Post("alice", "削除対象", LocalDateTime.of(2026, 5, 23, 10, 0));
        given(postRepository.findById(42L)).willReturn(Optional.of(post));

        postService.delete(42L);

        assertThat(post.getDeletedAt()).isNotNull();
        verify(postRepository).save(post);
    }
}
