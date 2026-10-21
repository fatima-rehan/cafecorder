package com.cafecorder.repository;

import com.cafecorder.model.Cafe;
import com.cafecorder.model.CafeKind;
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

    // "Containing" on a collection = visits tagged with that kind
    List<Cafe> findByUserIdAndKindsContaining(Long userId, CafeKind kind, Sort sort);

    List<Cafe> findByUserIdAndNeighbourhoodIgnoreCaseAndKindsContaining(Long userId, String neighbourhood,
                                                                        CafeKind kind, Sort sort);

    // id AND owner: someone else's visit simply isn't found, so it's a 404 rather than a 403
    // (a 403 would confirm the id exists)
    Optional<Cafe> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    @Query("select distinct c.neighbourhood from Cafe c where c.user.id = :userId order by c.neighbourhood")
    List<String> findNeighbourhoods(@Param("userId") Long userId);

    // null when there are no visits
    @Query("select sum(c.price) from Cafe c where c.user.id = :userId")
    BigDecimal totalSpent(@Param("userId") Long userId);

    // what was spent on extras (pastries etc.); null when there are none
    @Query("select sum(e.price) from Cafe c join c.extras e where c.user.id = :userId")
    BigDecimal extrasSpent(@Param("userId") Long userId);

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

    @EntityGraph(attributePaths = "user")
    List<Cafe> findBySharedTrueAndKindsContaining(CafeKind kind, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    List<Cafe> findBySharedTrueAndCityIgnoreCase(String city, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    List<Cafe> findBySharedTrueAndCityIgnoreCaseAndKindsContaining(String city, CafeKind kind, Pageable pageable);

    // Cities that have shared visits, for the Explore filter. "dubai" and "Dubai" count once,
    // shown with a capital if anyone typed it that way.
    @Query(value = """
            select (array_agg(trim(city) order by left(trim(city), 1) = upper(left(trim(city), 1)) desc, trim(city)))[1]
            from cafe_visits
            where shared = true and city is not null and trim(city) <> ''
            group by lower(trim(city))
            order by lower(trim(city))""", nativeQuery = true)
    List<String> findSharedCities();

    // For the two queries below: kind is a CafeKind name and city a city name, either null for all.
    // A cafe counts as a kind if any of its shared visits was tagged with it, and all of its visits
    // still count toward the rating. The casts tell Postgres the parameter types when they're null.

    // Native SQL because JPQL has no array_agg/FILTER. Groups visits into cafes (name + neighbourhood,
    // ignoring case), ranks by average rating then visit count, and picks the best-rated photo as the cover.
    @Query(value = """
            select min(c.name)          as "name",
                   min(c.neighbourhood) as "neighbourhood",
                   min(c.city)          as "city",
                   avg(c.rating)        as "averageRating",
                   count(*)             as "visits",
                   (array_agg(c.photo_url order by c.rating desc, c.visit_date desc)
                      filter (where c.photo_url is not null))[1] as "coverPhoto"
            from cafe_visits c
            where c.shared = true and c.rating > 0
              and (cast(:city as varchar) is null or lower(trim(c.city)) = lower(trim(cast(:city as varchar))))
            group by lower(c.name), lower(c.neighbourhood)
            having cast(:kind as varchar) is null
                or bool_or(exists (select 1 from visit_kinds k where k.visit_id = c.id and k.kind = cast(:kind as varchar)))
            order by avg(c.rating) desc, count(*) desc
            limit :limit""", nativeQuery = true)
    List<TopCafeView> findTopCafes(@Param("kind") String kind, @Param("city") String city, @Param("limit") int limit);

    // Spring Data fills this in from the column aliases above
    interface TopCafeView {
        String getName();
        String getNeighbourhood();
        String getCity();
        BigDecimal getAverageRating();
        Long getVisits();
        String getCoverPhoto();
    }

    // Map pins: shared visits that have coordinates, one row per cafe (name + address, ignoring case)
    @Query(value = """
            select min(c.name)                               as "name",
                   min(c.address)                            as "address",
                   min(c.city)                               as "city",
                   avg(c.latitude)                           as "latitude",
                   avg(c.longitude)                          as "longitude",
                   count(*)                                  as "visits",
                   avg(c.rating) filter (where c.rating > 0) as "averageRating",
                   (array_agg(c.photo_url order by c.rating desc, c.visit_date desc)
                      filter (where c.photo_url is not null))[1] as "coverPhoto"
            from cafe_visits c
            where c.shared = true and c.latitude is not null and c.longitude is not null
              and (cast(:city as varchar) is null or lower(trim(c.city)) = lower(trim(cast(:city as varchar))))
            group by lower(c.name), lower(c.address)
            having cast(:kind as varchar) is null
                or bool_or(exists (select 1 from visit_kinds k where k.visit_id = c.id and k.kind = cast(:kind as varchar)))
            order by count(*) desc
            limit :limit""", nativeQuery = true)
    List<MapCafeView> findMapCafes(@Param("kind") String kind, @Param("city") String city, @Param("limit") int limit);

    interface MapCafeView {
        String getName();
        String getAddress();
        String getCity();
        Double getLatitude();
        Double getLongitude();
        Long getVisits();
        BigDecimal getAverageRating();
        String getCoverPhoto();
    }
}
