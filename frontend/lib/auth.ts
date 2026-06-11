const ACCESS_TOKEN_KEY = 'accessToken'

export const authStorage = {
  getAccessToken(): string | null {
    if (typeof window === 'undefined') {
      return null
    }

    return localStorage.getItem(ACCESS_TOKEN_KEY)
  },

  setAccessToken(accessToken: string): void {
    if (typeof window === 'undefined') {
      return
    }

    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
  },

  removeAccessToken(): void {
    if (typeof window === 'undefined') {
      return
    }

    localStorage.removeItem(ACCESS_TOKEN_KEY)
  },
}