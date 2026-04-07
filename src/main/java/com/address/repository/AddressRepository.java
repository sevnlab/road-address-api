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
                A.building_name
            FROM road_address A
            """;

    public int count(String keyword) {
        WhereClause where = buildWhere(keyword);
        if (where == null) return 0;
        String sql = "SELECT COUNT(*) FROM road_address A " + where.sql;
        Integer result = jdbcTemplate.queryForObject(sql, Integer.class, where.params.toArray());
        return result != null ? result : 0;
    }

    public List<AddressResponse> search(String keyword, int limit, int offset) {
        WhereClause where = buildWhere(keyword);
        if (where == null) return Collections.emptyList();

        String sql = SELECT_PART + where.sql + " LIMIT ? OFFSET ?";
        List<Object> params = new ArrayList<>(where.params);
        params.add(limit);
        params.add(offset);

        log.debug("검색 - keyword: {}", keyword);

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                AddressResponse.builder()
                        .zipCode(rs.getString("zip_code"))
                        .roadAddress(rs.getString("road_address"))
                        .jibunAddress(rs.getString("jibun_address"))
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
                        .jibunNo(rs.getInt("jibun_no"))
                        .jibunSubNo(rs.getInt("jibun_sub_no"))
                        .adminCode(rs.getString("admin_code"))
                        .adminDong(rs.getString("admin_dong"))
                        .moveReasonCode(rs.getString("move_reason_code"))
                        .noticeDate(rs.getString("notice_date"))
                        .isApartment(rs.getString("is_apartment"))
                        .buildingLedger(rs.getString("building_ledger"))
                        .buildingName(rs.getString("building_name"))
                        .build(),
                params.toArray());
    }

    // 텍스트+숫자 혼합 토큰 파싱용 패턴 (예: 통일로767, 통일로767-1, 진흥로50-8)
    // [^0-9\-] : 숫자도 대시도 아닌 문자(한글 등)로 텍스트 부분이 끝나야 올바르게 분리됨
    private static final java.util.regex.Pattern MIXED_TOKEN =
            java.util.regex.Pattern.compile("^(.*[^0-9\\-])(\\d+)(?:-(\\d+))?$");

    // 도로 가지번호 토큰 패턴 (예: 57길, 12로, 3번길) - 앞 텍스트 토큰에 합체
    private static final java.util.regex.Pattern ROAD_BRANCH_TOKEN =
            java.util.regex.Pattern.compile("^\\d+(?:대로|번길|번가|[로길가])$");

    private WhereClause buildWhere(String keyword) {
        List<String> textTokens = new ArrayList<>();
        String zipCode = null;
        Integer buildingNo = null;
        Integer buildingSubNo = null;

        for (String token : keyword.trim().split("\\s+")) {
            if (token.matches("\\d{5}")) {
                // 우편번호
                zipCode = token;
            } else if (token.matches("\\d+")) {
                // 본번만 (예: 767)
                buildingNo = Integer.parseInt(token);
            } else if (token.matches("\\d+-\\d+")) {
                // 본번-부번 (예: 767-1)
                String[] parts = token.split("-", 2);
                buildingNo = Integer.parseInt(parts[0]);
                buildingSubNo = Integer.parseInt(parts[1]);
            } else {
                // 텍스트 또는 텍스트+숫자 혼합 (예: 통일로767, 통일로767-1)
                java.util.regex.Matcher m = MIXED_TOKEN.matcher(token);
                if (m.matches()) {
                    textTokens.add(m.group(1));
                    buildingNo = Integer.parseInt(m.group(2));
                    if (m.group(3) != null) {
                        buildingSubNo = Integer.parseInt(m.group(3));
                    }
                } else if (ROAD_BRANCH_TOKEN.matcher(token).matches() && !textTokens.isEmpty()) {
                    // "57길", "12로", "3번길" 등 가지번호 → 앞 텍스트에 붙임 (예: "통일로"+"57길" → "통일로57길")
                    textTokens.set(textTokens.size() - 1, textTokens.get(textTokens.size() - 1) + token);
                } else {
                    textTokens.add(token);
                }
            }
        }

        if (textTokens.isEmpty() && zipCode == null && buildingNo == null) return null;

        StringBuilder sql = new StringBuilder("WHERE 1=1");
        List<Object> params = new ArrayList<>();

        for (String text : textTokens) {
            sql.append("""
                     AND (A.sido            LIKE ?
                          OR A.sigungu      LIKE ?
                          OR A.eupmyeondong LIKE ?
                          OR A.road_name    LIKE ?
                          OR A.building_name LIKE ?)
                    """);
            String p = text + "%";
            params.add(p); params.add(p); params.add(p); params.add(p); params.add(p);
        }

        if (zipCode != null) {
            sql.append(" AND A.zip_code = ?");
            params.add(zipCode);
        }

        // 건물번호: 도로명 본번(building_type) OR 지번 본번(building_no) 둘 다 검색
        if (buildingNo != null && buildingSubNo != null) {
            sql.append(" AND ((A.building_type = ? AND A.is_apartment = ?) OR (A.building_no = ? AND A.building_sub = ?))");
            params.add(buildingNo); params.add(buildingSubNo);
            params.add(buildingNo); params.add(buildingSubNo);
        } else if (buildingNo != null) {
            sql.append(" AND (A.building_type = ? OR A.building_no = ?)");
            params.add(buildingNo); params.add(buildingNo);
        }

        return new WhereClause(sql.toString(), params);
    }

    private record WhereClause(String sql, List<Object> params) {}
}
