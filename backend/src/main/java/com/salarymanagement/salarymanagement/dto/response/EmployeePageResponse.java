package com.salarymanagement.salarymanagement.dto.response;

import java.util.List;

public class EmployeePageResponse {

    private List<EmployeeResponse> content;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;
    private boolean hasNext;

    public EmployeePageResponse() {
    }

    public EmployeePageResponse(List<EmployeeResponse> content, long totalElements,
                                int totalPages, int page, int size, boolean hasNext) {
        this.content = content;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.page = page;
        this.size = size;
        this.hasNext = hasNext;
    }

    public List<EmployeeResponse> getContent() {
        return content;
    }

    public void setContent(List<EmployeeResponse> content) {
        this.content = content;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }
}
