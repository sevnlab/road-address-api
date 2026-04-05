package com.address.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IndexInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        log.info("인덱스 확인 중...");

        createIndex("idx_road_name",    "road_address(road_name)");
        createIndex("idx_eupmyeondong", "road_address(eupmyeondong)");
        createIndex("idx_building_name","road_address(building_name)");
        createIndex("idx_zip_code",     "road_address(zip_code)");
        createIndex("idx_building_type","road_address(building_type)");

        log.info("인덱스 확인 완료");
    }

    private void createIndex(String indexName, String tableDotCol) {
        String sql = "CREATE INDEX IF NOT EXISTS " + indexName + " ON " + tableDotCol;
        try {
            jdbcTemplate.execute(sql);
            log.info("  인덱스 OK: {}", indexName);
        } catch (Exception e) {
            log.warn("  인덱스 생성 실패 {}: {}", indexName, e.getMessage());
        }
    }
}
