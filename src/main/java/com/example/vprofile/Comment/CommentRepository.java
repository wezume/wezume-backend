package com.example.vprofile.Comment;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByVideoId(Long videoId);
    List<Comment> findByUserId(Long userId);
    Comment findByIdAndUserId(Long commentId, Long userId);
}
