package com.address.controller;

import com.address.dto.AddressRequest;
import com.address.dto.AddressResponse;
import com.address.dto.ApiResponse;
import com.address.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @PostMapping("/search")
    public ApiResponse<List<AddressResponse>> search(@RequestBody AddressRequest request) {
        return ApiResponse.success("주소 검색 성공", addressService.search(request));
    }
}
