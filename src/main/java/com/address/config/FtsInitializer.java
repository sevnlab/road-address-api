package com.address.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Slf4j
@Component
@RequiredArgsConstructor
public class FtsInitializer {

    private final DataSource dataSource;

    // FTS5 비활성화 - 기존 인덱스(idx_sigungu, idx_road_name 등) 활용으로 전환
    // @EventListener(ApplicationReadyEvent.class)
    public void init() {
        boolean ready = false;
        try {
            ready = isFtsIndexReady();
        } catch (Exception e) {
            log.warn("FTS5 인덱스 상태 확인 실패 (재생성 진행): {}", e.getMessage());
        }

        if (ready) {
            log.info("FTS5 인덱스 확인 완료 - 검색 준비됨");
            return;
        }

        try {
            log.info("FTS5 인덱스 생성 중... (최초 1회, 수 분 소요됩니다)");
            buildFtsIndex();
            log.info("FTS5 인덱스 생성 완료");
        } catch (Exception e) {
            log.error("FTS5 인덱스 생성 실패", e);
        }
    }

    private boolean isFtsIndexReady() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // 테이블 존재 여부 확인
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT count(*) FROM sqlite_master WHERE type='table' AND name='road_address_fts'")) {
                if (!rs.next() || rs.getInt(1) == 0) return false;
            }

            // 실제 데이터 존재 여부 확인 (빈 테이블이면 rebuild 필요)
            try (ResultSet rs = stmt.executeQuery("SELECT rowid FROM road_address_fts LIMIT 1")) {
                return rs.next();
            }
        }
    }

    private void buildFtsIndex() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout(0); // 타임아웃 없음 (대용량 인덱싱)
            stmt.execute("DROP TABLE IF EXISTS road_address_fts");
            stmt.execute("""
                    CREATE VIRTUAL TABLE road_address_fts
                    USING fts5(road_name, eupmyeondong, building_name, sigungu, building_no,
                               content='road_address')
                    """);
            log.info("FTS5 테이블 생성 완료. 데이터 인덱싱 중...");
            stmt.execute("INSERT INTO road_address_fts(road_address_fts) VALUES('rebuild')");
        }
    }
}
