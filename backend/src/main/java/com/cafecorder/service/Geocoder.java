package com.cafecorder.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Optional;

/*
 * Turns an address into coordinates with OpenStreetMap's free Nominatim service.
 * Their usage policy: identify the app in the User-Agent, at most 1 request per second,
 * and don't repeat lookups. So this only runs when an address is saved or changed,
 * and the result is stored with the visit.
 */
@Component
public class Geocoder {

    private static final Logger log = LoggerFactory.getLogger(Geocoder.class);
    private static final long MIN_GAP_MS = 1_000;

    public record Coordinates(double latitude, double longitude) {
    }

    // Nominatim sends lat/lon as strings
    private record Place(String lat, String lon) {
    }

    private final RestClient http;
    private long lastCall;

    public Geocoder(@Value("${app.geocoder.url:https://nominatim.openstreetmap.org}") String baseUrl,
                    @Value("${app.geocoder.user-agent:CafeCorder (https://github.com/fatima-rehan/cafecorder)}") String userAgent) {
        SimpleClientHttpRequestFactory timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(Duration.ofSeconds(3));
        timeouts.setReadTimeout(Duration.ofSeconds(4));
        this.http = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", userAgent)
                .requestFactory(timeouts)
                .build();
    }

    // Empty if nothing matched or the service is down: the visit still saves, it just isn't on the map.
    // synchronized + the wait keeps us at 1 request per second across all users.
    public synchronized Optional<Coordinates> locate(String address) {
        try {
            long wait = lastCall + MIN_GAP_MS - System.currentTimeMillis();
            if (wait > 0) Thread.sleep(wait);
            lastCall = System.currentTimeMillis();

            Place[] places = http.get()
                    .uri(u -> u.path("/search").queryParam("q", address).queryParam("format", "jsonv2")
                            .queryParam("limit", 1).build())
                    .retrieve()
                    .body(Place[].class);
            if (places == null || places.length == 0) return Optional.empty();
            return Optional.of(new Coordinates(Double.parseDouble(places[0].lat()), Double.parseDouble(places[0].lon())));
        } catch (RestClientException | NumberFormatException e) {
            log.warn("Couldn't geocode \"{}\": {}", address, e.getMessage());
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }
}
