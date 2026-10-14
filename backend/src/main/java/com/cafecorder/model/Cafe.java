package com.cafecorder.model;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/*
 * One cafe visit, same idea as the original Cafe class.
 * Single-table inheritance: Cafe and StudyCafe share the cafe_visits table and
 * visit_type says which is which. One table means no joins, at the cost of
 * study-only columns being null on regular visits.
 */
@Entity
@Table(name = "cafe_visits")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "visit_type", length = 10)
@DiscriminatorValue("CAFE")
public class Cafe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 120)
    private String neighbourhood;

    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    // ORDER is a reserved word in SQL
    @Column(name = "order_item", nullable = false, length = 200)
    private String order;

    // BigDecimal, not double: 0.1 + 0.2 should be 0.30, not 0.30000000000000004
    @Column(nullable = false, precision = 7, scale = 2)
    private BigDecimal price;

    // 0 = unrated, 1-5 otherwise
    @Column(nullable = false)
    private int rating;

    // whose logbook this is. LAZY: the user row is only loaded if something actually reads it.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    // an https:// image link, or a small uploaded photo stored as a data URL
    @Column(name = "photo_url", columnDefinition = "text")
    private String photoUrl;

    // shown on the public feed when true
    @Column(nullable = false)
    private boolean shared = true;

    // Loaded lazily; @BatchSize loads the extras for up to 50 visits in one query instead of one per visit
    @ElementCollection
    @CollectionTable(name = "visit_extras", joinColumns = @JoinColumn(name = "visit_id"))
    @OrderColumn(name = "item_index")
    @BatchSize(size = 50)
    private List<Extra> extras = new ArrayList<>();

    protected Cafe() {
        // JPA needs a no-arg constructor
    }

    public Cafe(String name, String neighbourhood, LocalDate visitDate, String order, BigDecimal price, int rating) {
        this.name = name;
        this.neighbourhood = neighbourhood;
        this.visitDate = visitDate;
        this.order = order;
        this.price = price;
        this.rating = rating;
    }

    public Cafe(String name, String neighbourhood, LocalDate visitDate, String order, BigDecimal price) {
        this(name, neighbourhood, visitDate, order, price, 0);
    }

    public boolean isStudySpot() {
        return false;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getNeighbourhood() { return neighbourhood; }
    public LocalDate getVisitDate() { return visitDate; }
    public String getOrder() { return order; }
    public BigDecimal getPrice() { return price; }
    public int getRating() { return rating; }
    public AppUser getUser() { return user; }
    public String getPhotoUrl() { return photoUrl; }
    public boolean isShared() { return shared; }
    public List<Extra> getExtras() { return extras; }

    // drink plus extras
    public BigDecimal getTotal() {
        return extras.stream().map(Extra::getPrice).reduce(price, BigDecimal::add);
    }

    public void setName(String name) { this.name = name; }
    public void setNeighbourhood(String neighbourhood) { this.neighbourhood = neighbourhood; }
    public void setVisitDate(LocalDate visitDate) { this.visitDate = visitDate; }
    public void setOrder(String order) { this.order = order; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setRating(int rating) { this.rating = rating; }
    public void setUser(AppUser user) { this.user = user; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public void setShared(boolean shared) { this.shared = shared; }

    // edits the list Hibernate is tracking instead of swapping it out
    public void setExtras(List<Extra> extras) {
        this.extras.clear();
        this.extras.addAll(extras);
    }

    @Override
    public String toString() {
        String stars = rating == 0 ? "Unrated" : rating + "/5";
        return visitDate + " | " + name + " (" + neighbourhood + ") - " + order + " $" + price + " [" + stars + "]";
    }
}
