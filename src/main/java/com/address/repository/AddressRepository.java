package com.address.repository;

import com.address.dto.AddressResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AddressRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<AddressResponse> search(String keyword, int limit, int offset) {
        String sql = """
                SELECT DISTINCT
                    zip_code,
                    sido || ' '
                    || CASE WHEN sigungu   IS NULL OR sigungu   = '' THEN '' ELSE sigungu   || ' ' END
                    || CASE WHEN eupmyeondong IS NULL OR eupmyeondong = '' THEN ''
                            ELSE CASE WHEN ri IS NULL OR ri = '' THEN ''
                                      ELSE eupmyeondong || ' '
                                 END
                       END
                    || road_name || ' '
                    || CASE underground WHEN 1 THEN '지하' WHEN 2 THEN '공중' WHEN 3 THEN '수상' ELSE '' END
                    || building_no
                    || CASE WHEN building_sub IS NULL OR building_sub = 0 THEN '' ELSE '-' || building_sub END
                    || CASE
                           WHEN is_apartment = 0 OR is_apartment IS NULL THEN
                               CASE WHEN eupmyeondong IS NULL OR eupmyeondong = '' THEN ''
                                    ELSE CASE WHEN ri IS NULL OR ri = '' THEN '(' || eupmyeondong || ')' ELSE '' END
                               END
                           WHEN is_apartment = 1 THEN
                               CASE WHEN eupmyeondong IS NULL OR eupmyeondong = '' THEN
                                        CASE WHEN building_name IS NULL OR building_name = '' THEN ''
                                             ELSE '(' || building_name || ')'
                                        END
                                    ELSE '('
                                         || CASE WHEN ri IS NULL OR ri = '' THEN eupmyeondong || ', ' ELSE '' END
                                         || CASE WHEN building_name IS NULL OR building_name = '' THEN '' ELSE building_name END
                                         || ')'
                               END
                           ELSE ''
                       END AS road_address,
                    eupmyeondong,
                    building_name
                FROM road_address
                WHERE road_name    LIKE ?
                   OR eupmyeondong LIKE ?
                   OR building_name LIKE ?
                   OR sigungu      LIKE ?
                LIMIT ? OFFSET ?
                """;
        String param = "%" + keyword + "%";
        return jdbcTemplate.query(sql, (rs, rowNum) ->
                AddressResponse.builder()
                        .zipCode(rs.getString("zip_code"))
                        .roadAddress(rs.getString("road_address"))
                        .eupmyeondong(rs.getString("eupmyeondong"))
                        .buildingName(rs.getString("building_name"))
                        .build()
        , param, param, param, param, limit, offset);
    }
}
