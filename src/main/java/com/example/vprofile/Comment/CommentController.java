package com.example.vprofile.Comment;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    // Add a comment
    @PostMapping("/add")
    public ResponseEntity<Comment> addComment(@RequestParam Long userId, 
                                              @RequestParam Long videoId, 
                                              @RequestParam String firstName, 
                                              @RequestParam String comment) {
        Comment newComment = commentService.addComment(userId, videoId, firstName, comment);
        return ResponseEntity.ok(newComment);
    }

    // Edit a comment
    @PutMapping("/edit/{commentId}")
    public ResponseEntity<Comment> editComment(@PathVariable Long commentId, 
                                               @RequestParam Long userId, 
                                               @RequestParam String newComment) {
        Comment updatedComment = commentService.editComment(commentId, userId, newComment);
        if (updatedComment != null) {
            return ResponseEntity.ok(updatedComment);
        }
        return ResponseEntity.notFound().build();
    }

    // Delete a comment
    @DeleteMapping("/delete/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId, 
                                              @RequestParam Long userId) {
        boolean isDeleted = commentService.deleteComment(commentId, userId);
        if (isDeleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/videoId")
    public ResponseEntity<List<Comment>> getCommentsByVideoId(@RequestParam Long videoId) {
        List<Comment> comments = commentService.getCommentsByVideoId(videoId);
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/userId")
    public ResponseEntity<List<Comment>> getCommentsByUserId(@RequestParam Long userId) {
        List<Comment> comments = commentService.getCommentsByUserId(userId);
        return ResponseEntity.ok(comments);
    }
}