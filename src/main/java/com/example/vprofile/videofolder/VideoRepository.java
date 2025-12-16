
package com.example.vprofile.videofolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VideoRepository extends JpaRepository<Video, Long>, JpaSpecificationExecutor<Video> {
        Optional<Video> findByUserId(Long userId);

        Optional<Video> findByFileName(String fileName);

        Optional<Video> findByUrl(String url);

        Optional<Video> findTopByUserIdOrderByCreatedAtDesc(Long userId);

        Optional<Video> findTopByUserIdOrderByIdDesc(Long userId);

        @Override
        Optional<Video> findById(Long videoId);

        @Override
        List<Video> findAll();

        @Override
        Page<Video> findAll(Pageable pageable);

        Page<Video> findByJobId(String jobId, Pageable pageable);

        @Query("SELECT v.id FROM Video v WHERE v.userId = :userId")
        List<Long> findVideoIdsByUserId(@Param("userId") Long userId);

        @Query(value = "SELECT v.* FROM video v " +
                        "JOIN likes l ON l.video_id = v.id " +
                        "WHERE l.created_at >= :startOfWeek AND l.is_like = 1 " +
                        "GROUP BY l.video_id " +
                        "ORDER BY COUNT(l.id) DESC", nativeQuery = true)
        List<Video> findTrendingVideos(@Param("startOfWeek") LocalDateTime startOfWeek);

        @Query("SELECT DISTINCT u.id FROM User u " +
                        "LEFT JOIN Video v ON v.userId = u.id " +
                        "WHERE (:keySkills IS NULL OR LOWER(u.keySkills) LIKE %:keySkills%) AND " +
                        "(:experiences IS NULL OR u.experience IN :experiences) AND " +
                        "(:industries IS NULL OR u.industry IN :industries) AND " +
                        "(:cities IS NULL OR u.city IN :cities) AND " +
                        "(:jobId IS NULL OR v.jobId = :jobId) AND " + // Existing jobId filter
                        "(:college IS NULL OR LOWER(u.college) LIKE %:college%)") // New college filter
        List<Long> findUserIdsByFilters(
                        @Param("keySkills") String keySkills,
                        @Param("experiences") List<String> experiences,
                        @Param("industries") List<String> industries,
                        @Param("cities") List<String> cities,
                        @Param("jobId") String jobId,
                        @Param("college") String college); // New parameter for college

        @Query("SELECT COUNT(v) FROM Video v")
        long countAllUpload();

       List<Video> findByJobId(String jobId);

       long countByJobId(String jobid);

       Video findFirstByEmbeddingVectorIsNullAndTranscriptionIsNotNull();

       List<Video> findAllByUserId(Long userId);

}
