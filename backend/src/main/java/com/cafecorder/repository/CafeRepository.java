package com.cafecorder.repository;

import com.cafecorder.model.Cafe;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

// Spring Data writes the implementation. Queries on Cafe also return StudyCafe rows, since a StudyCafe is a Cafe.
public interface CafeRepository extends JpaRepository<Cafe, Long> {

    List<Cafe> findByNeighbourhoodIgnoreCase(String neighbourhood, Sort sort);

    @Query("select distinct c.neighbourhood from Cafe c order by c.neighbourhood")
    List<String> findNeighbourhoods();

    // null when there are no visits
    @Query("select sum(c.price) from Cafe c")
    BigDecimal totalSpent();

    // unrated (0) visits are left out so they don't drag the average down; null when nothing is rated
    @Query("select avg(c.rating) from Cafe c where c.rating > 0")
    Double averageRating();

    // same cafe = same name + neighbourhood, ignoring case (like the old equals())
    @Query(value = """
            select count(*) from (
              select distinct lower(name), lower(neighbourhood) from cafe_visits
            ) t""", nativeQuery = true)
    long countUniqueCafes();
}
