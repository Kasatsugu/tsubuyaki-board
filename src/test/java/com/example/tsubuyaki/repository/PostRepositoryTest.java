package com.example.tsubuyaki.repository;

import com.example.tsubuyaki.domain.Post;
import com.example.tsubuyaki.domain.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("h2")
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Test
    @DisplayName("投稿一覧_51件以上の投稿_新着50件だけを返す")
    void findLatest_whenMoreThanFifty_returnsOnlyLatestFifty() {
        LocalDateTime base = LocalDateTime.of(2026, 5, 23, 0, 0);
        IntStream.rangeClosed(1, 51)
                .mapToObj(index -> new Post("user" + index, "body" + index, base.plusSeconds(index)))
                .forEach(postRepository::save);

        List<Post> posts = postRepository.findTop50ByDeletedAtIsNullAndParentIsNullOrderByCreatedAtDesc();

        assertThat(posts).hasSize(50);
        assertThat(posts.get(0).getBody()).isEqualTo("body51");
        assertThat(posts.get(49).getBody()).isEqualTo("body2");
        assertThat(posts).isSortedAccordingTo((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()));
    }

    @Test
    @DisplayName("投稿一覧_論理削除済みの投稿_新着50件に含めない")
    void findLatest_whenDeletedPostExists_excludesDeletedPost() {
        LocalDateTime base = LocalDateTime.of(2026, 5, 23, 0, 0);
        postRepository.save(new Post("alice", "表示する投稿", base.plusMinutes(1)));
        Post deletedPost = new Post("bob", "削除済みの投稿", base.plusMinutes(2));
        deletedPost.markDeleted(base.plusMinutes(3));
        postRepository.save(deletedPost);

        List<Post> posts = postRepository.findTop50ByDeletedAtIsNullAndParentIsNullOrderByCreatedAtDesc();

        assertThat(posts).extracting(Post::getBody).containsExactly("表示する投稿");
    }

    @Test
    @DisplayName("投稿一覧_未削除50件と論理削除済み1件_未削除50件だけを返す")
    void findLatest_whenFiftyAliveAndOneDeleted_returnsOnlyFiftyAlivePosts() {
        LocalDateTime base = LocalDateTime.of(2026, 5, 23, 0, 0);
        Post deletedPost = new Post("deleted", "削除済みの最新投稿", base.plusSeconds(100));
        deletedPost.markDeleted(base.plusSeconds(101));
        postRepository.save(deletedPost);
        IntStream.rangeClosed(1, 50)
                .mapToObj(index -> new Post("user" + index, "body" + index, base.plusSeconds(index)))
                .forEach(postRepository::save);

        List<Post> posts = postRepository.findTop50ByDeletedAtIsNullAndParentIsNullOrderByCreatedAtDesc();

        assertThat(posts).hasSize(50);
        assertThat(posts).extracting(Post::getBody).doesNotContain("削除済みの最新投稿");
        assertThat(posts.get(0).getBody()).isEqualTo("body50");
        assertThat(posts.get(49).getBody()).isEqualTo("body1");
    }

    @Test
    @DisplayName("投稿検索_本文にキーワードを含む投稿_新着順で最大50件を返す")
    void searchByBody_whenKeywordExists_returnsMatchedLatestFifty() {
        LocalDateTime base = LocalDateTime.of(2026, 5, 23, 0, 0);
        postRepository.save(new Post("alice", "検索対象ではない投稿", base.plusSeconds(100)));
        IntStream.rangeClosed(1, 51)
                .mapToObj(index -> new Post("user" + index, "朝会メモ " + index, base.plusSeconds(index)))
                .forEach(postRepository::save);

        List<Post> posts =
                postRepository.findTop50ByDeletedAtIsNullAndParentIsNullAndBodyContainingOrderByCreatedAtDesc("朝会");

        assertThat(posts).hasSize(50);
        assertThat(posts).allMatch(post -> post.getBody().contains("朝会"));
        assertThat(posts.get(0).getBody()).isEqualTo("朝会メモ 51");
        assertThat(posts.get(49).getBody()).isEqualTo("朝会メモ 2");
        assertThat(posts).isSortedAccordingTo((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()));
    }

    @Test
    @DisplayName("返信保存_親投稿指定_parent_idに親投稿IDを保存する")
    void saveReply_whenParentExists_persistsParentId() {
        LocalDateTime base = LocalDateTime.of(2026, 5, 23, 0, 0);
        Post parent = postRepository.save(new Post("alice", "親投稿", base));
        Post reply = new Post("bob", "返信です", base.plusMinutes(1));
        reply.setParent(parent);

        Post savedReply = postRepository.save(reply);
        postRepository.flush();

        Post reloaded = postRepository.findById(savedReply.getId()).orElseThrow();
        assertThat(reloaded.getParent()).isNotNull();
        assertThat(reloaded.getParent().getId()).isEqualTo(parent.getId());
    }

    @Test
    @DisplayName("投稿一覧_返信投稿あり_通常投稿だけを最大50件返す")
    void findLatest_whenRepliesExist_returnsOnlyRootPosts() {
        LocalDateTime base = LocalDateTime.of(2026, 5, 23, 0, 0);
        Post parent = postRepository.save(new Post("alice", "通常投稿", base));
        Post reply = new Post("bob", "一覧には出さない返信", base.plusMinutes(1));
        reply.setParent(parent);
        postRepository.save(reply);

        List<Post> posts = postRepository.findTop50ByDeletedAtIsNullAndParentIsNullOrderByCreatedAtDesc();

        assertThat(posts).extracting(Post::getBody).containsExactly("通常投稿");
    }

    @Test
    @DisplayName("返信一覧_親投稿ID指定_紐づく未削除返信だけを新着順で返す")
    void findReplies_whenParentIdExists_returnsActiveRepliesInLatestOrder() {
        LocalDateTime base = LocalDateTime.of(2026, 5, 23, 0, 0);
        Post parent = postRepository.save(new Post("alice", "親投稿", base));
        Post otherParent = postRepository.save(new Post("dave", "別の親投稿", base.plusMinutes(1)));
        Post oldReply = new Post("bob", "古い返信", base.plusMinutes(2));
        oldReply.setParent(parent);
        Post newReply = new Post("carol", "新しい返信", base.plusMinutes(3));
        newReply.setParent(parent);
        Post otherReply = new Post("erin", "別投稿への返信", base.plusMinutes(4));
        otherReply.setParent(otherParent);
        Post deletedReply = new Post("frank", "削除済み返信", base.plusMinutes(5));
        deletedReply.setParent(parent);
        deletedReply.markDeleted(base.plusMinutes(6));
        postRepository.save(oldReply);
        postRepository.save(newReply);
        postRepository.save(otherReply);
        postRepository.save(deletedReply);

        List<Post> replies = postRepository.findByParentIdAndDeletedAtIsNullAndParentDeletedAtIsNullOrderByCreatedAtDesc(
                parent.getId());

        assertThat(replies).extracting(Post::getBody).containsExactly("新しい返信", "古い返信");
    }

    @Test
    @DisplayName("タグ別一覧_指定タグあり_該当タグを持つ投稿だけを新着順で返す")
    void findByTag_whenTagExists_returnsOnlyTaggedPosts() {
        LocalDateTime base = LocalDateTime.of(2026, 5, 23, 0, 0);
        Tag spring = new Tag("Spring");
        Tag javaTag = new Tag("Java");
        Post springPost = new Post("alice", "Spring投稿 #Spring", base.plusMinutes(2));
        springPost.replaceTags(java.util.Set.of(spring));
        Post javaPost = new Post("bob", "Java投稿 #Java", base.plusMinutes(3));
        javaPost.replaceTags(java.util.Set.of(javaTag));
        Post olderSpringPost = new Post("carol", "古いSpring投稿 #Spring", base.plusMinutes(1));
        olderSpringPost.replaceTags(java.util.Set.of(spring));
        postRepository.save(springPost);
        postRepository.save(javaPost);
        postRepository.save(olderSpringPost);

        List<Post> posts = postRepository.findDistinctTop50ByDeletedAtIsNullAndTagsNameOrderByCreatedAtDesc("Spring");

        assertThat(posts).extracting(Post::getBody)
                .containsExactly("Spring投稿 #Spring", "古いSpring投稿 #Spring");
    }

    @Test
    @DisplayName("タグ別一覧_論理削除済み投稿あり_削除済み投稿を含めない")
    void findByTag_whenDeletedTaggedPostExists_excludesDeletedPost() {
        LocalDateTime base = LocalDateTime.of(2026, 5, 23, 0, 0);
        Tag spring = new Tag("Spring");
        Post activePost = new Post("alice", "表示する #Spring", base.plusMinutes(1));
        activePost.replaceTags(java.util.Set.of(spring));
        Post deletedPost = new Post("bob", "表示しない #Spring", base.plusMinutes(2));
        deletedPost.replaceTags(java.util.Set.of(spring));
        deletedPost.markDeleted(base.plusMinutes(3));
        postRepository.save(activePost);
        postRepository.save(deletedPost);

        List<Post> posts = postRepository.findDistinctTop50ByDeletedAtIsNullAndTagsNameOrderByCreatedAtDesc("Spring");

        assertThat(posts).extracting(Post::getBody).containsExactly("表示する #Spring");
    }

    @Test
    @DisplayName("タグ別一覧_存在しないタグ_空リストを返す")
    void findByTag_whenTagDoesNotExist_returnsEmptyList() {
        postRepository.save(new Post("alice", "タグなし投稿", LocalDateTime.of(2026, 5, 23, 10, 0)));

        List<Post> posts = postRepository.findDistinctTop50ByDeletedAtIsNullAndTagsNameOrderByCreatedAtDesc("Unknown");

        assertThat(posts).isEmpty();
    }
}
