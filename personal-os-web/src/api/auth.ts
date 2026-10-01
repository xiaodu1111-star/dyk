import { get, post } from './request'
import type { LoginParams, LoginResult, UserInfo } from '@/types'

/**
 * 登录
 * @param data 用户名 / 密码
 * @param silent 为 true 时不弹全局提示，由登录页内联展示错误
 */
export function login(data: LoginParams, silent = false): Promise<LoginResult> {
  return post<LoginResult>('/auth/login', { ...data }, { silent })
}

/** 登出 */
export function logout(): Promise<void> {
  return post<void>('/auth/logout')
}

/** 获取当前用户信息 */
export function getMe(): Promise<UserInfo> {
  return get<UserInfo>('/auth/me')
}
