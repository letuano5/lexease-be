package com.lexease.stories;

import com.lexease.guardians.GuardianChildLink;
import com.lexease.guardians.GuardianChildLinkStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class StorySpecifications {
    private StorySpecifications() {
    }

    public static Specification<Story> search(
            String normalizedKeyword,
            List<UUID> genreIds,
            List<UUID> authorIds,
            boolean publishedOnly,
            UUID visibleForChildId
    ) {
        return (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();
            if (publishedOnly) {
                predicates.add(cb.equal(root.get("status"), StoryStatus.PUBLISHED));
            }
            if (normalizedKeyword != null && !normalizedKeyword.isBlank()) {
                predicates.add(cb.like(root.get("normalizedTitle"), "%" + normalizedKeyword + "%"));
            }
            if (!genreIds.isEmpty()) {
                Join<Story, Genre> genre = root.join("genres", JoinType.INNER);
                predicates.add(genre.get("id").in(genreIds));
            }
            if (!authorIds.isEmpty()) {
                Join<Story, Author> author = root.join("authors", JoinType.INNER);
                predicates.add(author.get("id").in(authorIds));
            }
            if (visibleForChildId != null) {
                predicates.add(cb.not(hasAcceptedActiveBlock(root, visibleForChildId, query, cb)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Predicate hasAcceptedActiveBlock(
            Root<Story> root,
            UUID childId,
            jakarta.persistence.criteria.CriteriaQuery<?> query,
            jakarta.persistence.criteria.CriteriaBuilder cb
    ) {
        Subquery<UUID> subquery = query.subquery(UUID.class);
        Root<StoryAccessBlock> block = subquery.from(StoryAccessBlock.class);
        Root<GuardianChildLink> link = subquery.from(GuardianChildLink.class);
        subquery.select(block.get("id"));
        subquery.where(
                cb.equal(block.get("story").get("id"), root.get("id")),
                cb.equal(block.get("child").get("id"), childId),
                cb.isTrue(block.get("active")),
                cb.equal(link.get("guardian").get("id"), block.get("blockedByGuardian").get("id")),
                cb.equal(link.get("child").get("id"), childId),
                cb.equal(link.get("status"), GuardianChildLinkStatus.ACCEPTED)
        );
        return cb.exists(subquery);
    }
}
