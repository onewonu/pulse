[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

# Pulse - 서울 지하철 혼잡도 통계 및 경로 추천 API

> 서울 수도권 지하철 승객 데이터 기반 경로 추천 서비스

- [API 문서](https://api.pulse.it.kr/docs/index.html)

### 주요 기능

- **역 검색**: 다중 노선 지원 및 좌표 정보를 포함한 지하철역 검색
- **경로 추천**: 상세한 혼잡도 분석과 함께 최적의 이동 시간 제공
- **혼잡도 분석**: 과거 승객 데이터 분석 (승차/하차 인원)
- **개인화된 북마크**: 자주 사용하는 경로를 저장하고 관리
- **소셜 인증**: 카카오 및 구글 OAuth 2.0을 통한 안전한 로그인
- **열차 시간표**: 서울교통공사 공식 열차 시간표 데이터 통합

<br/>

## 목차

- [기술 스택](#기술-스택)
- [아키텍처](#아키텍처)
  - [계층 구조](#계층-구조)
  - [도메인 모델](#도메인-모델)
  - [외부 API 통합](#외부-api-통합)
  - [보안 아키텍처](#보안-아키텍처)
- [데이터 적재 프로세스](#데이터-적재-프로세스)
- [검색 프로세스](#검색-프로세스)
- [배포 프로세스](#배포-프로세스)
- [프로젝트 구조](#프로젝트-구조)

<br/>

## ERD

<img width="2613" height="2142" alt="prod" src="https://github.com/user-attachments/assets/a8632673-ab24-4d37-afa9-bf216ca881ed" />

<br/><br/>

## 기술 스택

### 백엔드

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-Hibernate-brightgreen.svg)](https://spring.io/projects/spring-data-jpa)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-JWT-brightgreen.svg)](https://spring.io/projects/spring-security)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![Gradle](https://img.shields.io/badge/Gradle-8.x-02303A.svg)](https://gradle.org/)

### 외부 API 연동

[![서울 열린데이터](https://img.shields.io/badge/서울_열린데이터-API-red.svg)](https://data.seoul.go.kr/)
[![Odsay](https://img.shields.io/badge/Odsay-API-blue.svg)](https://lab.odsay.com/)
[![서울교통공사](https://img.shields.io/badge/서울교통공사-API-0052A4.svg)](https://www.data.go.kr/)
[![Kakao](https://img.shields.io/badge/Kakao-OAuth_2.0-FFCD00.svg)](https://developers.kakao.com/)
[![Google](https://img.shields.io/badge/Google-OAuth_2.0-4285F4.svg)](https://console.cloud.google.com/)

### 배포 및 인프라

[![AWS EC2](https://img.shields.io/badge/AWS-EC2-FF9900.svg)](https://aws.amazon.com/ec2/)
[![AWS CodeDeploy](https://img.shields.io/badge/AWS-CodeDeploy-FF9900.svg)](https://aws.amazon.com/codedeploy/)
[![AWS Secrets Manager](https://img.shields.io/badge/AWS-Secrets_Manager-FF9900.svg)](https://aws.amazon.com/secrets-manager/)
[![GitHub Actions](https://img.shields.io/badge/GitHub-Actions-2088FF.svg)](https://github.com/features/actions)

<br/><br/>

## 아키텍처

### 계층 구조

```
┌─────────────────────────────────────────┐
│         Controller 계층                    ← REST API 엔드포인트
├─────────────────────────────────────────┤
│          Service 계층                      ← 비즈니스 로직
├─────────────────────────────────────────┤
│        Repository 계층                     ← 데이터 접근
├─────────────────────────────────────────┤
│   Database / 외부 API                      ← 데이터 소스
└─────────────────────────────────────────┘
```

**책임 분리:**
- **Controller**: HTTP 요청/응답 처리만 담당
- **Service**: 비즈니스 로직 및 Repository/API 호출 조율
- **Repository**: JPA 인터페이스 및 커스텀 쿼리
- **API Client**: `com.pulse.api` 패키지에 격리된 외부 API 클라이언트

**설계 패턴:**
- **Adapter Pattern**: 외부 API 클라이언트의 일관된 인터페이스
- **DTO Pattern**: 요청/응답 객체 분리로 계층 간 결합도 감소
- **Repository Pattern**: 데이터 접근 로직 추상화
- **Strategy Pattern**: OAuth 제공자별 전략 패턴
- **Validator Pattern**: 외부 API 응답 검증 전담 클래스
- **Mapper Pattern**: 복잡한 데이터 변환 로직 분리

### 도메인 모델

#### 사용자 및 인증

```
User (사용자)
├── id: Long
├── nickname: String
├── providerType: OAuth 제공자 (KAKAO/GOOGLE)
├── providerId: 제공자별 고유 ID
└── role: 역할 (USER/ADMIN)

RefreshToken (리프레시 토큰)
├── id: Long
├── user: User (One-to-One)
├── token: String
├── expiryDate: 만료일 (30일)
└── Unique: user당 1개만 존재
```

#### 지하철 데이터

```
SubwayLine (노선)
├── id: Long
├── lineName: 노선명 (예: "2호선")
├── lineColor: 색상 코드 (예: "#00A84D")
└── stations: List<SubwayStation>

SubwayStation (역)
├── id: Long
├── stationName: 역명
├── line: SubwayLine
├── latitude: 위도
├── longitude: 경도
└── Composite unique: (stationName, lineName)

SubwayPassengerHourly (시간대별 승객 통계)
├── id: Long
├── station: SubwayStation
├── statDate: 통계 날짜
├── hourSlot: 시간대 (0-23)
├── boardingCount: 승차 인원
├── alightingCount: 하차 인원
└── Unique: (station, statDate, hourSlot)

SubwayTrainSchedule (열차 시간표)
├── id: Long
├── station: SubwayStation
├── dayCode: 요일 구분 (0=평일, 1=주말)
├── arrivalTime: 도착 시간
└── departureTime: 출발 시간
```

#### 북마크

```
Bookmark (사용자 저장 경로)
├── id: Long
├── user: User
├── name: 북마크 이름
├── departureStationId: 출발역 ID
├── arrivalStationId: 도착역 ID
├── displayOrder: 표시 순서
└── Index: (user_id, display_order)
```

### 외부 API 통합

#### 1. 서울 열린데이터 광장 API (`SeoulOpenDataClient`)
```
역할: 시간대별 지하철 승객 통계 제공
요청: 페이지네이션 (1000개/페이지)
응답: 24시간 승하차 데이터 (역별/날짜별)
검증: SeoulApiResponseValidator
정규화: StationNameNormalizer로 역명 표준화
```

**데이터 플로우:**
```
Seoul Open Data API
    ↓ (HTTP GET)
SeoulOpenDataClient
    ↓ (검증)
SeoulApiResponseValidator
    ↓ (정규화)
StationNameNormalizer
    ↓ (매핑)
SubwayDataMapper (1개 → 24개 레코드)
    ↓ (저장)
SubwayPassengerHourly 엔티티
```

#### 2. Odsay API (`OdsayClient`)
```
역할: 역 검색 및 경로 계획
필수 파라미터: CID=1000 (서울 수도권 필터링)
Rate Limit: 요청 간 200ms 지연
검증: OdsayApiResponseValidator
정규화: StationNameNormalizer, LineNameNormalizer
```

**역 검색 플로우:**
```
사용자 검색 → Odsay API (CID=1000)
    ↓
역 정보 반환 (좌표 포함)
    ↓
StationNameNormalizer (역명 표준화)
    ↓
클라이언트 응답
```

#### 3. 서울교통공사 API (`SeoulMetroClient`)
```
역할: 공식 열차 시간표 제공
데이터: 역별/요일별 열차 도착/출발 시간
검증: SeoulMetroApiResponseValidator
변환: TimeParser (HHmmss → LocalTime)
```

#### 4. Kakao OAuth API (`KakaoApiClient`)
```
역할: 소셜 로그인
플로우: Authorization Code → Access Token → User Info
엔드포인트:
  - Token: https://kauth.kakao.com/oauth/token
  - UserInfo: https://kapi.kakao.com/v2/user/me
```

#### 5. Google OAuth API (`GoogleApiClient`)
```
역할: 소셜 로그인
플로우: Authorization Code → Access Token → User Info
엔드포인트:
  - Token: https://oauth2.googleapis.com/token
  - UserInfo: https://www.googleapis.com/oauth2/v3/userinfo
```

### 보안 아키텍처

#### JWT 기반 상태 비저장 인증

```
┌─────────────────────────────────────────────────┐
│  클라이언트                                        
└───────────┬─────────────────────────────────────┘
            │ 1. Authorization Code
            ↓
┌─────────────────────────────────────────────────┐
│  AuthController                                  
│  POST /auth/login                                
└───────────┬─────────────────────────────────────┘
            │ 2. OAuth Token Exchange
            ↓
┌─────────────────────────────────────────────────┐
│  AuthService                                    
│  ├─ KakaoApiClient / GoogleApiClient            
│  ├─ User 조회 또는 생성                            
│  ├─ JwtTokenProvider (Access + Refresh)         
│  └─ RefreshToken DB 저장                         
└───────────┬─────────────────────────────────────┘
            │ 3. Access + Refresh Token 반환
            ↓
┌─────────────────────────────────────────────────┐
│  클라이언트                                         
│  Authorization: Bearer {accessToken}             
└───────────┬─────────────────────────────────────┘
            │ 4. API 요청
            ↓
┌─────────────────────────────────────────────────┐
│  JwtAuthenticationFilter                         
│  ├─ Bearer Token 추출                            
│  ├─ JwtTokenProvider.validateToken()           
│  ├─ Token Type 검증 (ACCESS)                     
│  └─ SecurityContext 설정                         
└───────────┬─────────────────────────────────────┘
            │ 5. 인증된 요청
            ↓
┌─────────────────────────────────────────────────┐
│  Controller → Service → Repository               
└─────────────────────────────────────────────────┘
```

**토큰 갱신 플로우:**
```
1. Access Token 만료 → AccessTokenExpiredException
2. 클라이언트 → POST /auth/refresh (refreshToken)
3. AuthService:
   ├─ DB에서 RefreshToken 조회
   ├─ 유효성 검증 (만료, Type)
   ├─ 기존 RefreshToken 삭제
   ├─ 새 Access + Refresh Token 생성
   └─ 새 RefreshToken DB 저장
4. 새 토큰 반환
```

<br/><br/>

## 데이터 적재 프로세스

### 1. 마스터 데이터 적재 (노선 및 역)

`POST /admin/data-load/subway/master`

JSON 파일에서 노선/역 데이터를 읽어 역명·노선명을 정규화한 후 배치 저장(batch_size=100)합니다. `(stationName, lineName)` 복합 Unique 제약으로 중복을 자동 처리합니다.

### 2. 승객 통계 데이터 적재

`POST /admin/data-load/subway/statistics?yearMonth=202401`

서울 열린데이터 광장 API를 페이지네이션(1000개/페이지)으로 호출합니다. 1개 레코드(역별 일별 24시간 데이터)를 24개의 `SubwayPassengerHourly` 레코드로 변환해 저장합니다. N+1 방지를 위해 노선·역 정보를 시작 시 메모리 캐시로 적재합니다.

### 3. 열차 시간표 적재

`POST /admin/data-load/train-schedule/all`

서울교통공사 API에서 전 노선의 역별·요일별 시간표를 조회합니다. 기존 데이터를 전체 삭제 후 신규 데이터를 배치 삽입하는 전체 교체 방식을 사용합니다. 노선 개편 시 수동으로 재적재합니다.

## 검색 프로세스

### 1. 역 검색

`GET /search/station?stationName=강남`

Odsay API(CID=1000, 서울 수도권 필터)로 역을 검색해 이름·좌표·노선 색상을 반환합니다.

### 2. 경로 및 시간 추천

`GET /search/route?departureStationId=222&arrivalStationId=234&searchDate=2024-01-15&startTime=08:00&endTime=09:00`

1. **경로 템플릿 생성**: Odsay API 1회 호출로 최단시간 경로와 역별 시간 오프셋을 산출합니다.
2. **실제 출발역 확정**: 환승역의 경우 사용자 선택 ID와 실제 경로 ID가 다를 수 있으므로, Odsay API가 결정한 첫 번째 역 ID를 기준으로 사용합니다.
3. **출발 시간 조회**: DB에서 해당 역의 요일별 실제 열차 출발 시간 목록을 조회합니다.
4. **혼잡도 계산**: 출발 시간별로 경유 역의 `SubwayPassengerHourly` 데이터를 조회해 평균 혼잡도를 산출합니다.
5. **다양성 기반 추천**: LOW/MEDIUM/HIGH 레벨에서 각 1개씩 선택해 최대 3개의 시간대를 추천합니다.

### 3. 북마크

`(user_id, display_order)` 복합 인덱스로 사용자별 북마크를 정렬된 순서로 조회합니다.

## 배포 프로세스

**GitHub Actions + AWS CodeDeploy** 기반 자동화 파이프라인입니다.

```
GitHub Push
    ↓
GitHub Actions: 빌드(./gradlew clean build) → ZIP 패키징 → S3 업로드
    ↓
AWS CodeDeploy: EC2 Lifecycle Hook 순차 실행
    ├─ ApplicationStop   : 기존 프로세스 종료
    ├─ BeforeInstall     : 기존 JAR 삭제
    ├─ AfterInstall      : 파일 권한 설정
    ├─ ApplicationStart  : Spring Boot 시작 (prod 프로파일, JVM Xms256m/Xmx768m)
    └─ ValidateService   : 헬스체크 성공 시 배포 완료, 실패 시 자동 롤백
```

**환경 변수**: `application-prod.yml` 로드 시 AWS Secrets Manager에서 DB·JWT·외부 API 자격증명을 가져와 `@ConfigurationProperties`에 바인딩합니다.

## 프로젝트 구조

```
pulse/
├── src/main/java/com/pulse/
│   ├── api/                           # 외부 API 클라이언트
│   │   ├── config/
│   │   │   └── RestTemplateConfig.java
│   │   ├── seoulopendata/             # 서울 열린데이터 광장
│   │   │   ├── SeoulOpenDataClient.java
│   │   │   ├── ApiResult.java
│   │   │   ├── dto/
│   │   │   └── validator/             # SeoulApiResponseValidator
│   │   ├── odsay/                     # Odsay (역 검색, 경로)
│   │   │   ├── OdsayClient.java
│   │   │   ├── dto/
│   │   │   └── validator/             # OdsayApiResponseValidator, OdsaySubwayScheduleResponseValidator
│   │   ├── seoulmetro/                # 서울교통공사 (시간표)
│   │   │   ├── SeoulMetroClient.java
│   │   │   ├── dto/
│   │   │   └── validator/             # SeoulMetroApiResponseValidator
│   │   ├── kakao/                     # Kakao OAuth
│   │   │   ├── KakaoApiClient.java
│   │   │   └── dto/
│   │   └── google/                    # Google OAuth
│   │       ├── GoogleApiClient.java
│   │       └── dto/
│   ├── config/                        # Spring 설정 및 @ConfigurationProperties
│   │   ├── SecurityConfig.java        # 보안 설정
│   │   ├── WebConfig.java             # CORS 설정
│   │   ├── AwsSecretsConfig.java      # AWS Secrets Manager 연동
│   │   ├── JwtProperties.java
│   │   ├── KakaoApiProperties.java
│   │   ├── GoogleApiProperties.java
│   │   ├── OdsayApiProperties.java
│   │   ├── SeoulApiProperties.java
│   │   ├── SeoulMetroApiProperties.java
│   │   └── MasterDataProperties.java
│   ├── controller/                    # REST 컨트롤러
│   │   ├── auth/                      # 인증
│   │   ├── web/                       # 검색 (공개)
│   │   ├── bookmark/                  # 북마크
│   │   ├── user/                      # 사용자
│   │   └── admin/                     # 관리자 (데이터 적재)
│   ├── service/                       # 비즈니스 로직
│   │   ├── auth/                      # AuthService, RefreshTokenService, SocialAuthService
│   │   ├── search/                    # StationSearchService, TimeRecommendationService
│   │   ├── dataload/subway/           # SubwayMasterDataLoadService, SubwayStatisticsDataLoadService,
│   │   │                              # TrainScheduleDataLoadService
│   │   ├── bookmark/                  # BookmarkService
│   │   └── user/                      # UserService
│   ├── repository/                    # JPA 리포지토리
│   │   ├── user/
│   │   ├── subway/
│   │   └── bookmark/
│   ├── entity/                        # JPA 엔티티
│   │   ├── user/                      # User, RefreshToken, ProviderType, UserRole
│   │   ├── subway/                    # SubwayLine, SubwayStation,
│   │   │                              # SubwayPassengerHourly, SubwayTrainSchedule
│   │   └── bookmark/                  # Bookmark
│   ├── dto/                           # DTO (요청/응답)
│   │   ├── auth/
│   │   ├── bookmark/
│   │   ├── user/
│   │   ├── masterdata/
│   │   ├── StationSearchResult.java
│   │   ├── TimeRecommendationRequest.java
│   │   ├── TimeRecommendationResult.java
│   │   ├── CongestionLevel.java
│   │   ├── DataLoadResult.java
│   │   └── ErrorResponse.java
│   ├── security/                      # 보안 컴포넌트
│   │   ├── JwtAuthenticationFilter.java  # JWT 필터
│   │   └── JwtTokenProvider.java         # JWT 생성/검증
│   ├── mapper/                        # 데이터 변환 매퍼
│   │   ├── SubwayDataMapper.java          # 1개 → 24개 레코드
│   │   └── TrainScheduleMapper.java
│   ├── util/                          # 유틸리티
│   │   ├── StationNameNormalizer.java     # 역명 정규화
│   │   ├── LineNameNormalizer.java        # 노선명 정규화
│   │   ├── LineDirectionResolver.java     # 노선 방향 결정
│   │   ├── SubwayTimeNormalizer.java      # 지하철 시간 정규화
│   │   ├── DayCodeConverter.java          # 요일 코드 변환
│   │   └── TimeParser.java               # 시간 파싱
│   ├── exception/                     # 커스텀 예외
│   │   ├── ErrorCode.java
│   │   ├── BaseException.java
│   │   ├── GlobalExceptionHandler.java
│   │   ├── auth/
│   │   ├── bookmark/
│   │   ├── config/
│   │   ├── dataload/
│   │   ├── search/
│   │   └── user/
│   └── scheduler/                     # 스케줄러
│       └── RefreshTokenCleanupScheduler.java  # 만료 토큰 정리
├── src/main/resources/
│   ├── application.yml                # 기본 설정
│   ├── application-local.yml          # 로컬 개발
│   ├── application-prod.yml           # 프로덕션
│   └── data/                          # 마스터 데이터
│       ├── lines.json                 # 노선 정보
│       └── stations.json              # 역 정보
├── src/docs/asciidoc/                 # API 문서 소스
│   ├── index.adoc
│   ├── auth.adoc
│   ├── search.adoc
│   ├── bookmark.adoc
│   ├── user.adoc
│   └── admin.adoc
├── src/test/java/com/pulse/
│   ├── controller/                    # Spring REST Docs 컨트롤러 테스트
│   │   ├── auth/
│   │   ├── web/
│   │   ├── bookmark/
│   │   ├── user/
│   │   └── admin/
│   ├── service/                       # 서비스 단위 테스트
│   │   ├── bookmark/
│   │   ├── dataload/subway/
│   │   └── search/
│   └── support/
│       └── RestDocsSupport.java       # RestDocs 공통 설정
├── scripts/                           # 배포 스크립트
│   ├── stop_application.sh
│   ├── before_install.sh
│   ├── after_install.sh
│   ├── start_application.sh
│   └── validate_service.sh
├── .github/workflows/
│   └── deploy.yml                     # GitHub Actions CI/CD
├── appspec.yml                        # AWS CodeDeploy 설정
├── build.gradle                       # Gradle 빌드 설정
└── README.md                          # 이 파일
```
