# road-address-api

한국 도로명주소 검색 REST API 서버입니다.  
SQLite DB를 기반으로 우편번호, 도로명, 읍면동, 건물명을 키워드로 검색할 수 있습니다.

## Tech Stack

- Java 21
- Spring Boot 3.5.6
- SQLite (sqlite-jdbc)

## DB 설정

DB 파일은 용량 문제로 저장소에 포함되어 있지 않습니다.  
행정안전부에서 제공하는 도로명주소 데이터를 다운로드 후 SQLite DB로 변환하여 사용합니다.

### 1. 데이터 다운로드

[도로명주소 개발자센터](https://business.juso.go.kr/addrlink/main.do) 에서 **전체분 주소 DB** 를 다운로드합니다.

### 2. DB 테이블 구조

```sql
CREATE TABLE road_address (
    zip_code        TEXT,
    sido            TEXT,
    sigungu         TEXT,
    eupmyeondong    TEXT,
    ri              TEXT,
    road_name       TEXT,
    underground     INTEGER,
    building_no     INTEGER,
    building_sub    INTEGER,
    building_name   TEXT,
    is_apartment    INTEGER
);
```

### 3. application.yml 설정

`src/main/resources/application.yml` 에 DB 파일 경로를 입력합니다.

```yaml
address:
  db:
    path: C:/data/address.db  # 실제 DB 파일 경로로 변경
```

## API

### 주소 검색

**POST** `/api/address/search`

#### Request

```json
{
  "keyword": "테헤란로",
  "currentPage": 1,
  "countPerPage": 10
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| keyword | String | 검색 키워드 (최소 2글자) |
| currentPage | int | 페이지 번호 (1부터 시작) |
| countPerPage | int | 페이지당 결과 수 (최대 100) |

#### Response

```json
{
  "success": true,
  "message": "주소 검색 성공",
  "data": [
    {
      "zipCode": "06236",
      "roadAddress": "서울특별시 강남구 테헤란로 152 (역삼동, 강남파이낸스센터)",
      "eupmyeondong": "역삼동",
      "buildingName": "강남파이낸스센터"
    }
  ]
}
```
