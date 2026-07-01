package com.example.tsubuyaki.controller;

import com.example.tsubuyaki.domain.Post;
import com.example.tsubuyaki.service.PostService;
import com.example.tsubuyaki.web.dto.PostForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping({ "/", "/posts", "/posts/" })
    public String list(Model model) {
        model.addAttribute("posts", postService.latest());
        return "posts/list";
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

        postService.create(postForm.getAuthor(), postForm.getContent());
        return "redirect:/posts";
    }

    @GetMapping("/posts/{id}")
    public String detail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Post> post = postService.findById(id);
        if (post.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "指定された投稿は見つかりませんでした");
            return "redirect:/posts";
        }

        model.addAttribute("post", post.get());
        return "posts/detail";
    }
}
