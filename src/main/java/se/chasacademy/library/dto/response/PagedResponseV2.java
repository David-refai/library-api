package se.chasacademy.library.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Wrapper response object used by API v2.
 * Wraps a list of BookResponseV2 items together with the API version string.
 *
 * Example:
 * {
 *   "data": [ { "title": "...", "author": "...", "available": true } ],
 *   "version": "v2"
 * }
 */
@Schema(description = "Paginated wrapper response for v2 endpoints")
public class PagedResponseV2<T> {

    @Schema(description = "List of book items")
    private List<T> data;

    @Schema(description = "API version identifier", example = "v2")
    private String version;

    // ── Constructors ──────────────────────────────────────────────────────────

    public PagedResponseV2() {}

    public PagedResponseV2(List<T> data, String version) {
        this.data = data;
        this.version = version;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public List<T> getData() { return data; }
    public void setData(List<T> data) { this.data = data; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
}
