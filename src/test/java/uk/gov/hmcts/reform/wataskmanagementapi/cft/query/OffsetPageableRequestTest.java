package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.Sort;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OffsetPageableRequestTest {

    @ParameterizedTest
    @MethodSource("providePageableRequests")
    public void should_create_pageable_request_with_defaults(int offset, int limit, int expectedPageNumber) {
        OffsetPageableRequest request = OffsetPageableRequest.of(offset, limit);

        assertNotNull(request);
        assertEquals(offset, request.getOffset());
        assertEquals(limit, request.getLimit());
        assertEquals(expectedPageNumber, request.getPageNumber());
        assertEquals(Sort.unsorted(), request.getSort());
    }

    @ParameterizedTest
    @MethodSource("providePageableRequestsWithSort")
    public void should_create_pageable_request_with_sort(int offset, int limit, int expectedPageNumber, Sort sort) {
        OffsetPageableRequest request = OffsetPageableRequest.of(offset, limit, sort);

        assertNotNull(request);
        assertEquals(offset, request.getOffset());
        assertEquals(limit, request.getLimit());
        assertEquals(expectedPageNumber, request.getPageNumber());
        assertEquals(sort, request.getSort());
    }

    @Test
    public void should_create_pageable_request_with_sort() {

        OffsetPageableRequest request = OffsetPageableRequest.of(
            0,
            25,
            Sort.by("locationName").descending()
        );
        assertNotNull(request);
        assertEquals(0, request.getOffset());
        assertEquals(25, request.getLimit());
        assertEquals(0, request.getPageNumber()); // Page 1 (starts from 0)
        assertEquals(Sort.by("locationName").descending(), request.getSort());
    }

    @Test
    public void should_cover_override_methods() {
        OffsetPageableRequest request = OffsetPageableRequest.of(
            0,
            25
        );

        assertNotNull(request);
        assertEquals(0, request.getOffset());
        assertEquals(25, request.getLimit());
        assertEquals(0, request.getPageNumber());
        assertEquals(Sort.unsorted(), request.getSort());
        assertEquals(OffsetPageableRequest.of(25, 25), request.next());

        assertEquals(request, request.previous()); //Does not have previous
        //This is the first page
        assertEquals(request, request.first()); //Should be the same
        assertFalse(request.hasPrevious());
        assertEquals(request, request.previousOrFirst()); //should return same page since it does not have a previous
    }

    @Test
    public void should_cover_override_methods_with_multiple_pages() {
        OffsetPageableRequest request = OffsetPageableRequest.of(
            50,
            25
        );

        assertNotNull(request);
        assertEquals(50, request.getOffset());
        assertEquals(25, request.getLimit());
        assertEquals(2, request.getPageNumber());
        assertEquals(Sort.unsorted(), request.getSort());
        assertEquals(OffsetPageableRequest.of(75, 25), request.next());
        assertEquals(OffsetPageableRequest.of(25, 25), request.previous());
        assertEquals(OffsetPageableRequest.of(0, 25), request.first()); //Should be the same
        assertTrue(request.hasPrevious());
        assertEquals(OffsetPageableRequest.of(25, 25), request.previousOrFirst());
    }

    @Test
    public void should_cover_override_methods_with_Page() {
        OffsetPageableRequest request = OffsetPageableRequest.of(
            50,
            25
        );
        assertNotNull(request);
        assertEquals(OffsetPageableRequest.of(2, 25), request.withPage(2));
    }

    private static Stream<Object[]> providePageableRequests() {
        return Stream.of(
            new Object[]{0, 25, 0},   // Page 1
            new Object[]{25, 25, 1},  // Page 2
            new Object[]{50, 25, 2},  // Page 3
            new Object[]{23, 25, 0}   // Edge case
        );
    }

    private static Stream<Object[]> providePageableRequestsWithSort() {
        return Stream.of(
            new Object[]{0, 25, 0, Sort.by("locationName").descending()},    // Page 1 with sort
            new Object[]{25, 25, 1, Sort.by("locationName").descending()},   // Page 2 with sort
            new Object[]{50, 25, 2, Sort.by("locationName").descending()},   // Page 3 with sort
            new Object[]{23, 25, 0, Sort.by("locationName").descending()}    // Edge case with sort
        );
    }
}
