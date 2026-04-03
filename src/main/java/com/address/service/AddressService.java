package com.address.service;

import com.address.dto.AddressRequest;
import com.address.dto.AddressResponse;
import com.address.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;

    public List<AddressResponse> search(AddressRequest request) {
        String keyword = request.getKeyword();
        if (keyword == null || keyword.trim().length() < 2) {
            return Collections.emptyList();
        }

        int countPerPage = Math.min(Math.max(request.getCountPerPage(), 1), 100);
        int currentPage  = Math.max(request.getCurrentPage(), 1);
        int offset       = (currentPage - 1) * countPerPage;

        return addressRepository.search(keyword.trim(), countPerPage, offset);
    }
}
