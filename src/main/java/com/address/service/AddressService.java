package com.address.service;

import com.address.dto.AddressRequest;
import com.address.dto.SearchResult;
import com.address.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;

    public SearchResult search(AddressRequest request) {
        String keyword = request.getKeyword();
        if (keyword == null || keyword.trim().length() < 2) {
            return SearchResult.builder()
                    .totalCount(0).currentPage(1).countPerPage(0)
                    .results(Collections.emptyList())
                    .build();
        }

        int countPerPage = Math.min(Math.max(request.getCountPerPage(), 1), 100);
        int currentPage  = Math.max(request.getCurrentPage(), 1);
        int offset       = (currentPage - 1) * countPerPage;
        String kw        = keyword.trim();

        SearchResult result = addressRepository.search(kw, countPerPage, offset);
        return SearchResult.builder()
                .totalCount(result.getTotalCount())
                .currentPage(currentPage)
                .countPerPage(countPerPage)
                .results(result.getResults())
                .build();
    }
}
