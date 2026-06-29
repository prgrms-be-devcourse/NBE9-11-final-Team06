import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
    scenarios: {
        crowd_fallback: {
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
const CROWD_AREA_NAME = __ENV.CROWD_AREA_NAME || "회기역";

export default function () {
    const response = http.get(
        `${BASE_URL}/api/crowds?areaName=${encodeURIComponent(CROWD_AREA_NAME)}`,
        {
            headers: {
                Authorization: `Bearer ${ACCESS_TOKEN}`,
            },
            tags: {
                name: "crowd_fallback",
            },
            redirects: 0,
        },
    );

    check(response, {
        "혼잡도 조회 응답이 200이다": (res) => res.status === 200,
        "혼잡도 fallback 데이터가 반환된다": (res) => {
            if (res.status !== 200 || !res.body) {
                return false;
            }

            try {
                return res.json("areaName") === CROWD_AREA_NAME
                    && res.json("congestionLevel") !== "";
            } catch (_) {
                return false;
            }
        },
    });

    sleep(1);
}