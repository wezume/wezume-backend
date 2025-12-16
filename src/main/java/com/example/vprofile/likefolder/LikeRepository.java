package com.example.vprofile.likefolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.vprofile.videofolder.Video;

public interface LikeRepository extends JpaRepository<Like, Long> {
    boolean existsByUserIdAndVideoId(Long userId, Long videoId);  // Check if a user has liked the video
    Long countByUserId(Long userId);
    Long countByVideoIdAndIsLikeTrue(Long videoId);
    Optional<Like> findByUserIdAndVideoId(Long userId, Long videoId);
    
 @Query("SELECT v FROM Like l JOIN Video v ON l.videoId = v.id WHERE l.userId = :userId")
    List<Video> findLikedVideosByUserId(@Param("userId") Long userId); 
      

    @Query("SELECT v.userId, l.videoId, COUNT(l.id) AS likeCount " +
           "FROM Like l " +
           "JOIN Video v ON l.videoId = v.id " + // Assuming there's a Video table and a relation with Like
           "WHERE l.createdAt >= :startOfWeek AND l.isLike = true " +
           "GROUP BY l.videoId, v.userId " +
           "ORDER BY likeCount DESC")
List<Object[]> findTrendingVideos(@Param("startOfWeek") LocalDateTime startOfWeek);


}

