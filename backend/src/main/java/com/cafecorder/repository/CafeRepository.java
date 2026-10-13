package com.cafecorder.repository;

import com.cafecorder.model.Cafe;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

// Spring Data writes the implementation. Queries on Cafe also return StudyCafe rows, since a StudyCafe is a Cafe.
// Every query here takes the owner's id: there is no "find any visit" path for the logbook.
public interface CafeRepository extends JpaRepository<Cafe, Long> {

    List<Cafe> findByUserId(Long userId, Sort sort);

    List<Cafe> findByUserIdAndNeighbourhoodIgnoreCase(Long userId, String neighbourhood, Sort sort);

    // id AND owner: someone else's visit simply isn't found, so it's a 404 rather than a 403
    // (a 403 would confirm the id exists)
    Optional<Cafe> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    @Query("select distinct c.neighbourhood from Cafe c where c.user.id = :userId order by c.neighbourhood")
    List<String> findNeighbourhoods(@Param("userId") Long userId);

    // null when there are no visits
    @Query("select sum(c.price) from Cafe c where c.user.id = :userId")
    BigDecimal totalSpent(@Param("userId") Long userId);

    // unrated (0) visits are left out so they don't drag the average down; null when nothing is rated
    @Query("select avg(c.rating) from Cafe c where c.user.id = :userId and c.rating > 0")
    Double averageRating(@Param("userId") Long userId);

    // same cafe = same name + neighbourhood, ignoring case (like the old equals())
    @Query(value = """
            select count(*) from (
              select distinct lower(name), lower(neighbourhood) from cafe_visits where user_id = :userId
            ) t""", nativeQuery = true)
    long countUniqueCafes(@Param("userId") Long userId);

    // ---- public feed ----

    // N+1 problem: the feed shows each author's name, and user is LAZY, so without this
    // 30 visits = 1 query + 30 more for the users. The entity graph joins users into the same query.
    @EntityGraph(attributePaths = "user")
    List<Cafe> findBySharedTrue(Pageable pageable);

    // Native SQL because JPQL has no array_agg/FILTER. Groups visits into cafes (name + neighbourhood,
    // ignoring case), ranks by average rating then visit count, and picks the best-rated photo as the cover.
    @Query(value = """
            select min(c.name)          as "name",
                   min(c.neighbourhood) as "neighbourhood",
                   avg(c.rating)        as "averageRating",
                   count(*)             as "visits",
                   (array_agg(c.photo_url order by c.rating desc, c.visit_date desc)
                      filter (where c.photo_url is not null))[1] as "coverPhoto"
            from cafe_visits c
            where c.shared = true and c.rating > 0
            group by lower(c.name), lower(c.neighbourhood)
            order by avg(c.rating) desc, count(*) desc
            limit :limit""", nativeQuery = true)
    List<TopCafeView> findTopCafes(@Param("limit") int limit);

    // Spring Data fills this in from the column aliases above
    interface TopCafeView {
        String getName();
        String getNeighbourhood();
        BigDecimal getAverageRating();
        Long getVisits();
        String getCoverPhoto();
    }
}
