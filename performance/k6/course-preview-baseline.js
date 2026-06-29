import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
    scenarios: {
        preview_baseline: {
            executor: "ramping-vus",
            startVUs: 1,
            stages: [
                { duration: "30s", target: 2 },
                { duration: "1m", target: 5 },
                { duration: "1m", target: 5 },
                { duration: "30s", target: 0 },
            ],
            gracefulRampDown: "10s",
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
                name: "course_preview",
            },
            redirects: 0,
        },
    );

    check(response, {
        "응답 상태가 200이다": (res) => res.status === 200,
        "프리뷰 생성이 성공했다": (res) => {
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