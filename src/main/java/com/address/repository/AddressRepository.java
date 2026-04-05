package com.address.repository;

import com.address.dto.AddressResponse;
import com.address.dto.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class AddressRepository {

    private final JdbcTemplate jdbcTemplate;

    // road_address 실제 컬럼 매핑
    // building_type  = 건물본번  (cols[12])
    // is_apartment   = 건물부번  (cols[13])
    // is_bulk        = 지하여부  (cols[11])
    // change_reason  = 공동주택여부 (cols[19])
    // underground    = 산여부    (cols[6])
    // building_name  = 건물명    (cols[22])
    // col22          = 건물명2   (cols[21])
    private static final String SELECT_PART = """
            SELECT
                A.zip_code,
                A.sido || ' '
                || CASE WHEN A.sigungu IS NULL OR A.sigungu = '' THEN '' ELSE A.sigungu || ' ' END
                || CASE WHEN A.eupmyeondong IS NULL OR A.eupmyeondong = '' THEN ''
                        ELSE CASE WHEN A.ri IS NULL OR A.ri = '' THEN ''
                                  ELSE A.eupmyeondong || ' '
                             END
                   END
                || A.road_name || ' '
                || CASE A.is_bulk WHEN 1 THEN '지하' WHEN 2 THEN '공중' WHEN 3 THEN '수상' ELSE '' END
                || A.building_type
                || CASE WHEN A.is_apartment IS NULL OR A.is_apartment = 0 THEN '' ELSE '-' || A.is_apartment END
                || CASE
                       WHEN A.change_reason = '0' OR A.change_reason IS NULL THEN
                           CASE WHEN A.eupmyeondong IS NULL OR A.eupmyeondong = '' THEN ''
                                ELSE CASE WHEN A.ri IS NULL OR A.ri = '' THEN '(' || A.eupmyeondong || ')' ELSE '' END
                           END
                       WHEN A.change_reason = '1' THEN
                           CASE WHEN A.eupmyeondong IS NULL OR A.eupmyeondong = '' THEN
                                    CASE WHEN A.building_name IS NULL OR A.building_name = '' THEN ''
                                         ELSE '(' || A.building_name || ')'
                                    END
                                ELSE '('
                                     || CASE WHEN A.ri IS NULL OR A.ri = '' THEN
                                                 CASE WHEN A.building_name IS NULL OR A.building_name = '' THEN A.eupmyeondong ELSE A.eupmyeondong || ', ' END
                                             ELSE '' END
                                     || CASE WHEN A.building_name IS NULL OR A.building_name = '' THEN '' ELSE A.building_name END
                                     || ')'
                           END
                       ELSE ''
                   END AS road_address,
                A.sido || ' '
                || CASE WHEN A.sigungu IS NULL OR A.sigungu = '' THEN '' ELSE A.sigungu || ' ' END
                || CASE WHEN A.eupmyeondong IS NULL OR A.eupmyeondong = '' THEN '' ELSE A.eupmyeondong || ' ' END
                || CASE WHEN A.ri IS NULL OR A.ri = '' THEN '' ELSE A.ri || ' ' END
                || A.building_no
                || CASE WHEN A.building_sub IS NULL OR A.building_sub = 0 THEN '' ELSE '-' || A.building_sub END
                || CASE WHEN A.building_name IS NULL OR A.building_name = '' THEN '' ELSE ' ' || A.building_name END
                AS jibun_address,
                A.building_mgmt  AS mgmt_no,
                A.road_code      AS bupjungdong_code,
                A.sido,
                A.sigungu,
                A.eupmyeondong,
                A.ri,
                A.underground    AS san,
                A.admin_code     AS road_code,
                A.road_name,
                A.is_bulk        AS underground,
                A.building_type  AS building_no,
                A.is_apartment   AS building_sub_no,
                A.building_no    AS jibun_no,
                A.building_sub   AS jibun_sub_no,
                A.admin_code2    AS admin_code,
                A.admin_dong,
                A.prev_address   AS move_reason_code,
                A.effective_date AS notice_date,
                A.change_reason  AS is_apartment,
                A.col21          AS building_ledger,
                A.building_name,
                COUNT(*) OVER()  AS total_count
            FROM road_address A
            """;

    public SearchResult search(String keyword, int limit, int offset) {
        WhereClause where = buildWhere(keyword);
        if (where == null) return SearchResult.builder()
                .totalCount(0).results(Collections.emptyList()).build();

        String sql = SELECT_PART + where.sql + " LIMIT ? OFFSET ?";
        List<Object> params = new ArrayList<>(where.params);
        params.add(limit);
        params.add(offset);

        log.debug("검색 - keyword: {}", keyword);

        final int[] totalCount = {0};
        List<AddressResponse> results = jdbcTemplate.query(sql, (rs, rowNum) -> {
            if (totalCount[0] == 0) totalCount[0] = rs.getInt("total_count");
            return AddressResponse.builder()
                    .zipCode(rs.getString("zip_code"))
                    .roadAddress(rs.getString("road_address"))
                    .mgmtNo(rs.getString("mgmt_no"))
                    .bupjungdongCode(rs.getString("bupjungdong_code"))
                    .sido(rs.getString("sido"))
                    .sigungu(rs.getString("sigungu"))
                    .eupmyeondong(rs.getString("eupmyeondong"))
                    .ri(rs.getString("ri"))
                    .san(rs.getString("san"))
                    .roadCode(rs.getString("road_code"))
                    .roadName(rs.getString("road_name"))
                    .underground(rs.getString("underground"))
                    .buildingNo(rs.getInt("building_no"))
                    .buildingSubNo(rs.getInt("building_sub_no"))
                    .adminCode(rs.getString("admin_code"))
                    .adminDong(rs.getString("admin_dong"))
                    .moveReasonCode(rs.getString("move_reason_code"))
                    .noticeDate(rs.getString("notice_date"))
                    .isApartment(rs.getString("is_apartment"))
                    .buildingLedger(rs.getString("building_ledger"))
                    .buildingName(rs.getString("building_name"))
                    .jibunAddress(rs.getString("jibun_address"))
                    .jibunNo(rs.getInt("jibun_no"))
                    .jibunSubNo(rs.getInt("jibun_sub_no"))
                    .build();
        }, params.toArray());

        return SearchResult.builder()
                .totalCount(totalCount[0])
                .results(results)
                .build();
    }

    private WhereClause buildWhere(String keyword) {
        List<String> textTokens = new ArrayList<>();
        String zipCode = null;
        Integer buildingNo = null;

        for (String token : keyword.trim().split("\\s+")) {
            if (token.matches("\\d{5}")) {
                zipCode = token;
            } else if (token.matches("\\d+")) {
                buildingNo = Integer.parseInt(token);
            } else {
                textTokens.add(token);
            }
        }

        if (textTokens.isEmpty() && zipCode == null && buildingNo == null) return null;

        StringBuilder sql = new StringBuilder("WHERE 1=1");
        List<Object> params = new ArrayList<>();

        for (String text : textTokens) {
            sql.append("""
                     AND (A.sido          LIKE ?
                          OR A.sigungu    LIKE ?
                          OR A.eupmyeondong LIKE ?
                          OR A.road_name  LIKE ?
                          OR A.building_name LIKE ?)
                    """);
            String p = text + "%";
            params.add(p); params.add(p); params.add(p); params.add(p); params.add(p);
        }

        if (zipCode != null) {
            sql.append(" AND A.zip_code = ?");
            params.add(zipCode);
        }

        if (buildingNo != null) {
            sql.append(" AND A.building_type = ?");  // building_type = 실제 건물본번
            params.add(buildingNo);
        }

        return new WhereClause(sql.toString(), params);
    }

    private record WhereClause(String sql, List<Object> params) {}
}
