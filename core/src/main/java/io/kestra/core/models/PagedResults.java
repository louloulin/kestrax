package io.kestra.core.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Generic wrapper for paginated results.
 */
@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
    title = "Paged Results",
    description = "A wrapper for paginated query results"
)
public class PagedResults<T> {
    
    @NotNull
    @Schema(title = "List of results for the current page")
    List<T> results;
    
    @NotNull
    @Schema(title = "Total number of items across all pages")
    Long total;
    
    @Schema(title = "Current page number (0-based)")
    Integer page;
    
    @Schema(title = "Number of items per page")
    Integer size;
    
    @Schema(title = "Total number of pages")
    Integer totalPages;
    
    @Schema(title = "Whether this is the first page")
    Boolean first;
    
    @Schema(title = "Whether this is the last page")
    Boolean last;
    
    @Schema(title = "Whether there is a next page")
    Boolean hasNext;
    
    @Schema(title = "Whether there is a previous page")
    Boolean hasPrevious;
    
    /**
     * Create a PagedResults instance with calculated pagination metadata.
     */
    public static <T> PagedResults<T> of(List<T> results, long total, int page, int size) {
        int totalPages = (int) Math.ceil((double) total / size);
        
        return PagedResults.<T>builder()
            .results(results)
            .total(total)
            .page(page)
            .size(size)
            .totalPages(totalPages)
            .first(page == 0)
            .last(page >= totalPages - 1)
            .hasNext(page < totalPages - 1)
            .hasPrevious(page > 0)
            .build();
    }
    
    /**
     * Create a simple PagedResults instance with just results and total.
     */
    public static <T> PagedResults<T> of(List<T> results, long total) {
        return PagedResults.<T>builder()
            .results(results)
            .total(total)
            .build();
    }
}
