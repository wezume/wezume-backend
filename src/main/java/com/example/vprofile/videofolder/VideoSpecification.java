package com.example.vprofile.videofolder;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;

public class VideoSpecification {

    // Existing filterByUserIds method
    public static Specification<Video> filterByUserIds(List<Long> userIds) {
        return (root, query, builder) -> {
            if (userIds != null && !userIds.isEmpty()) {
                return root.get("userId").in(userIds);
            }
            return builder.conjunction(); // No filtering if no userIds
        };
    }

    // ✅ New hasCollege specification to filter by college
    public static Specification<Video> hasCollege(String college) {
        return (root, query, builder) -> {
            if (college != null && !college.isEmpty()) {
                return builder.equal(root.get("college"), college); // Compare "college" field
            }
            return builder.conjunction(); // No filtering if no college
        };
    }

    // Existing hasJobId method (for reference)
    public static Specification<Video> hasJobId(String jobId) {
        return (root, query, builder) -> {
            if (jobId != null && !jobId.isBlank()) {
                return builder.equal(root.get("jobId"), jobId); // Compare "jobId" field
            }
            return builder.conjunction(); // No filtering if no jobId
        };
    }
}
