import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
    scenarios: {
        kakao_fallback: {
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

const previewRequest = {
    courseType: "RECOMMENDATION",
    startDate: "2026-06-28",
    endDate: "2026-06-28",
    baseArea: "종로구",
    companionType: "SOLO",
    restaurantType: "KOREAN",
    startLatitude: 37.5746723659,
    startLongitude: 126.9573421635,
    eventIds: [],
    tourIds: [169],
};

export default function () {
    const response = http.post(
        `${BASE_URL}/api/courses/preview`,
        JSON.stringify(previewRequest),
        {
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${ACCESS_TOKEN}`,
            },
            tags: {
                name: "kakao_fallback_course_preview",
            },
            redirects: 0,
        },
    );

    check(response, {
        "코스 프리뷰 응답이 200이다": (res) => res.status === 200,
        "카카오 장애 중에도 프리뷰 생성이 성공한다": (res) => {
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