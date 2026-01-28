[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

# Pulse - 서울 지하철 혼잡도 통계 및 경로 추천 API

> 서울 수도권 지하철 승객 데이터 기반 경로 추천 서비스

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
- [API 엔드포인트](#api-엔드포인트)
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

Pulse는 엄격한 계층형 아키텍처 패턴을 따릅니다:

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

Pulse는 5개의 외부 API를 Adapter 패턴으로 통합합니다:

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

**API**: `POST /admin/data-load/subway/master`

**프로세스:**
```
1. JSON 파일 읽기
   ├─ src/main/resources/data/lines.json
   └─ src/main/resources/data/stations.json

2. 데이터 검증
   ├─ 필수 필드 확인 (lineName, stationName, coordinates)
   └─ 데이터 형식 검증

3. SubwayLine 엔티티 생성/업데이트
   ├─ 노선명으로 중복 확인
   ├─ LineNameNormalizer로 표준화
   └─ 배치 저장 (batch_size=500)

4. SubwayStation 엔티티 생성/업데이트
   ├─ (stationName, lineName) 복합키로 중복 확인
   ├─ StationNameNormalizer로 표준화
   ├─ 좌표 데이터 매핑
   └─ 배치 저장

5. 메모리 캐시 구성
   └─ HashMap<stationName, SubwayStation> (이후 승객 데이터 적재 시 사용)
```

**최적화 전략:**
- Hibernate 배치 삽입 (batch_size=500)
- 중복 데이터는 Unique 제약조건으로 자동 처리
- 메모리 내 HashMap 캐싱으로 조회 성능 향상

### 2. 승객 통계 데이터 적재

**API**: `POST /admin/data-load/subway/statistics?yearMonth=202401`

**프로세스:**
```
1. 서울 열린데이터 광장 API 호출
   ├─ 파라미터: yearMonth (예: 202401)
   ├─ 페이지네이션: 1000개/페이지
   └─ 응답: 일별/역별 24시간 승하차 데이터

2. 응답 검증 (SeoulApiResponseValidator)
   ├─ 성공 코드 확인 (INFO-000)
   ├─ 데이터 존재 확인
   └─ 필수 필드 검증

3. 데이터 정규화
   ├─ StationNameNormalizer: 역명 표준화
   │   예: "강남역" → "강남", "신논현 역" → "신논현"
   └─ LineNameNormalizer: 노선명 표준화
       예: "2 호선" → "2호선", "신분당선 " → "신분당선"

4. SubwayDataMapper: 1개 레코드 → 24개 레코드 변환
   ┌──────────────────────────────────────┐
   │ API 응답 (1개 레코드)                    
   │ - statDate: 2024-01-15               
   │ - station: 강남                        
   │ - 0500승차: 120                        
   │ - 0500하차: 80                         
   │ - ... (24시간 데이터)                   
   └──────────────────────────────────────┘
                    ↓
   ┌──────────────────────────────────────┐
   │ SubwayPassengerHourly (24개 레코드)    
   │ 1. statDate=2024-01-15, hour=5,       
   │    boarding=120, alighting=80         
   │ 2. statDate=2024-01-15, hour=6, ...   
   │ ... (시간대별 24개)                     
   └──────────────────────────────────────┘

5. 역 매핑 및 저장
   ├─ 메모리 캐시에서 SubwayStation 조회
   ├─ Unique 제약조건: (station, statDate, hourSlot)
   │   → 중복 시 자동 스킵 또는 업데이트
   └─ 배치 삽입 (batch_size=500)

6. 진행 상황 로깅
   └─ 페이지별 진행률, 총 적재 건수 출력
```

**성능 최적화:**
- 배치 삽입으로 DB I/O 최소화
- 메모리 캐시로 역 조회 최적화 (O(1))
- Unique 제약조건으로 중복 처리 자동화

### 3. 열차 시간표 적재

**API**: `POST /admin/data-load/train-schedule/all`

**프로세스:**
```
1. 서울교통공사 API 호출
   ├─ 전체 노선 순회
   └─ 역별/요일별 시간표 조회

2. 응답 검증 (SeoulMetroApiResponseValidator)
   ├─ 성공 코드 확인 (resultCode=0)
   └─ 데이터 유효성 검증

3. 시간 파싱 (TimeParser)
   ├─ 입력: "HHmmss" 형식 (예: "053000")
   └─ 출력: LocalTime (예: 05:30:00)

4. TrainScheduleMapper: API 응답 → 엔티티 변환
   ┌──────────────────────────────────────┐
   │ API 응답                               
   │ - stationName: 강남                    
   │ - lineName: 2호선                      
   │ - dayCode: 0 (평일)                    
   │ - arrivalTime: 053000                  
   │ - departureTime: 053030               
   └──────────────────────────────────────┘
                    ↓
   ┌──────────────────────────────────────┐
   │ SubwayTrainSchedule 엔티티             
   │ - station: SubwayStation               
   │ - dayCode: 0                           
   │ - arrivalTime: 05:30:00                
   │ - departureTime: 05:30:30              
   └──────────────────────────────────────┘

5. 요일 코드 변환 (DayCodeConverter)
   ├─ 평일: 0
   └─ 주말: 1

6. 배치 저장
   ├─ 기존 데이터 삭제 (전체 교체 방식)
   └─ 신규 데이터 삽입 (batch_size=500)
```

**데이터 신선도:**
- 열차 시간표는 노선 개편 시에만 변경
- 관리자가 변경 시점에 수동으로 재적재

## 검색 프로세스

### 1. 역 검색 프로세스

**API**: `GET /search/station?stationName=강남`

**플로우:**
```
1. 사용자 요청
   └─ 검색어: "강남" (최소 2자)

2. StationSearchController
   ├─ @Validated로 입력 검증
   └─ StationSearchService 호출

3. StationSearchService
   └─ OdsayClient.searchStation()

4. Odsay API 호출
   ┌──────────────────────────────────────┐
   │ GET https://api.odsay.com/v1/api/...  
   │ Parameters:                            
   │ - stationName: 강남                    
   │ - CID: 1000 (서울 수도권 필터)          
   └──────────────────────────────────────┘

5. 응답 검증 (OdsayApiResponseValidator)
   ├─ resultCode == 0 확인
   └─ station 배열 존재 확인

6. 응답 변환
   ┌──────────────────────────────────────┐
   │ Odsay API 응답                         
   │ - stationName: 강남                    
   │ - stationID: 222                      
   │ - x: 127.02761 (경도)                  
   │ - y: 37.49794 (위도)                   
   │ - laneName: 2호선                      
   └──────────────────────────────────────┘
                    ↓
   ┌──────────────────────────────────────┐
   │ StationSearchResult                    
   │ - totalCount: 1                        
   │ - stations: [                          
   │     {                                  
   │       stationName: 강남                
   │       stationID: 222                  
   │       x: 127.02761                     
   │       y: 37.49794                      
   │       laneName: 2호선                  
   │       lineColor: #00A84D               
   │     }                                  
   │   ]                                    
   └──────────────────────────────────────┘

7. 클라이언트 응답
```

**특징:**
- **CID=1000 필터**: 서울 수도권만 검색 (타 지역 제외)
- **좌표 제공**: 지도 통합 가능
- **노선 색상**: 시각화에 활용

### 2. 경로 및 시간 추천 프로세스

**API**: `GET /search/route?departureStationId=222&arrivalStationId=234&searchDate=2024-01-15&startTime=08:00&endTime=09:00`

```
┌─────────────────────────────────────────────────────────────────┐
│ 1단계: 요청 검증                                                   
├─────────────────────────────────────────────────────────────────┤
│ StationSearchController                                          
│ ├─ departureStationId: 출발역 ID (필수)                          
│ ├─ arrivalStationId: 도착역 ID (필수)                            
│ ├─ searchDate: 검색 날짜 (필수, ISO 형식)                         
│ ├─ startTime: 시작 시간 (필수, HH:mm)                            
│ └─ endTime: 종료 시간 (필수, HH:mm)                               
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 2단계: 요일 코드 변환                                              
├─────────────────────────────────────────────────────────────────┤
│ DayCodeConverter.convert(searchDate)                             
│ ├─ 월~금: dayCode = 0 (평일)                                     
│ └─ 토~일: dayCode = 1 (주말)                                     
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 3단계: Odsay 경로 검색 (Rate Limit 고려)                          
├─────────────────────────────────────────────────────────────────┤
│ TimeRecommendationService.recommendTimes()                       
│                                                                   
│ For time in [startTime, endTime] (30분 간격):                    
│   ├─ OdsayClient.searchRoute(departure, arrival, date, time)    
│   ├─ Thread.sleep(200ms) ← Rate Limit 방지                      
│   └─ 경로 정보 수집:                                              
│       ├─ departureTime: 출발 시간                                
│       ├─ arrivalTime: 도착 시간                                  
│       ├─ totalTime: 총 소요 시간 (분)                            
│       ├─ transferCount: 환승 횟수                                
│       └─ path: 경유 역 목록                                       
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 4단계: 경유 역별 승객 통계 조회                                     
├─────────────────────────────────────────────────────────────────┤
│ For each route:                                                  
│   For each station in route.path:                                
│     ├─ StationNameNormalizer.normalize(stationName)             
│     ├─ SubwayStationRepository.findByName(normalized)           
│     └─ SubwayPassengerHourlyRepository.findByStationAndDateTime
│         ├─ statDate: searchDate                                 
│         └─ hourSlot: station 도착 시간의 시간대                   
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 5단계: 혼잡도 점수 계산                                            
├─────────────────────────────────────────────────────────────────┤
│ congestionScore = Σ (boardingCount + alightingCount)             
│                                                                   
│ 경유하는 모든 역의:                                                
│ - 승차 인원 (boardingCount)                                       
│ - 하차 인원 (alightingCount)                                     
│ 을 합산하여 경로의 총 혼잡도 산출                                    
│                                                                   
│ 혼잡도 레벨 분류:                                                  
│ ├─ LOW: score < 10,000 (쾌적)                                    
│ ├─ MEDIUM: 10,000 ≤ score < 30,000 (보통)                       
│ └─ HIGH: score ≥ 30,000 (혼잡)                                   
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 6단계: 경로 정렬 및 반환                                           
├─────────────────────────────────────────────────────────────────┤
│ ├─ 혼잡도 점수 오름차순 정렬 (낮은 혼잡도 우선)                      
│ ├─ 상위 10개 경로 선택                                             
│ └─ TimeRecommendationResult 생성:                                
│     ├─ departureStationName: 출발역명                            
│     ├─ arrivalStationName: 도착역명                              
│     ├─ travelDate: 검색 날짜                                      
│     ├─ dayType: "weekday" 또는 "weekend"                         
│     └─ recommendations: [                                        
│           {                                                      
│             departureTime: 출발 시간                              
│             arrivalTime: 도착 시간                                
│             totalTime: 소요 시간                                  
│             transferCount: 환승 횟수                              
│             congestionScore: 혼잡도 점수                          
│             congestionLevel: LOW/MEDIUM/HIGH                     
│             stationCongestions: [                                
│               {                                                  
│                 stationName: 역명                                
│                 lineName: 노선명                                 
│                 arrivalTime: 도착 시간                            
│                 boardingCount: 승차 인원                          
│                 alightingCount: 하차 인원                         
│                 totalPassengers: 총 승객                         
│               }, ...                                             
│             ]                                                    
│           }, ...                                                 
│         ]                                                        
└─────────────────────────────────────────────────────────────────┘
```

**핵심 알고리즘:**
1. **Rate Limiting**: Odsay API 호출 간 200ms 지연으로 과부하 방지
2. **혼잡도 계산**: 경유 역의 승하차 인원 합산으로 경로 혼잡도 산출
3. **최적화**: 혼잡도 낮은 순으로 정렬하여 최적 경로 추천
4. **평일/주말 구분**: 요일별로 다른 승객 패턴 반영

### 3. 북마크 검색 최적화

**인덱스 전략:**
```sql
CREATE INDEX idx_bookmark_user_order
ON bookmark (user_id, display_order);
```

**조회 최적화:**
- `(user_id, display_order)` 복합 인덱스로 O(log n) 조회
- Display order로 정렬된 상태로 반환
- 사용자별 북마크 격리

## 배포 프로세스

Pulse는 **GitHub Actions + AWS CodeDeploy**를 통한 자동화된 배포 파이프라인을 사용합니다.

### 배포 아키텍처

```
┌──────────────────────────────────────────────────────────────────┐
│                        GitHub Repository                          
│                     (코드 저장소 + CI/CD)                           
└────────────────────────┬─────────────────────────────────────────┘
                         │
                         │ Push / Manual Trigger
                         ↓
┌──────────────────────────────────────────────────────────────────┐
│                      GitHub Actions Workflow                      
│                    (.github/workflows/deploy.yml)                 
│                                                                    
│  ┌──────────────────────────────────────────────────────────┐   
│  │ 1. Build Stage                                            
│  │    ├─ Checkout code                                       
│  │    ├─ Setup Java 21 (Amazon Corretto)                    
│  │    ├─ Grant execute permission: chmod +x gradlew         
│  │    └─ Build: ./gradlew clean build                       
│  └──────────────────────────────────────────────────────────┘   
│                         ↓                                          
│  ┌──────────────────────────────────────────────────────────┐   
│  │ 2. Package Stage                                          
│  │    ├─ Create deploy/ directory                           
│  │    ├─ Copy JAR: pulse-0.0.1-SNAPSHOT.jar                 
│  │    ├─ Copy appspec.yml                                   
│  │    ├─ Copy scripts/ directory                            
│  │    └─ Create ZIP: pulse-deployment-{run_number}.zip     
│  └──────────────────────────────────────────────────────────┘   
│                         ↓                                          
│  ┌──────────────────────────────────────────────────────────┐   
│  │ 3. Upload Stage                                           
│  │    ├─ Configure AWS credentials (OIDC)                   
│  │    └─ Upload ZIP to S3 bucket                            
│  └──────────────────────────────────────────────────────────┘   
│                         ↓                                          
│  ┌──────────────────────────────────────────────────────────┐   
│  │ 4. Deploy Trigger                                        
│  │    └─ Create CodeDeploy deployment                       
│  └──────────────────────────────────────────────────────────┘   
└────────────────────────┬─────────────────────────────────────────┘
                         │
                         ↓
┌──────────────────────────────────────────────────────────────────┐
│                         AWS S3 Bucket                             
│                      (배포 아티팩트 임시 저장소)                              
└────────────────────────┬─────────────────────────────────────────┘
                         │
                         │ CodeDeploy pulls artifact
                         ↓
┌──────────────────────────────────────────────────────────────────┐
│                       AWS CodeDeploy                              
│                      (배포 오케스트레이션)                              
└────────────────────────┬─────────────────────────────────────────┘
                         │
                         │ Execute lifecycle hooks
                         ↓
┌──────────────────────────────────────────────────────────────────┐
│                         AWS EC2 Instance                          
│                         (애플리케이션 서버)                             
│                                                                    
│  ┌──────────────────────────────────────────────────────────┐   
│  │ Lifecycle Hook 1: ApplicationStop                         
│  │ Script: scripts/stop_application.sh                       
│  │ ┌────────────────────────────────────────────────────┐   
│  │ │ #!/bin/bash                                         
│  │ │ PID_FILE="/home/ec2-user/pulse/application.pid"    
│  │ │ if [ -f "$PID_FILE" ]; then                        
│  │ │   PID=$(cat "$PID_FILE")                           
│  │ │   if ps -p $PID > /dev/null; then                  
│  │ │     kill -15 $PID  # SIGTERM (graceful)            
│  │ │     sleep 10                                       
│  │ │     if ps -p $PID > /dev/null; then                
│  │ │       kill -9 $PID  # SIGKILL (force)              
│  │ │     fi                                             
│  │ │   fi                                               
│  │ │ fi                                                 
│  │ └────────────────────────────────────────────────────┘   
│  └──────────────────────────────────────────────────────────┘   
│                         ↓                                          
│  ┌──────────────────────────────────────────────────────────┐   
│  │ Lifecycle Hook 2: BeforeInstall                           
│  │ Script: scripts/before_install.sh                         
│  │ ┌────────────────────────────────────────────────────┐   
│  │ │ #!/bin/bash                                         
│  │ │ APP_DIR="/home/ec2-user/pulse"                     
│  │ │ rm -rf $APP_DIR/*.jar  # 기존 JAR 삭제             
│  │ │ mkdir -p $APP_DIR/logs                             
│  │ └────────────────────────────────────────────────────┘   
│  └──────────────────────────────────────────────────────────┘   
│                         ↓                                          
│  ┌──────────────────────────────────────────────────────────┐   
│  │ Lifecycle Hook 3: AfterInstall                            
│  │ Script: scripts/after_install.sh                          
│  │ ┌────────────────────────────────────────────────────┐   
│  │ │ #!/bin/bash                                         
│  │ │ chown -R ec2-user:ec2-user /home/ec2-user/pulse   
│  │ │ chmod 755 /home/ec2-user/pulse/*.jar              
│  │ └────────────────────────────────────────────────────┘   
│  └──────────────────────────────────────────────────────────┘   
│                         ↓                                          
│  ┌──────────────────────────────────────────────────────────┐   
│  │ Lifecycle Hook 4: ApplicationStart                        
│  │ Script: scripts/start_application.sh                      
│  │ ┌────────────────────────────────────────────────────┐   
│  │ │ #!/bin/bash                                         
│  │ │ APP_DIR="/home/ec2-user/pulse"                     
│  │ │ JAR_FILE="$APP_DIR/pulse-0.0.1-SNAPSHOT.jar"      
│  │ │ LOG_FILE="$APP_DIR/logs/application.log"          
│  │ │ JAVA_OPTS="-Xms256m -Xmx768m \                    
│  │ │            -Dspring.profiles.active=prod"         
│  │ │                                                   
│  │ │ nohup java $JAVA_OPTS -jar "$JAR_FILE" \          
│  │ │       > "$LOG_FILE" 2>&1 &                        
│  │ │ echo $! > "$APP_DIR/application.pid"              
│  │ └────────────────────────────────────────────────────┘   
│  └──────────────────────────────────────────────────────────┘ 
│                         ↓                                      
│  ┌──────────────────────────────────────────────────────────┐   
│  │ Lifecycle Hook 5: ValidateService                         
│  │ Script: scripts/validate_service.sh                       
│  │ ┌────────────────────────────────────────────────────┐   
│  │ │ #!/bin/bash                                         
│  │ │ for i in {1..30}; do                                
│  │ │   HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}"
│  │ │                http://localhost:8080/actuator/health)
│  │ │   if [ "$HTTP_CODE" -eq 200 ]; then                
│  │ │     echo "Health check passed"                     
│  │ │     exit 0                                         
│  │ │   fi                                               
│  │ │   sleep 2                                          
│  │ │ done                                               
│  │ │ echo "Health check failed"                         
│  │ │ exit 1                                             
│  │ └────────────────────────────────────────────────────┘  
│  └──────────────────────────────────────────────────────────┘ 
└──────────────────────────────────────────────────────────────────┘
                         ↓
┌──────────────────────────────────────────────────────────────────┐
│                      Deployment Complete                          
│                  애플리케이션 정상 실행 중                           
└──────────────────────────────────────────────────────────────────┘
```

### 프로덕션 설정

**환경 변수 로딩 (AWS Secrets Manager):**
```
Spring Boot 시작
    ↓
application-prod.yml 로드
    ↓
AWS Secrets Manager 연동
    ↓
Secrets 조회:
    ├─ pulse/database (DB 자격증명)
    ├─ pulse/jwt (JWT secret)
    ├─ pulse/seoul-api (Seoul API key)
    ├─ pulse/odsay-api (Odsay API key)
    ├─ pulse/metro-api (Metro API key)
    ├─ pulse/kakao-oauth (Kakao credentials)
    └─ pulse/google-oauth (Google credentials)
    ↓
@ConfigurationProperties 바인딩
    ↓
애플리케이션 초기화 완료
```

### 롤백 전략

**자동 롤백:**
- `ValidateService` 헬스 체크 실패 시 자동 롤백
- 이전 배포 버전으로 복원

## API 엔드포인트

### 인증 API

| Method | Endpoint | 인증 | 설명 |
|--------|----------|------|------|
| POST | `/auth/login` | 불필요 | OAuth 소셜 로그인 (Kakao/Google) |
| POST | `/auth/refresh` | 불필요 | Access Token 갱신 |
| POST | `/auth/logout` | 필요 | 로그아웃 (Refresh Token 삭제) |

### 검색 API

| Method | Endpoint | 인증 | 설명 |
|--------|----------|------|------|
| GET | `/search/station` | 불필요 | 역 검색 (이름 기반) |
| GET | `/search/route` | 불필요 | 경로 및 시간 추천 (혼잡도 포함) |

**주요 파라미터:**
- `stationName`: 역 이름 (최소 2자)
- `departureStationId`: 출발역 ID
- `arrivalStationId`: 도착역 ID
- `searchDate`: 검색 날짜 (YYYY-MM-DD)
- `startTime`: 시작 시간 (HH:mm)
- `endTime`: 종료 시간 (HH:mm)

### 북마크 API

| Method | Endpoint | 인증 | 설명 |
|--------|----------|------|------|
| POST | `/bookmarks` | 필요 | 북마크 생성 |
| GET | `/bookmarks` | 필요 | 모든 북마크 조회 |
| GET | `/bookmarks/{id}` | 필요 | 특정 북마크 조회 |
| PATCH | `/bookmarks/{id}` | 필요 | 북마크 수정 |
| PUT | `/bookmarks/reorder` | 필요 | 북마크 순서 변경 |
| DELETE | `/bookmarks/{id}` | 필요 | 북마크 삭제 |

### 사용자 API

| Method | Endpoint | 인증 | 설명 |
|--------|----------|------|------|
| GET | `/user/me` | 필요 | 현재 사용자 정보 조회 |

### 관리자 API

| Method | Endpoint | 인증 | 설명 |
|--------|----------|------|------|
| POST | `/admin/data-load/subway/master` | Admin | 노선/역 마스터 데이터 적재 |
| POST | `/admin/data-load/subway/statistics` | Admin | 승객 통계 데이터 적재 |
| POST | `/admin/data-load/train-schedule/all` | Admin | 열차 시간표 적재 |

**상세 API 문서:**
- 요청/응답 예시는 Postman Collection 참조
- Swagger UI: `/swagger-ui.html` (개발 환경)

## 프로젝트 구조

```
pulse/
├── src/main/java/com/pulse/
│   ├── api/                           # 외부 API 클라이언트
│   │   ├── seoulopendata/             # 서울 열린데이터 광장
│   │   │   ├── SeoulOpenDataClient.java
│   │   │   ├── dto/                   # 요청/응답 DTO
│   │   │   └── validator/             # SeoulApiResponseValidator
│   │   ├── odsay/                     # Odsay (역 검색, 경로)
│   │   │   ├── OdsayClient.java
│   │   │   ├── dto/
│   │   │   └── validator/             # OdsayApiResponseValidator
│   │   ├── seoulmetro/                # 서울교통공사 (시간표)
│   │   │   ├── SeoulMetroClient.java
│   │   │   └── validator/
│   │   ├── kakao/                     # Kakao OAuth
│   │   │   ├── KakaoApiClient.java
│   │   │   └── dto/
│   │   └── google/                    # Google OAuth
│   │       ├── GoogleApiClient.java
│   │       └── dto/
│   ├── config/                        # Spring 설정
│   │   ├── SecurityConfig.java        # 보안 설정
│   │   ├── JpaConfig.java             # JPA 설정
│   │   └── properties/                # @ConfigurationProperties
│   ├── controller/                    # REST 컨트롤러
│   │   ├── auth/                      # 인증
│   │   ├── web/                       # 검색 (공개)
│   │   ├── bookmark/                  # 북마크
│   │   ├── user/                      # 사용자
│   │   └── admin/                     # 관리자 (데이터 적재)
│   ├── service/                       # 비즈니스 로직
│   │   ├── auth/                      # 인증 서비스
│   │   ├── search/                    # 검색 서비스
│   │   │   ├── StationSearchService.java
│   │   │   └── TimeRecommendationService.java
│   │   ├── dataload/                  # 데이터 적재 서비스
│   │   │   ├── SubwayDataLoadService.java
│   │   │   └── TrainScheduleLoadService.java
│   │   ├── bookmark/                  # 북마크 서비스
│   │   └── user/                      # 사용자 서비스
│   ├── repository/                    # JPA 리포지토리
│   │   ├── user/
│   │   ├── subway/
│   │   └── bookmark/
│   ├── entity/                        # JPA 엔티티
│   │   ├── user/                      # User, RefreshToken
│   │   ├── subway/                    # SubwayLine, SubwayStation,
│   │   │                              # SubwayPassengerHourly,
│   │   │                              # SubwayTrainSchedule
│   │   └── bookmark/                  # Bookmark
│   ├── dto/                           # DTO (요청/응답)
│   │   ├── auth/
│   │   ├── bookmark/
│   │   └── user/
│   ├── security/                      # 보안 컴포넌트
│   │   ├── JwtAuthenticationFilter.java  # JWT 필터
│   │   ├── JwtTokenProvider.java         # JWT 생성/검증
│   │   └── CustomUserDetailsService.java
│   ├── mapper/                        # 데이터 변환 매퍼
│   │   ├── SubwayDataMapper.java          # 1개 → 24개 레코드
│   │   └── TrainScheduleMapper.java
│   ├── util/                          # 유틸리티
│   │   ├── StationNameNormalizer.java     # 역명 정규화
│   │   ├── LineNameNormalizer.java        # 노선명 정규화
│   │   ├── DayCodeConverter.java          # 요일 코드 변환
│   │   └── TimeParser.java                # 시간 파싱
│   ├── exception/                     # 커스텀 예외
│   │   ├── ErrorCode.java
│   │   ├── auth/                          # 인증 예외
│   │   └── GlobalExceptionHandler.java
│   └── scheduler/                     # 스케줄러
│       └── RefreshTokenCleanupScheduler.java  # 토큰 정리
├── src/main/resources/
│   ├── application.yml                # 기본 설정
│   ├── application-local.yml          # 로컬 개발
│   ├── application-prod.yml           # 프로덕션
│   └── data/                          # 마스터 데이터
│       ├── lines.json                 # 노선 정보
│       └── stations.json              # 역 정보
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
