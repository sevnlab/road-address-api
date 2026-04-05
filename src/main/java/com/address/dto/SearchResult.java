package com.address.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SearchResult {
    private int totalCount;
    private int currentPage;
    private int countPerPage;
    private List<AddressResponse> results;
}
