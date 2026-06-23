package se.chasacademy.library.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Wrapper response object used by API v2.
 * Wraps a list of BookResponseV2 items together with the API version string and pagination metadata.
 */
@Schema(description = "Paginated wrapper response for v2 endpoints")
public class PagedResponseV2<T> {

    @Schema(description = "List of items")
    private List<T> data;

    @Schema(description = "API version identifier", example = "v2")
    private String version;

    @Schema(description = "Current page number", example = "0")
    private int pageNumber;

    @Schema(description = "Size of the page", example = "10")
    private int pageSize;

    @Schema(description = "Total number of elements across all pages", example = "100")
    private long totalElements;

    @Schema(description = "Total number of pages", example = "10")
    private int totalPages;

    // ── Constructors ──────────────────────────────────────────────────────────

    public PagedResponseV2() {}

    public PagedResponseV2(Page<T> page, String version) {
        this.data = page.getContent();
        this.version = version;
        this.pageNumber = page.getNumber();
        this.pageSize = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public List<T> getData() { return data; }
    public void setData(List<T> data) { this.data = data; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public int getPageNumber() { return pageNumber; }
    public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}
