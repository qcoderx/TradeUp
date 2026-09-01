package ng.edu.unilag.tradeup.web.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * A slice of results plus the numbers the UI needs to draw pagination, without
 * leaking the shape of Spring Data Page into the public API.
 */
public record PageResponse<T>(
        List<T> items, int page, int size, long totalItems, int totalPages, boolean hasNext, boolean hasPrevious) {

    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious());
    }
}
