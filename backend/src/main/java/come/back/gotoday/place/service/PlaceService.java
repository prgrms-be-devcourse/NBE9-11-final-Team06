package come.back.gotoday.place.service;

import come.back.gotoday.category.entity.Category;
import come.back.gotoday.category.repository.CategoryRepository;
import come.back.gotoday.external.kakao.dto.KakaoPlaceDocument;
import come.back.gotoday.external.naver.NaverLocalSearchClient;
import come.back.gotoday.external.naver.NaverReverseGeocodingClient;
import come.back.gotoday.external.naver.dto.NaverLocalSearchResponse;
import come.back.gotoday.place.dto.PlaceCreateRequest;
import come.back.gotoday.place.dto.PlaceResponse;
import come.back.gotoday.place.dto.PlaceSearchResponse;
import come.back.gotoday.place.dto.ReverseGeocodingResponse;
import come.back.gotoday.place.entity.Place;
import come.back.gotoday.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final CategoryRepository categoryRepository;
    private final NaverLocalSearchClient naverLocalSearchClient;
    private final NaverReverseGeocodingClient naverReverseGeocodingClient;
    private final TransactionTemplate transactionTemplate;

    @Transactional
    public Long createPlace(PlaceCreateRequest request) {
        log.info("장소 생성 처리 시작: name={}, categoryId={}", request.name(), request.categoryId());

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> {
                    log.warn("장소 생성 실패: 존재하지 않는 카테고리입니다. categoryId={}", request.categoryId());
                    return new IllegalArgumentException("카테고리가 존재하지 않습니다.");
                });

        Place place = Place.create(
                category,
                request.name(),
                request.address(),
                request.roadAddress(),
                request.latitude(),
                request.longitude(),
                request.phone(),
                request.placeUrl(),
                request.description(),
                request.source(),
                request.externalId(),
                true // isActive 기본값
        );

        placeRepository.save(place);
        log.info("장소 생성 처리 완료: placeId={}, name={}", place.getId(), place.getName());

        return place.getId();
    }

    @Transactional(readOnly = true)
    public PlaceResponse getPlace(Long placeId) {
        log.info("장소 단건 조회 처리 시작: placeId={}", placeId);

        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> {
                    log.warn("장소 단건 조회 실패: 존재하지 않는 장소입니다. placeId={}", placeId);
                    return new IllegalArgumentException("장소가 존재하지 않습니다.");
                });

        PlaceResponse response = new PlaceResponse(
                place.getId(),
                place.getName(),
                place.getAddress(),
                place.getLatitude(),
                place.getLongitude(),
                place.getPlaceUrl(),
                place.getCategory().getId()
        );

        log.info("장소 단건 조회 처리 완료: placeId={}", placeId);
        return response;
    }

    @Transactional(readOnly = true)
    public List<PlaceResponse> getPlaces() {
        log.info("장소 목록 조회 처리 시작");

        List<PlaceResponse> places = placeRepository.findAll()
                .stream()
                .map(place -> new PlaceResponse(
                        place.getId(),
                        place.getName(),
                        place.getAddress(),
                        place.getLatitude(),
                        place.getLongitude(),
                        place.getPlaceUrl(),
                        place.getCategory().getId()
                ))
                .toList();

        log.info("장소 목록 조회 처리 완료: resultCount={}", places.size());
        return places;
    }

    public List<PlaceSearchResponse> searchPlaces(String query) {
        log.info("장소 검색 처리 시작: query={}", query);

        if (query == null || query.isBlank()) {
            log.warn("장소 검색 실패: 검색어가 비어 있습니다.");
            return List.of();
        }

        NaverLocalSearchResponse response = naverLocalSearchClient.search(query, 5, 1);
        if (response == null || response.items() == null) {
            log.warn("장소 검색 실패: 네이버 지역 검색 API 응답이 비어 있습니다. query={}", query);
            return List.of();
        }

        List<PlaceSearchResponse> places = response.items()
                .stream()
                .map(PlaceSearchResponse::from)
                .toList();

        log.info("장소 검색 처리 완료: query={}, resultCount={}", query, places.size());
        return places;
    }

    public ReverseGeocodingResponse reverseGeocode(double latitude, double longitude) {
        log.info("좌표 기반 지역명 조회 처리 시작: latitude={}, longitude={}", latitude, longitude);

        NaverReverseGeocodingClient.ReverseGeocodingResult result =
                naverReverseGeocodingClient.reverseGeocode(latitude, longitude);

        ReverseGeocodingResponse response = new ReverseGeocodingResponse(
                result.areaName(),
                result.district(),
                result.neighborhood()
        );

        log.info(
                "좌표 기반 지역명 조회 처리 완료: latitude={}, longitude={}, areaName={}",
                latitude,
                longitude,
                response.areaName()
        );

        return response;
    }

    @Transactional
    public void deletePlace(Long placeId) {
        log.info("장소 삭제 처리 시작: placeId={}", placeId);

        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> {
                    log.warn("장소 삭제 실패: 존재하지 않는 장소입니다. placeId={}", placeId);
                    return new IllegalArgumentException("장소가 존재하지 않습니다.");
                });

        placeRepository.delete(place);
        log.info("장소 삭제 처리 완료: placeId={}", placeId);
    }

    public Place getOrCreatePlace(KakaoPlaceDocument doc, Category category) {
        String externalId = extractExternalId(doc.placeUrl());

        if (externalId == null) {
            throw new IllegalArgumentException("externalId is null");
        }

        return placeRepository.findBySourceAndExternalId("KAKAO", externalId)
                .orElseGet(() -> createOrGetPlaceAfterDuplicate(doc, category, externalId));
    }

    private Place createOrGetPlaceAfterDuplicate(
            KakaoPlaceDocument doc,
            Category category,
            String externalId
    ) {
        try {
            Place savedPlace = transactionTemplate.execute(status -> placeRepository
                    .findBySourceAndExternalId("KAKAO", externalId)
                    .orElseGet(() -> createPlace(doc, category, externalId))
            );

            if (savedPlace == null) {
                throw new IllegalStateException("장소 생성 트랜잭션 결과가 없습니다.");
            }

            return savedPlace;
        } catch (DataIntegrityViolationException e) {
            log.info(
                    "동일 카카오 장소가 동시에 생성되어 기존 데이터를 재조회합니다. externalId={}",
                    externalId
            );

            return transactionTemplate.execute(status -> placeRepository
                    .findBySourceAndExternalId("KAKAO", externalId)
                    .orElseThrow(() -> new IllegalStateException(
                            "중복 생성 이후 기존 장소를 찾을 수 없습니다. externalId=" + externalId
                    ))
            );
        }
    }

    private Place createPlace(KakaoPlaceDocument doc, Category category, String externalId) {

        return placeRepository.saveAndFlush(
                Place.create(
                        category,
                        doc.placeName(),
                        doc.addressName(),
                        doc.roadAddressName(),
                        BigDecimal.valueOf(Double.parseDouble(doc.y())),
                        BigDecimal.valueOf(Double.parseDouble(doc.x())),
                        doc.phone(),
                        doc.placeUrl(),
                        null,
                        "KAKAO",
                        externalId,
                        true
                )
        );
    }

    private String extractExternalId(String placeUrl) {
        if (placeUrl == null || !placeUrl.contains("/")) return null;
        return placeUrl.substring(placeUrl.lastIndexOf("/") + 1);
    }



}
