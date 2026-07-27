// 核心类型定义：与后端 DTO/VO 字段对齐
// 后端 API 统一响应包络：{ code, message, data }

/** 后端统一响应包络 */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

/** 用户角色：0=普通用户，1=管理员 */
export type UserRole = 0 | 1

/** 用户信息 */
export interface UserInfo {
  id: number
  username: string
  nickname: string
  email: string
  avatar: string
  points: number
  netProfit: number
  totalBets: number
  correctBets: number
  winStreak: number
  role: UserRole
  loanAmount: number
  lastStipendDate: string
  loanLimit: number
}

/** 比赛状态：0=未开始，1=进行中，2=已结束 */
export type MatchStatus = 0 | 1 | 2

/** 比赛结果：null=未结算，'A'/'B'/'DRAW'=已结算 */
export type MatchResult = 'A' | 'B' | 'DRAW' | null

/** 比赛信息 */
export interface MatchInfo {
  id: number
  gameId: number
  title: string
  teamA: string
  teamB: string
  teamALogo: string
  teamBLogo: string
  tournament: string
  matchTime: string
  scoreA: number
  scoreB: number
  status: MatchStatus
  result: MatchResult
  oddsA: number
  oddsB: number
  betCount: number
  poolA: number
  poolB: number
  betDeadline: string
}

/** 押注状态：0=已下注未结算，1=猜中，2=猜错，3=平局退回 */
export type BetStatus = 0 | 1 | 2 | 3

/** 押注侧：'A' 或 'B' */
export type BetSide = 'A' | 'B'

/** 押注信息 */
export interface BetInfo {
  id: number
  userId: number
  matchId: number
  betSide: BetSide
  teamASnapshot: string
  teamBSnapshot: string
  betAmount: number
  status: BetStatus
  createTime: string
}

/** 押注历史 VO（含比赛快照与结算信息） */
export interface BetHistoryVO extends BetInfo {
  matchTitle: string
  teamA: string
  teamB: string
  scoreA: number
  scoreB: number
  result: MatchResult
  returnedAmount: number
  netChange: number
}

/** 评论 VO */
export interface CommentVO {
  id: number
  matchId: number
  userId: number
  username: string
  nickname: string
  avatar: string
  content: string
  createTime: string
}

/** 通知信息 */
export interface NotificationInfo {
  id: number
  userId: number
  type: number
  title: string
  content: string
  isRead: boolean
  createTime: string
}

/** 公告信息 */
export interface AnnouncementInfo {
  id: number
  title: string
  content: string
  createTime: string
}

/** 排行榜条目 */
export interface LeaderboardEntry {
  rank: number
  userId: number
  username: string
  nickname: string
  avatar: string
  points: number
  netProfit: number
  winRate: number
}