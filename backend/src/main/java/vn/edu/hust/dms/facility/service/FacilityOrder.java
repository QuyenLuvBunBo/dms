package vn.edu.hust.dms.facility.service;

import java.util.Comparator;

/** Orders codes the way people read them: B6 before B10, bed 9 before bed 10. */
final class FacilityOrder {

    static final Comparator<String> CODES = Comparator.comparingInt(String::length)
            .thenComparing(String.CASE_INSENSITIVE_ORDER);

    private FacilityOrder() {
    }
}
