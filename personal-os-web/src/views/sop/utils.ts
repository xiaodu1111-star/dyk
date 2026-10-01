import dayjs from 'dayjs'
import type { SopDetailVO, SopVO } from '@/api/sop'

// SOP 视图层共用的小工具（纯函数，无副作用）

/** SOP 状态 → 中文标签 */
export const STATUS_LABELS: Record<string, string> = {
  draft: '草稿',
  active: '在用',
  deprecated: '归档'
}

/** 常用分类建议（小杜的典型工作流场景） */
export const CATEGORY_SUGGESTIONS: string[] = [
  '需求评审',
  '故障处理',
  '发布上线',
  '周报',
  '复盘',
  '排期'
]

/** 状态中文标签兜底 */
export function statusLabel(status: string | null | undefined): string {
  if (!status) return '草稿'
  return STATUS_LABELS[status] ?? status
}

/**
 * 分类 → 色相（用于卡片分类色点）。
 * 用字符哈希稳定映射到 0-359，配合 `hsl(var(--h) 78% 52%)` 取色，
 * 避免写死具体色值，同时保证同一分类颜色恒定。
 */
export function categoryHue(category: string | null | undefined): number {
  const text = category || '未分类'
  let hash = 0
  for (let i = 0; i < text.length; i += 1) {
    hash = (hash * 31 + text.charCodeAt(i)) % 360
  }
  return hash
}

/** 平均耗时文案 */
export function formatAvgMinutes(minutes: number | null | undefined): string {
  const value = Number(minutes ?? 0)
  if (!Number.isFinite(value) || value <= 0) {
    return '暂无耗时'
  }
  return `均 ${value} 分钟`
}

/** 日期时间格式化（容错空值） */
export function formatDateTime(value: string | null | undefined): string {
  if (!value) return '—'
  const d = dayjs(value)
  return d.isValid() ? d.format('YYYY-MM-DD HH:mm') : String(value)
}

/** 相对时间（列表卡"最近使用"用） */
export function formatRelative(value: string | null | undefined): string {
  if (!value) return '还没用过'
  const d = dayjs(value)
  if (!d.isValid()) return String(value)
  const diffDays = dayjs().startOf('day').diff(d.startOf('day'), 'day')
  if (diffDays <= 0) return '今天用过'
  if (diffDays === 1) return '昨天用过'
  if (diffDays < 7) return `${diffDays} 天前用过`
  return d.format('MM-DD') + ' 用过'
}

/** 秒 → 可读计时文案 */
export function formatDuration(seconds: number): string {
  const total = Math.max(0, Math.floor(seconds))
  const h = Math.floor(total / 3600)
  const m = Math.floor((total % 3600) / 60)
  const s = total % 60
  const pad = (n: number): string => String(n).padStart(2, '0')
  if (h > 0) return `${h}:${pad(m)}:${pad(s)}`
  return `${pad(m)}:${pad(s)}`
}

/** 计划总耗时（各步预估之和，忽略空值） */
export function plannedMinutes(sop: SopDetailVO | null): number {
  if (!sop?.steps?.length) return 0
  return sop.steps.reduce((sum, step) => sum + (Number(step.estimateMin) || 0), 0)
}

/** 列表卡一句话摘要：优先场景，其次目标 */
export function cardSummary(sop: SopVO): string {
  return (sop.triggerScene || sop.goal || '还没有写使用场景').trim()
}
