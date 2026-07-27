// 分页数据提取工具集
// 兼容后端多种分页返回格式（records / list / content），统一提取列表与总数

interface PageData {
  records?: unknown[]
  list?: unknown[]
  content?: unknown[]
  total?: number | string
  totalElements?: number
  [key: string]: unknown
}

/**
 * 提取分页列表数据
 * 兼容后端返回的 records / list / content 三种字段，以及直接返回数组的情况
 * @param data - 后端返回对象（可为数组或 {records/list/content, total} 结构）
 * @returns 列表数组，data 为空时返回 []
 */
export function extractList(data: unknown): unknown[] {
  if (!data) return []
  if (Array.isArray(data)) return data
  const page = data as PageData
  return (page.records || page.list || page.content || []) as unknown[]
}

/**
 * 提取分页总数
 * 仅读取 data.total 字段（数字直接返回，字符串尝试 parseInt，否则返回 0）
 * 注意：本函数不回退到 data.records.length 或 data.totalElements，
 *       若原逻辑使用了这些回退，请保留原 || 链以避免改变行为
 * @param data - 后端返回对象
 * @returns 总数
 */
export function extractTotal(data: unknown): number {
  if (!data) return 0
  const page = data as PageData
  const t = page.total
  if (typeof t === 'number') return t
  return parseInt(t as string) || 0
}