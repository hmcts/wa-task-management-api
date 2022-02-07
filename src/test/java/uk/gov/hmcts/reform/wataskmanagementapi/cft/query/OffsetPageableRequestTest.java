package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import org.junit.Test;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class OffsetPageableRequestTest {

    @Test
    public void should_create_pageable_request_with_defaults() {

        OffsetPageableRequest request = OffsetPageableRequest.of(0, 25);
        assertNotNull(request);
        assertEquals(0, request.getOffset());
        assertEquals(25, request.getLimit());
        assertEquals(0, request.getPageNumber()); // Page 1 (starts from 0)
        assertEquals(Sort.unsorted(), request.getSort());
    }

    @Test
    public void should_create_pageable_request_with_defaults_page_two() {

        OffsetPageableRequest request = OffsetPageableRequest.of(25, 25);
        assertNotNull(request);
        assertEquals(25, request.getOffset());
        assertEquals(25, request.getLimit());
        assertEquals(1, request.getPageNumber()); // Page 2 (starts from 0)
        assertEquals(Sort.unsorted(), request.getSort());
    }

    @Test
    public void should_create_pageable_request_with_defaults_page_three() {
        OffsetPageableRequest request = OffsetPageableRequest.of(50, 25);
        assertNotNull(request);
        assertEquals(50, request.getOffset());
        assertEquals(25, request.getLimit());
        assertEquals(2, request.getPageNumber()); // Page 3 (starts from 0)
        assertEquals(Sort.unsorted(), request.getSort());
    }

    @Test
    public void should_create_pageable_request_with_defaults_edge_case() {
        OffsetPageableRequest request = OffsetPageableRequest.of(23, 25);
        assertNotNull(request);
        assertEquals(23, request.getOffset());
        assertEquals(25, request.getLimit());
        assertEquals(0, request.getPageNumber());
        assertEquals(Sort.unsorted(), request.getSort());
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
    public void should_create_pageable_request_with_sort_page_two() {

        OffsetPageableRequest request = OffsetPageableRequest.of(
            25,
            25,
            Sort.by("locationName").descending()
        );
        assertNotNull(request);
        assertEquals(25, request.getOffset());
        assertEquals(25, request.getLimit());
        assertEquals(1, request.getPageNumber()); // Page 2 (starts from 0)
        assertEquals(Sort.by("locationName").descending(), request.getSort());
    }

    @Test
    public void should_create_pageable_request_with_sort_page_three() {
        OffsetPageableRequest request = OffsetPageableRequest.of(
            50,
            25,
            Sort.by("locationName").descending()
        );
        assertNotNull(request);
        assertEquals(50, request.getOffset());
        assertEquals(25, request.getLimit());
        assertEquals(2, request.getPageNumber()); // Page 3 (starts from 0)
        assertEquals(Sort.by("locationName").descending(), request.getSort());
    }

    @Test
    public void should_create_pageable_request_with_sort_edge_case() {
        OffsetPageableRequest request = OffsetPageableRequest.of(
            23,
            25,
            Sort.by("locationName").descending()
        );
        assertNotNull(request);
        assertEquals(23, request.getOffset());
        assertEquals(25, request.getLimit());
        assertEquals(0, request.getPageNumber());
        assertEquals(Sort.by("locationName").descending(), request.getSort());
    }
}
