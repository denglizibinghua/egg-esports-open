// 时间格式化工具集
// 提取自各页面中重复定义的 formatTime 局部方法，统一维护输出格式

/**
 * 时间格式化：MM-DD HH:mm
 * 与原各页面局部 formatTime 输出完全一致（月-日 时:分）
 * @param dateStr - 时间字符串/时间戳/Date 对象
 * @returns 格式化后的字符串，无法解析时原样返回
 */
export function formatTime(dateStr: string | number | Date): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  // 无法解析的入参原样返回，避免页面显示 Invalid Date
  if (isNaN(d.getTime())) return String(dateStr)
  const pad = (n: number): string => String(n).padStart(2, '0')
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/**
 * 队标 URL 防御：http → https，避免 https 页面 Mixed Content 被拦截
 * null / 空 / generic 占位图返回空字符串，前端 el-image 会走 error slot 显示首字母
 */
export function fixLogoUrl(url: string | null | undefined): string {
  if (!url) return ''
  if (url.includes('/generic')) return ''
  if (url.startsWith('http://')) return 'https://' + url.slice(7)
  return url
}