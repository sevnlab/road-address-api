package com.address.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;

@Configuration
public class SqliteConfig {

    @Value("${address.db.path}")
    private String dbPath;

    @Bean
    public DataSource sqliteDataSource() {
        SQLiteConfig sqliteConfig = new SQLiteConfig();
        // WAL모드 = SQLite가 읽기 요청 여러 개를 동시에 허용 (읽기 끼리는 서로 안막음)
        sqliteConfig.setJournalMode(SQLiteConfig.JournalMode.WAL);
        sqliteConfig.setCacheSize(-65536);           // 64MB 캐시
        sqliteConfig.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
        sqliteConfig.setTempStore(SQLiteConfig.TempStore.MEMORY);
        // sqlite 는 like문 사용시 기본적으로 대소문자 구분없이 비교
        // 대소문자 무시하면 인덱스 정렬 순서 깨짐, 그래서 전체데이터 다읽음
        // 아래 옵션을 키면 LIKE도 대소문자 정확히 구분
        sqliteConfig.enableCaseSensitiveLike(true);  // 한글 LIKE에 인덱스 사용하도록 강제

        SQLiteDataSource sqDs = new SQLiteDataSource(sqliteConfig);
        sqDs.setUrl("jdbc:sqlite:" + dbPath);

        HikariConfig hikari = new HikariConfig();
        hikari.setDataSource(sqDs);
        hikari.setMaximumPoolSize(8);
        hikari.setMinimumIdle(2);

        return new HikariDataSource(hikari);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource sqliteDataSource) {
        return new JdbcTemplate(sqliteDataSource);
    }
}
