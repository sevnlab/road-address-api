package com.address.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AddressResponse {
    private String zipCode;         // 기초구역번호
    private String roadAddress;     // 도로명주소 (computed)
    private String mgmtNo;          // 도로명주소관리번호
    private String bupjungdongCode; // 법정동코드
    private String sido;            // 시도명
    private String sigungu;         // 시군구명
    private String eupmyeondong;    // 읍면동명
    private String ri;              // 리명
    private String san;             // 산여부
    private String roadCode;        // 도로명코드
    private String roadName;        // 도로명
    private String underground;     // 지하여부
    private int    buildingNo;      // 건물본번
    private int    buildingSubNo;   // 건물부번
    private String adminCode;       // 행정동코드
    private String adminDong;       // 행정동명
    private String moveReasonCode;  // 이동사유코드
    private String noticeDate;      // 고시일자
    private String isApartment;     // 공동주택여부
    private String buildingLedger;  // 건축물대장건물명
    private String buildingName;    // 건물명
    private String jibunAddress;    // 지번주소 (computed)
    private int    jibunNo;         // 번지(지번본번)
    private int    jibunSubNo;      // 호(지번부번)
}
