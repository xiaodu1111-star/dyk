/** 后端统一响应体 */
export interface ApiResult<T = unknown> {
  code: number
  msg: string
  data: T
}

/** 登录入参 */
export interface LoginParams {
  username: string
  password: string
}

/** 登录结果 */
export interface LoginResult {
  token: string
  userId: number
  username: string
  nickname: string
  expiresIn: number
}

/** 当前用户信息 */
export interface UserInfo {
  userId: number
  username: string
  nickname: string
  city: string | null
  avatar: string | null
}
