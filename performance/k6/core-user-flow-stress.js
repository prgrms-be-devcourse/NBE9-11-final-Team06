import http from "k6/http"
import { check, group, sleep } from "k6"

const BASE_URL = __ENV.BASE_URL || "https://gotoday.site"
const ACCESS_TOKEN = __ENV.ACCESS_TOKEN || ""
const COURSE_ID = __ENV.COURSE_ID || "66"

export const options = {
    scenarios: {
        core_user_flow_stress: {
            executor: "ramping-vus",
            exec: "coreUserFlowStress",
            stages: [
                { duration: "30s", target: 100 },
                { duration: "1m", target: 300 },
                { duration: "1m", target: 500 },
                { duration: "30s", target: 0 },
            ],
            gracefulRampDown: "10s",
        },
    },

    thresholds: {
        checks: ["rate>0.70"],
        http_req_failed: ["rate<0.30"],
        http_req_duration: ["p(95)<10000"],
    },
}

function authParams() {
    return {
        headers: {
            Cookie: `accessToken=${ACCESS_TOKEN}`,
            "Content-Type": "application/json",
        },
        redirects: 0,
    }
}

function publicParams() {
    return {
        redirects: 0,
    }
}

export function coreUserFlowStress() {
    group("핵심 사용자 흐름 API Stress Test", function () {
        const homeResponse = http.get(`${BASE_URL}/`, publicParams())

        check(homeResponse, {
            "GET / status is 200": (res) => res.status === 200,
        })

        const myInfoResponse = http.get(
            `${BASE_URL}/api/members/me`,
            authParams()
        )

        check(myInfoResponse, {
            "GET /api/members/me status is 200": (res) => res.status === 200,
            "GET /api/members/me success is true": (res) => isSuccess(res),
        })

        const categoriesResponse = http.get(
            `${BASE_URL}/api/categories`,
            authParams()
        )

        check(categoriesResponse, {
            "GET /api/categories status is 200": (res) => res.status === 200,
            "GET /api/categories success is true": (res) => isSuccess(res),
        })

        const previewPayload = JSON.stringify({
            startDate: "2026-07-01",
            days: 1,
            startTime: "10:00",
            endTime: "18:00",
            baseArea: "성동구",
            latitude: 37.5446,
            longitude: 127.0557,
            companionType: "FRIEND",
            categoryIds: [1, 2, 3, 4, 5],
        })

        const previewResponse = http.post(
            `${BASE_URL}/api/courses/preview`,
            previewPayload,
            authParams()
        )

        check(previewResponse, {
            "POST /api/courses/preview status is 200": (res) =>
                res.status === 200,
            "POST /api/courses/preview success is true": (res) =>
                isSuccess(res),
        })

        const coursesResponse = http.get(
            `${BASE_URL}/api/courses`,
            authParams()
        )

        check(coursesResponse, {
            "GET /api/courses status is 200": (res) => res.status === 200,
            "GET /api/courses success is true": (res) => isSuccess(res),
        })

        const courseDetailResponse = http.get(
            `${BASE_URL}/api/courses/${COURSE_ID}`,
            authParams()
        )

        check(courseDetailResponse, {
            "GET /api/courses/{courseId} status is 200": (res) =>
                res.status === 200,
            "GET /api/courses/{courseId} success is true": (res) =>
                isSuccess(res),
        })
    })

    sleep(1)
}

function isSuccess(response) {
    try {
        return response.json("success") === true
    } catch {
        return false
    }
}
