package com.example.tsubuyaki.service;

import com.example.tsubuyaki.domain.Post;
import com.example.tsubuyaki.domain.Tag;
import com.example.tsubuyaki.repository.PostRepository;
import com.example.tsubuyaki.repository.TagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("h2")
@Import(PostService.class)
class PostServiceJpaTest {

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TagRepository tagRepository;

    @Test
    @DisplayName("タグ自動解析_投稿保存_Tagテーブルに保存して投稿と紐付ける")
    void create_whenBodyHasTags_persistsTagsAndLinksPost() {
        postService.create("alice", "こんにちは #Greeting #Spring");

        List<Tag> tags = tagRepository.findAll();
        List<Post> posts = postRepository.findAll();

        assertThat(tags).extracting(Tag::getName).containsExactlyInAnyOrder("Greeting", "Spring");
        assertThat(posts).hasSize(1);
        assertThat(posts.get(0).getTags()).extracting(Tag::getName)
                .containsExactlyInAnyOrder("Greeting", "Spring");
    }

    @Test
    @DisplayName("返信タグ自動解析_返信保存_Tagテーブルに保存して返信投稿と紐付ける")
    void createReply_whenBodyHasTag_persistsTagAndLinksReply() {
        Post parent = postService.create("alice", "親投稿");

        Post reply = postService.createReply(parent.getId(), "bob", "返信です #ReplyTag", "#3366CC")
                .orElseThrow();

        List<Tag> tags = tagRepository.findAll();
        Post savedReply = postRepository.findById(reply.getId()).orElseThrow();

        assertThat(tags).extracting(Tag::getName).contains("ReplyTag");
        assertThat(savedReply.getParent().getId()).isEqualTo(parent.getId());
        assertThat(savedReply.getTags()).extracting(Tag::getName).containsExactly("ReplyTag");
    }
}
