package com.santos.facebookapi;

import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostRepository repository;

    public PostController(PostRepository repository) {
        this.repository = repository;
    }

    // List all posts (most recent first)
    @GetMapping
    public List<Post> getAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    // Get a single post
    @GetMapping("/{id}")
    public ResponseEntity<Post> getOne(@PathVariable Long id) {
        Optional<Post> post = repository.findById(id);
        return post.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    // Create a post (manual validation for all relevant fields)
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Post post) {
        if (post == null) {
            return ResponseEntity.badRequest().body("Request body is required");
        }

        // Validate required fields: author and content
        if (post.getAuthor() == null || post.getAuthor().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("author is required and cannot be empty");
        }
        if (post.getContent() == null || post.getContent().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("content is required and cannot be empty");
        }

        // Validate imageUrl if provided (must be a valid URL or empty)
        if (post.getImageUrl() != null && !post.getImageUrl().trim().isEmpty()) {
            try {
                new URL(post.getImageUrl().trim());
            } catch (MalformedURLException e) {
                return ResponseEntity.badRequest().body("imageUrl must be a valid URL");
            }
            // normalize whitespace
            post.setImageUrl(post.getImageUrl().trim());
        } else {
            post.setImageUrl(null);
        }

        // Ensure client doesn't set the id
        post.setId(null);

        Post saved = repository.save(post);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();

        return ResponseEntity.created(location).body(saved);
    }

    // Update a post (partial update; manual validation on provided fields)
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Post payload) {
        return repository.findById(id).map(existing -> {
            if (payload == null) {
                return ResponseEntity.badRequest().body("Request body is required");
            }

            // If author is provided, it must not be empty
            if (payload.getAuthor() != null) {
                if (payload.getAuthor().trim().isEmpty()) {
                    return ResponseEntity.badRequest().body("author cannot be empty");
                }
                existing.setAuthor(payload.getAuthor().trim());
            }

            // If content is provided, it must not be empty
            if (payload.getContent() != null) {
                if (payload.getContent().trim().isEmpty()) {
                    return ResponseEntity.badRequest().body("content cannot be empty");
                }
                existing.setContent(payload.getContent().trim());
            }

            // If imageUrl is provided, validate it; empty -> null (clears image)
            if (payload.getImageUrl() != null) {
                String image = payload.getImageUrl().trim();
                if (image.isEmpty()) {
                    existing.setImageUrl(null);
                } else {
                    try {
                        new URL(image);
                    } catch (MalformedURLException e) {
                        return ResponseEntity.badRequest().body("imageUrl must be a valid URL");
                    }
                    existing.setImageUrl(image);
                }
            }

            // Save and return updated entity
            Post saved = repository.save(existing);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    // Delete a post
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return repository.findById(id)
                .map(p -> {
                    repository.deleteById(id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}