package com.cafecorder.dto;

import com.cafecorder.model.CafeKind;
import com.cafecorder.model.NoiseLevel;
import com.cafecorder.model.Seating;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/*
 * What the frontend sends to create or update a visit. Replaces the old Scanner prompts:
 * the while-loop input checks are now annotations, checked by @Valid in the controller.
 */
public record VisitRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 120) String neighbourhood,
        @NotNull @PastOrPresent LocalDate visitDate,
        @NotBlank @Size(max = 200) String order,
        // matches NUMERIC(7,2): up to 99999.99
        @NotNull @DecimalMin("0.00") @Digits(integer = 5, fraction = 2) BigDecimal price,
        @NotNull @Min(0) @Max(5) Integer rating,
        // Boolean, not boolean: Jackson 3 rejects a missing primitive, and missing should just mean "no"
        Boolean studySession,
        Boolean hasWifi,
        Boolean hasOutlets,
        Seating seating,
        @Size(max = 100) String laptopPolicy,
        NoiseLevel noiseLevel,
        // only https links or jpeg/png/webp uploads, so nothing like "javascript:..." ends up in an <img src>.
        // ~3 MB cap; the frontend resizes uploads to 1200px JPEG, which is usually well under that.
        @Size(max = 3_000_000, message = "photo is too large")
        @Pattern(regexp = "^(https://|data:image/(jpeg|png|webp);base64,).*",
                message = "must be an https:// image link or a JPEG, PNG or WebP upload")
        String photoUrl,
        // null counts as true, so visits are shared unless you opt out
        Boolean shared,
        // anything besides the main drink. @Valid checks each item, so errors look like "extras[0].price".
        @Size(max = 10, message = "up to 10 extra items") List<@Valid ExtraItem> extras,
        // optional; used to pin the cafe on the map
        @Size(max = 200) String address,
        @Size(max = 120) String city,
        // sent when an address was picked from the search suggestions, so the server doesn't look it up again
        @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @DecimalMin("-180") @DecimalMax("180") Double longitude,
        // optional tags like MATCHA; an unknown name is a 400 from the JSON parser
        Set<CafeKind> kinds,
        // null counts as true, like shared
        Boolean showName
) {
    public boolean nameShown() {
        return !Boolean.FALSE.equals(showName);
    }

    public Set<CafeKind> kindsOrEmpty() {
        return kinds == null ? Set.of() : kinds;
    }

    // a pin needs both halves
    @AssertTrue(message = "latitude and longitude go together")
    public boolean isCoordinatesPaired() {
        return (latitude == null) == (longitude == null);
    }

    public List<ExtraItem> extrasOrEmpty() {
        return extras == null ? List.of() : extras;
    }

    public boolean isShareable() {
        return !Boolean.FALSE.equals(shared);
    }

    public boolean study() {
        return Boolean.TRUE.equals(studySession);
    }

    // Cross-field rule: a study session needs seating and noise level, a regular visit doesn't.
    // Shows up in the errors map as "studyDetailsComplete".
    @AssertTrue(message = "study sessions need seating and noise level")
    public boolean isStudyDetailsComplete() {
        return !study() || (seating != null && noiseLevel != null);
    }
}
