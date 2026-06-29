import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
    scenarios: {
        naver_search_fallback: {
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
const QUERY = __ENV.QUERY || "성수 카페";

export default function () {
    const response = http.get(
        `${BASE_URL}/api/places/search?query=${encodeURIComponent(QUERY)}`,
        {
            headers: {
                Authorization: `Bearer ${ACCESS_TOKEN}`,
            },
            tags: {
                name: "naver_search_fallback",
            },
            redirects: 0,
        },
    );

    check(response, {
        "장소 검색 응답이 200이다": (res) => res.status === 200,
        "네이버 장애 시 빈 목록을 반환한다": (res) => {
            if (res.status !== 200 || !res.body) {
                return false;
            }

            try {
                const body = res.json();

                if (Array.isArray(body)) {
                    return body.length === 0;
                }

                return Array.isArray(body.data) && body.data.length === 0;
            } catch (_) {
                return false;
            }
        },
    });

    sleep(1);
}