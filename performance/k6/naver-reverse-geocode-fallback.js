import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
    scenarios: {
        naver_reverse_geocode_fallback: {
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

const LATITUDE = __ENV.LATITUDE || "37.5665";
const LONGITUDE = __ENV.LONGITUDE || "126.9780";

export default function () {
    const response = http.get(
        `${BASE_URL}/api/places/reverse-geocode?latitude=${LATITUDE}&longitude=${LONGITUDE}`,
        {
            headers: {
                Authorization: `Bearer ${ACCESS_TOKEN}`,
            },
            tags: {
                name: "naver_reverse_geocode_fallback",
            },
            redirects: 0,
        },
    );

    check(response, {
        "역지오코딩 응답이 200이다": (res) => res.status === 200,
        "네이버 장애 시 빈 지역 정보를 반환한다": (res) => {
            if (res.status !== 200 || !res.body) {
                return false;
            }

            try {
                const body = res.json();
                return body.success === true
                    && body.data?.areaName === null
                    && body.data?.district === null
                    && body.data?.neighborhood === null;
            } catch (_) {
                return false;
            }
        },
    });

    sleep(1);
}