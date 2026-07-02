package com.example.tsubuyaki.controller;

import com.example.tsubuyaki.domain.Post;
import com.example.tsubuyaki.service.ClientHashGenerator;
import com.example.tsubuyaki.service.ClientIpResolver;
import com.example.tsubuyaki.service.LikeService;
import com.example.tsubuyaki.service.PostService;
import com.example.tsubuyaki.web.dto.PostForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class PostController {

    private final PostService postService;
    private final LikeService likeService;
    private final ClientHashGenerator clientHashGenerator;
    private final ClientIpResolver clientIpResolver;

    public PostController(PostService postService, LikeService likeService, ClientHashGenerator clientHashGenerator,
            ClientIpResolver clientIpResolver) {
        this.postService = postService;
        this.likeService = likeService;
        this.clientHashGenerator = clientHashGenerator;
        this.clientIpResolver = clientIpResolver;
    }

    @GetMapping({ "/", "/posts", "/posts/" })
    public String list(@RequestParam(name = "q", required = false) String query, Model model) {
        addListAttributes(query, model);
        return "posts/list";
    }

    @GetMapping("/tags/{name}")
    public String tagList(@PathVariable String name, Model model) {
        model.addAttribute("tagName", name);
        model.addAttribute("posts", postService.findByTag(name));
        return "posts/tag_list";
    }

    private void addListAttributes(String query, Model model) {
        boolean searchActive = hasSearchQuery(query);
        model.addAttribute("posts", searchActive ? postService.search(query) : postService.latest());
        model.addAttribute("searchQuery", query == null ? "" : query);
        model.addAttribute("searchActive", searchActive);
    }

    private boolean hasSearchQuery(String query) {
        return query != null && !query.isBlank();
    }

    @GetMapping("/posts/new")
    public String newForm(Model model) {
        model.addAttribute("postForm", new PostForm());
        return "posts/form";
    }

    @PostMapping("/posts/create")
    public String create(@Valid @ModelAttribute("postForm") PostForm postForm,
            BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "posts/form";
        }

        postService.create(postForm.getAuthor(), postForm.getContent(), postForm.getAvatarColor());
        return "redirect:/posts";
    }

    @GetMapping("/posts/{id}")
    public String detail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes,
            HttpServletRequest request) {
        Optional<Post> post = postService.findById(id);
        if (post.isEmpty() || post.get().getDeletedAt() != null) {
            redirectAttributes.addFlashAttribute("errorMessage", "指定された投稿は見つかりませんでした");
            return "redirect:/posts";
        }

        return renderDetail(post.get(), model, request);
    }

    @PostMapping("/posts/{id}/reply")
    public String createReply(@PathVariable Long id, @Valid @ModelAttribute("replyForm") PostForm replyForm,
            BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes,
            HttpServletRequest request) {
        Optional<Post> parent = postService.findById(id);
        if (parent.isEmpty() || parent.get().getDeletedAt() != null) {
            redirectAttributes.addFlashAttribute("errorMessage", "指定された投稿は見つかりませんでした");
            return "redirect:/posts";
        }
        if (bindingResult.hasErrors()) {
            return renderDetail(parent.get(), model, request);
        }

        Optional<Post> created = postService.createReply(
                id, replyForm.getAuthor(), replyForm.getContent(), replyForm.getAvatarColor());
        if (created.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "削除された投稿にはリプライできません");
            return "redirect:/posts";
        }
        return "redirect:/posts/" + id;
    }

    private String renderDetail(Post post, Model model, HttpServletRequest request) {
        String clientHash = clientHashGenerator.generate(
                clientIpResolver.resolve(request), request.getHeader("User-Agent"));
        model.addAttribute("post", post);
        model.addAttribute("replies", postService.repliesFor(post.getId()));
        if (!model.containsAttribute("replyForm")) {
            model.addAttribute("replyForm", new PostForm());
        }
        model.addAttribute("likeSummary", likeService.summary(post.getId(), clientHash));
        return "posts/detail";
    }

    @PostMapping("/posts/{id}/likes")
    public String toggleLike(@PathVariable Long id, HttpServletRequest request) {
        String clientHash = clientHashGenerator.generate(clientIpResolver.resolve(request), request.getHeader("User-Agent"));
        likeService.toggle(id, clientHash);
        return "redirect:/posts/" + id;
    }

    @PostMapping("/posts/{id}/delete")
    public String delete(@PathVariable Long id) {
        postService.delete(id);
        return "redirect:/posts";
    }
}
