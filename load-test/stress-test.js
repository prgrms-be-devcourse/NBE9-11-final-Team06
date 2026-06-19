import http from "k6/http"
import { check, group, sleep } from "k6"
import { SharedArray } from "k6/data"

export const options = {
    scenarios: {
        public_browse_stress: {
            executor: "ramping-vus",
            exec: "publicBrowseFlow",
            stages: [
                { duration: "30s", target: 20 },
                { duration: "1m", target: 50 },
                { duration: "1m", target: 100 },
                { duration: "1m", target: 100 },
                { duration: "30s", target: 0 },
            ],
            gracefulRampDown: "30s",
            gracefulStop: "30s",
        },

        authenticated_read_stress: {
            executor: "ramping-vus",
            exec: "authenticatedReadFlow",
            startTime: "15s",
            stages: [
                { duration: "30s", target: 10 },
                { duration: "1m", target: 25 },
                { duration: "1m", target: 50 },
                { duration: "1m", target: 50 },
                { duration: "30s", target: 0 },
            ],
            gracefulRampDown: "30s",
            gracefulStop: "30s",
        },

        recommendation_create_stress: {
            executor: "ramping-vus",
            exec: "recommendationCreateFlow",
            startTime: "30s",
            stages: [
                { duration: "30s", target: 2 },
                { duration: "1m", target: 5 },
                { duration: "1m", target: 10 },
                { duration: "1m", target: 10 },
                { duration: "30s", target: 0 },
            ],
            gracefulRampDown: "30s",
            gracefulStop: "30s",
        },
    },

    thresholds: {
        // 스트레스 테스트는 한계 지점 확인이 목적이므로 load test보다 기준을 느슨하게 둔다.
        http_req_failed: ["rate<0.2"],

        "http_req_duration{scenario:public_browse_stress}": ["p(95)<3000"],
        "http_req_duration{scenario:authenticated_read_stress}": ["p(95)<5000"],
        "http_req_duration{scenario:recommendation_create_stress}": ["p(95)<10000"],
    },
}

const BASE_URL = __ENV.BASE_URL || "http://127.0.0.1:8080"
const RECOMMEND_ENDPOINT =
    __ENV.RECOMMEND_ENDPOINT || "/api/recommendations/courses"

const accounts = new SharedArray("accounts", function () {
    return JSON.parse(open("./accounts.json"))
})

const jsonHeaders = {
    headers: {
        "Content-Type": "application/json",
    },
}

export function publicBrowseFlow() {
    group("공개 조회 API 스트레스", function () {
        const healthResponse = http.get(`${BASE_URL}/actuator/health`)

        check(healthResponse, {
            "health status is 200": (res) => res.status === 200,
        })

        const allCategoryResponse = http.get(`${BASE_URL}/api/categories`)

        check(allCategoryResponse, {
            "all categories status is 200": (res) => res.status === 200,
            "all categories success is true": (res) => isSuccess(res),
        })

        const preferenceCategoryResponse = http.get(
            `${BASE_URL}/api/categories?type=PREFERENCE`
        )

        check(preferenceCategoryResponse, {
            "preference categories status is 200": (res) => res.status === 200,
            "preference categories success is true": (res) => isSuccess(res),
            "preference categories has 8 items": (res) => {
                try {
                    const data = res.json("data")
                    return Array.isArray(data) && data.length === 8
                } catch {
                    return false
                }
            },
        })
    })

    sleep(1)
}

export function authenticatedReadFlow() {
    group("인증 사용자 조회 API 스트레스", function () {
        const loggedIn = login()

        if (!loggedIn) {
            return
        }

        const myInfoResponse = http.get(`${BASE_URL}/api/members/me`)

        check(myInfoResponse, {
            "my info status is 200": (res) => res.status === 200,
            "my info success is true": (res) => isSuccess(res),
        })

        const myPreferenceResponse = http.get(`${BASE_URL}/api/preferences/me`)

        check(myPreferenceResponse, {
            "my preference status is 200": (res) => res.status === 200,
            "my preference success is true": (res) => isSuccess(res),
        })
    })

    sleep(1)
}

export function recommendationCreateFlow() {
    group("추천 코스 생성 API 스트레스", function () {
        const loggedIn = login()

        if (!loggedIn) {
            return
        }

        const startDate = formatDate(addDays(new Date(), 1))
        const endDate = formatDate(addDays(new Date(), 14))

        const requestBody = {
            title: `스트레스테스트 추천 코스 ${__VU}-${Date.now()}`,
            area: "마포구",

            // 현재 추천 로직은 EVENT 카테고리명을 기준으로 필터링한다.
            categories: ["콘서트", "클래식", "뮤지컬/오페라"],

            companionType: "FRIEND",
            startDate: startDate,
            endDate: endDate,
            latitude: 37.5501234,
            longitude: 126.9421234,
            topK: 3,
        }

        const recommendResponse = http.post(
            `${BASE_URL}${RECOMMEND_ENDPOINT}`,
            JSON.stringify(requestBody),
            jsonHeaders
        )

        if (recommendResponse.status !== 201 && __ITER < 2) {
            console.log("========== 추천 API 실패 ==========")
            console.log(`status=${recommendResponse.status}`)
            console.log(`url=${BASE_URL}${RECOMMEND_ENDPOINT}`)
            console.log(`body=${recommendResponse.body}`)
            console.log(`requestBody=${JSON.stringify(requestBody)}`)
            console.log("===================================")
        }

        check(recommendResponse, {
            "recommend status is 201": (res) => res.status === 201,
            "recommend success is true": (res) => isSuccess(res),
        })
    })

    // 추천 API는 DB insert가 발생하므로 공개 조회보다 간격을 조금 더 둔다.
    sleep(2)
}

function login() {
    const account = getAccount()

    const loginResponse = http.post(
        `${BASE_URL}/api/auth/login`,
        JSON.stringify({
            email: account.email,
            password: account.password,
        }),
        jsonHeaders
    )

    if (loginResponse.status !== 200 && __ITER < 2) {
        console.log("========== 로그인 API 실패 ==========")
        console.log(`status=${loginResponse.status}`)
        console.log(`url=${BASE_URL}/api/auth/login`)
        console.log(`body=${loginResponse.body}`)
        console.log(`email=${account.email}`)
        console.log("===================================")
    }

    return check(loginResponse, {
        "login status is 200": (res) => res.status === 200,
        "login success is true": (res) => isSuccess(res),
    })
}

function getAccount() {
    return accounts[(__VU - 1) % accounts.length]
}

function isSuccess(response) {
    try {
        return response.json("success") === true
    } catch {
        return false
    }
}

function addDays(date, days) {
    const copiedDate = new Date(date)
    copiedDate.setDate(copiedDate.getDate() + days)
    return copiedDate
}

function formatDate(date) {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, "0")
    const day = String(date.getDate()).padStart(2, "0")

    return `${year}-${month}-${day}`
}