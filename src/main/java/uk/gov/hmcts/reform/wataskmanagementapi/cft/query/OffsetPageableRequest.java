package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.Serializable;


@EqualsAndHashCode
@ToString
@SuppressWarnings({"PMD.ShortMethodName", "PMD.AvoidLiteralsInIfCondition"})
public class OffsetPageableRequest implements Pageable, Serializable {

    private static final long serialVersionUID = 5127306127952604824L;

    private final int limit;
    private final long offset;
    private final Sort sort;

    /**
     * Creates a new {@link OffsetPageableRequest} with sort parameters applied.
     *
     * @param offset zero-based offset.
     * @param limit  the size of the elements to be returned.
     * @param sort   can be {@literal null}.
     */
    protected OffsetPageableRequest(long offset, int limit, Sort sort) {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset index must not be less than zero");
        }

        if (limit < 1) {
            throw new IllegalArgumentException("Limit must not be less than one");
        }
        this.limit = limit;
        this.offset = offset;
        this.sort = sort;
    }

    /**
     * Creates a new {@link OffsetPageableRequest} with sort parameters applied.
     *
     * @param offset zero-based offset.
     * @param limit  the size of the elements to be returned.
     */
    public static OffsetPageableRequest of(int offset, int limit) {
        return new OffsetPageableRequest(offset, limit, Sort.unsorted());
    }

    /**
     * Creates a new {@link OffsetPageableRequest} with sort parameters applied.
     *
     * @param offset zero-based page offset must not be negative.
     * @param limit  the size of the elements to be returned.
     * @param sort   must not be {@literal null}, use {@link Sort#unsorted()} instead.
     */
    public static OffsetPageableRequest of(int offset, int limit, Sort sort) {
        return new OffsetPageableRequest(offset, limit, sort);
    }

    @Override
    public int getPageNumber() {
        return Math.toIntExact(offset / limit);
    }

    @Override
    public int getPageSize() {
        return limit;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    public int getLimit() {
        return limit;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public Pageable next() {
        return new OffsetPageableRequest(getOffset() + getPageSize(), getPageSize(), getSort());
    }

    public OffsetPageableRequest previous() {
        return hasPrevious() ? new OffsetPageableRequest(getOffset() - getPageSize(), getPageSize(), getSort()) : this;
    }


    @Override
    public Pageable previousOrFirst() {
        return hasPrevious() ? previous() : first();
    }

    @Override
    public Pageable first() {
        return new OffsetPageableRequest(0, getPageSize(), getSort());
    }

    @Override
    public boolean hasPrevious() {
        return offset > limit;
    }
}
