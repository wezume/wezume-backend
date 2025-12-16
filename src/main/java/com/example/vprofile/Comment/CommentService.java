package com.example.vprofile.Comment;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    // Add a new comment
    public Comment addComment(Long userId, Long videoId, String firstName, String comment) {
        Comment newComment = new Comment();
        newComment.setUserId(userId);
        newComment.setVideoId(videoId);
        newComment.setFirstName(firstName);
        newComment.setComment(comment);
        newComment.setCreatedDate(LocalDateTime.now());
        return commentRepository.save(newComment);
    }

    // Edit an existing comment
    public Comment editComment(Long commentId, Long userId, String newComment) {
        Comment existingComment = commentRepository.findByIdAndUserId(commentId, userId);
        if (existingComment != null) {
            existingComment.setComment(newComment);
            return commentRepository.save(existingComment);
        }
        return null; // Comment not found or user doesn't have permission
    }

    // Delete a comment
    public boolean deleteComment(Long commentId, Long userId) {
        Comment existingComment = commentRepository.findByIdAndUserId(commentId, userId);
        if (existingComment != null) {
            commentRepository.delete(existingComment);
            return true;
        }
        return false; // Comment not found or user doesn't have permission
    }

    public List<Comment> getCommentsByVideoId(Long videoId) {
        return commentRepository.findByVideoId(videoId);
    }

    // Get comments by userId
    public List<Comment> getCommentsByUserId(Long userId) {
        return commentRepository.findByUserId(userId);
    }
}
