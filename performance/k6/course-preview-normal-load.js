

import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
    scenarios: {
        course_preview_normal_load: {
            executor: "ramping-vus",
            startVUs: 1,
            stages: [
                { duration: "30s", target: 20 },
                { duration: "30s", target: 50 },
                { duration: "30s", target: 100 },
                { duration: "60s", target: 100 },
                { duration: "30s", target: 0 },
            ],
            gracefulRampDown: "20s",
        },
    },
    thresholds: {
        http_req_failed: ["rate<0.05"],
        http_req_duration: ["p(95)<1000"],
    },
};

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const ACCESS_TOKEN = __ENV.ACCESS_TOKEN || "";
const START_DATE = __ENV.START_DATE || "2026-07-05";
const END_DATE = __ENV.END_DATE || START_DATE;
const TOUR_ID = Number(__ENV.TOUR_ID || "169");

const requestBody = {
    courseType: "RECOMMENDATION",
    startDate: START_DATE,
    endDate: END_DATE,
    baseArea: "종로구",
    companionType: "SOLO",
    restaurantType: "KOREAN",
    startLatitude: 37.5746723659,
    startLongitude: 126.9573421635,
    eventIds: [],
    tourIds: [TOUR_ID],
};

export default function () {
    const response = http.post(
        `${BASE_URL}/api/courses/preview`,
        JSON.stringify(requestBody),
        {
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${ACCESS_TOKEN}`,
            },
            tags: {
                name: "course_preview_normal_load",
            },
        },
    );

    check(response, {
        "코스 프리뷰 응답이 200이다": (res) => res.status === 200,
        "코스 프리뷰 생성이 성공한다": (res) => {
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