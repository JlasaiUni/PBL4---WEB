package com.template.controller;

import com.template.dto.CreatePostRequest;
import com.template.dto.PostResponse;
import com.template.service.PostService;
import com.template.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final PostService postService;
    private final UserService userService;

    @GetMapping("/")
    public String home(Model model,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "6") int size) {
        Page<PostResponse> posts = postService.findPublished(PageRequest.of(page, size));
        model.addAttribute("posts", posts);
        return "index";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails,
                            Model model) {
        userService.findByUsernameWithPosts(userDetails.getUsername())
                .ifPresent(u -> model.addAttribute("user", u));
        return "user/dashboard";
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminPanel(Model model) {
        model.addAttribute("users", userService.findAll());
        return "admin/panel";
    }

    @GetMapping("/posts/new")
    @PreAuthorize("isAuthenticated()")
    public String newPostForm(Model model) {
        model.addAttribute("postRequest", new CreatePostRequest());
        return "user/post-form";
    }

    @PostMapping("/posts")
    @PreAuthorize("isAuthenticated()")
    public String createPost(@ModelAttribute CreatePostRequest request,
                             @AuthenticationPrincipal UserDetails userDetails) {
        postService.create(request, userDetails.getUsername());
        return "redirect:/dashboard";
    }

    @GetMapping("/posts/{id}")
    public String viewPost(@PathVariable Long id, Model model) {
        postService.findResponseById(id).ifPresent(p -> model.addAttribute("post", p));
        return "user/post-detail";
    }

    @GetMapping("/search")
    public String search(@RequestParam String q, Model model,
                         @RequestParam(defaultValue = "0") int page) {
        Page<PostResponse> results = postService.search(q, PageRequest.of(page, 10));
        model.addAttribute("results", results);
        model.addAttribute("query", q);
        return "user/search-results";
    }

    @GetMapping("/chat")
    public String chatPage() {
        return "user/chat";
    }
}
