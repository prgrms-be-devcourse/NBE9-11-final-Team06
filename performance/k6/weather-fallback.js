import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
    scenarios: {
        weather_fallback: {
            executor: "per-vu-iterations",
            vus: 1,
            iterations: 5,
            maxDuration: "2m",
        },
    },

    thresholds: {
        http_req_failed: ["rate<0.05"],
        http_req_duration: ["p(95)<15000"],
    },
};

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const ACCESS_TOKEN = __ENV.ACCESS_TOKEN || "";
const TARGET_DATE = __ENV.TARGET_DATE || "2026-06-29";

const recommendationRequest = {
    title: "기상청 장애 fallback k6 테스트",
    startDate: TARGET_DATE,
    endDate: TARGET_DATE,
    topK: 5,
    area: "종로구",
    categories: ["문화생활"],
    companionType: "SOLO",
    address: "서울특별시 종로구",
    latitude: 37.5746723659,
    longitude: 126.9573421635,
};

export default function () {
    const response = http.post(
        `${BASE_URL}/api/recommendations/candidates`,
        JSON.stringify(recommendationRequest),
        {
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${ACCESS_TOKEN}`,
            },
            tags: {
                name: "recommendation_weather_fallback",
            },
            redirects: 0,
        },
    );

    check(response, {
        "추천 후보 조회 응답이 200이다": (res) => res.status === 200,
        "추천 후보 조회가 성공 형식이다": (res) => {
            if (res.status !== 200 || !res.body) {
                return false;
            }

            try {
                return res.json("success") === true;
            } catch (_) {
                return false;
            }
        },
    });

    sleep(1);
}