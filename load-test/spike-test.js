import http from "k6/http"
import { check, group, sleep } from "k6"
import { SharedArray } from "k6/data"
import exec from "k6/execution"

export const options = {
    scenarios: {
        public_browse_spike: {
            executor: "ramping-vus",
            exec: "publicBrowseFlow",
            stages: [
                { duration: "20s", target: 10 },
                { duration: "10s", target: 150 },
                { duration: "1m", target: 150 },
                { duration: "10s", target: 10 },
                { duration: "30s", target: 0 },
            ],
            gracefulRampDown: "30s",
            gracefulStop: "30s",
        },

        authenticated_read_spike: {
            executor: "ramping-vus",
            exec: "authenticatedReadFlow",
            startTime: "10s",
            stages: [
                { duration: "20s", target: 5 },
                { duration: "10s", target: 70 },
                { duration: "1m", target: 70 },
                { duration: "10s", target: 5 },
                { duration: "30s", target: 0 },
            ],
            gracefulRampDown: "30s",
            gracefulStop: "30s",
        },

        recommendation_create_spike: {
            executor: "ramping-vus",
            exec: "recommendationCreateFlow",
            startTime: "20s",
            stages: [
                { duration: "20s", target: 2 },
                { duration: "10s", target: 15 },
                { duration: "40s", target: 15 },
                { duration: "10s", target: 2 },
                { duration: "30s", target: 0 },
            ],
            gracefulRampDown: "30s",
            gracefulStop: "30s",
        },
    },

    thresholds: {
        http_req_failed: ["rate<0.2"],

        "http_req_duration{scenario:public_browse_spike}": ["p(95)<3000"],
        "http_req_duration{scenario:authenticated_read_spike}": ["p(95)<5000"],
        "http_req_duration{scenario:recommendation_create_spike}": ["p(95)<10000"],
    },
}

const BASE_URL = __ENV.BASE_URL || "http://127.0.0.1:8080"
const RECOMMEND_ENDPOINT =
    __ENV.RECOMMEND_ENDPOINT || "/api/recommendations/courses"

const REQUIRED_ACCOUNT_COUNT = 85

const accounts = new SharedArray("accounts", function () {
    return JSON.parse(open("./accounts.json"))
})

const jsonHeaders = {
    headers: {
        "Content-Type": "application/json",
    },
}

// k6 전역 변수는 VU 단위로 독립적으로 유지된다.
// 각 VU가 최초 1회 로그인하고, 이후 반복에서는 저장한 Cookie 헤더를 재사용한다.
let loggedIn = false
let authCookieHeader = ""

let loginFailureLogCount = 0
let myInfoFailureLogCount = 0
let myPreferenceFailureLogCount = 0
let recommendFailureLogCount = 0

export function setup() {
    if (accounts.length === 0) {
        throw new Error("accounts.json에 테스트 계정이 없습니다.")
    }

    if (accounts.length < REQUIRED_ACCOUNT_COUNT) {
        console.warn(
            `[WARN] accounts.json 계정 수가 부족합니다. 현재 ${accounts.length}개, 권장 최소 ${REQUIRED_ACCOUNT_COUNT}개입니다. ` +
            "계정 수가 부족하면 여러 VU가 동일 계정을 공유하여 실제 운영 환경과 다른 결과가 나올 수 있습니다."
        )
    }
}

export function publicBrowseFlow() {
    group("공개 조회 API 스파이크", function () {
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
    group("인증 사용자 조회 API 스파이크", function () {
        if (!ensureLoggedIn()) {
            return
        }

        const myInfoResponse = http.get(
            `${BASE_URL}/api/members/me`,
            authRequestOptions()
        )

        if (!isSuccess(myInfoResponse) && myInfoFailureLogCount < 5) {
            myInfoFailureLogCount++

            console.log("========== 내 정보 조회 실패 ==========")
            console.log(`status=${myInfoResponse.status}`)
            console.log(`url=${BASE_URL}/api/members/me`)
            console.log(`body=${myInfoResponse.body}`)
            console.log(`vuId=${exec.vu.idInTest}`)
            console.log("===================================")
        }

        check(myInfoResponse, {
            "my info status is 200": (res) => res.status === 200,
            "my info success is true": (res) => isSuccess(res),
        })

        const myPreferenceResponse = http.get(
            `${BASE_URL}/api/preferences/me`,
            authRequestOptions()
        )

        if (!isSuccess(myPreferenceResponse) && myPreferenceFailureLogCount < 5) {
            myPreferenceFailureLogCount++

            console.log("========== 내 선호정보 조회 실패 ==========")
            console.log(`status=${myPreferenceResponse.status}`)
            console.log(`url=${BASE_URL}/api/preferences/me`)
            console.log(`body=${myPreferenceResponse.body}`)
            console.log(`vuId=${exec.vu.idInTest}`)
            console.log("===================================")
        }

        check(myPreferenceResponse, {
            "my preference status is 200": (res) => res.status === 200,
            "my preference success is true": (res) => isSuccess(res),
        })
    })

    sleep(1)
}

export function recommendationCreateFlow() {
    group("추천 코스 생성 API 스파이크", function () {
        if (!ensureLoggedIn()) {
            return
        }

        const startDate = formatDate(addDays(new Date(), 1))
        const endDate = formatDate(addDays(new Date(), 14))

        const requestBody = {
            title: `스파이크테스트 추천 코스 ${exec.vu.idInTest}-${Date.now()}`,
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
            authJsonRequestOptions()
        )

        if (recommendResponse.status !== 201 && recommendFailureLogCount < 5) {
            recommendFailureLogCount++

            console.log("========== 추천 API 실패 ==========")
            console.log(`status=${recommendResponse.status}`)
            console.log(`url=${BASE_URL}${RECOMMEND_ENDPOINT}`)
            console.log(`body=${recommendResponse.body}`)
            console.log(`requestBody=${JSON.stringify(requestBody)}`)
            console.log(`vuId=${exec.vu.idInTest}`)
            console.log("===================================")
        }

        check(recommendResponse, {
            "recommend status is 201": (res) => res.status === 201,
            "recommend success is true": (res) => isSuccess(res),
        })
    })

    // 추천 API는 실제 DB insert가 발생하므로 요청 간격을 조금 둔다.
    sleep(2)
}

function ensureLoggedIn() {
    if (loggedIn && authCookieHeader !== "") {
        return true
    }

    const account = getAccount()

    const loginResponse = http.post(
        `${BASE_URL}/api/auth/login`,
        JSON.stringify({
            email: account.email,
            password: account.password,
        }),
        jsonHeaders
    )

    const cookieHeader = buildAuthCookieHeader(loginResponse)

    if ((loginResponse.status !== 200 || cookieHeader === "") && loginFailureLogCount < 5) {
        loginFailureLogCount++

        console.log("========== 로그인 API 실패 ==========")
        console.log(`status=${loginResponse.status}`)
        console.log(`url=${BASE_URL}/api/auth/login`)
        console.log(`body=${loginResponse.body}`)
        console.log(`email=${account.email}`)
        console.log(`vuId=${exec.vu.idInTest}`)
        console.log(`setCookie=${loginResponse.headers["Set-Cookie"]}`)
        console.log("===================================")
    }

    const success = check(loginResponse, {
        "login status is 200": (res) => res.status === 200,
        "login success is true": (res) => isSuccess(res),
        "accessToken cookie exists": () => cookieHeader.includes("accessToken="),
        "refreshToken cookie exists": () => cookieHeader.includes("refreshToken="),
    })

    if (success) {
        loggedIn = true
        authCookieHeader = cookieHeader
    }

    return loggedIn
}

function getAccount() {
    const vuId = exec.vu.idInTest
    const accountIndex = (vuId - 1) % accounts.length

    return accounts[accountIndex]
}

function authRequestOptions() {
    return {
        headers: {
            Cookie: authCookieHeader,
        },
    }
}

function authJsonRequestOptions() {
    return {
        headers: {
            "Content-Type": "application/json",
            Cookie: authCookieHeader,
        },
    }
}

function buildAuthCookieHeader(response) {
    const setCookieHeader = response.headers["Set-Cookie"]

    if (!setCookieHeader) {
        return ""
    }

    const accessToken = extractCookieValue(setCookieHeader, "accessToken")
    const refreshToken = extractCookieValue(setCookieHeader, "refreshToken")

    if (!accessToken || !refreshToken) {
        return ""
    }

    return `accessToken=${accessToken}; refreshToken=${refreshToken}`
}

function extractCookieValue(setCookieHeader, cookieName) {
    const pattern = new RegExp(`${cookieName}=([^;]+)`)
    const matched = setCookieHeader.match(pattern)

    if (!matched) {
        return ""
    }

    return matched[1]
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