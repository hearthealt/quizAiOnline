/** API响应结构 */
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

/** 分页结果 */
export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
}

/** 分页参数 */
export interface PageParams {
  pageNum?: number
  pageSize?: number
}

// ==================== 枚举定义 ====================

/** 状态枚举 */
export enum Status {
  DISABLED = 0,
  ENABLED = 1
}

/** 题目类型 */
export enum QuestionType {
  SINGLE = 1,    // 单选
  MULTIPLE = 2,  // 多选
  JUDGE = 3,     // 判断
  FILL = 4,      // 填空
  SHORT = 5      // 简答
}

/** 题目类型映射 */
export const QuestionTypeMap: Record<QuestionType, string> = {
  [QuestionType.SINGLE]: '单选题',
  [QuestionType.MULTIPLE]: '多选题',
  [QuestionType.JUDGE]: '判断题',
  [QuestionType.FILL]: '填空题',
  [QuestionType.SHORT]: '简答题'
}

/** 难度枚举 */
export enum Difficulty {
  EASY = 1,
  MEDIUM = 2,
  HARD = 3
}

/** 难度映射 */
export const DifficultyMap: Record<Difficulty, string> = {
  [Difficulty.EASY]: '简单',
  [Difficulty.MEDIUM]: '中等',
  [Difficulty.HARD]: '困难'
}

/** VIP订单状态 */
export enum OrderStatus {
  PENDING = 0,   // 待支付
  PAID = 1,      // 已支付
  CANCELLED = 2, // 已取消
  REFUNDED = 3   // 已退款
}

/** 订单状态映射 */
export const OrderStatusMap: Record<OrderStatus, string> = {
  [OrderStatus.PENDING]: '待支付',
  [OrderStatus.PAID]: '已支付',
  [OrderStatus.CANCELLED]: '已取消',
  [OrderStatus.REFUNDED]: '已退款'
}

// ==================== 实体类型 ====================

/** 分类 */
export interface Category {
  id: number
  name: string
  icon: string
  sort: number
  status: number
  createTime: string
}

/** 题库 */
export interface QuestionBank {
  id: number
  categoryId: number
  name: string
  description: string
  cover: string
  questionCount: number
  passScore: number
  sort: number
  status: number
  createTime: string
}

/** 题目 */
export interface Question {
  id: number
  bankId: number
  type: number
  content: string
  answer: string
  analysis: string
  difficulty: number
  sort: number
  status: number
  createTime: string
}

/** 题目选项 */
export interface QuestionOption {
  label: string
  content: string
}

/** 用户 */
export interface User {
  id: number
  openid: string
  phone: string
  nickname: string
  avatar: string
  status: number
  isVip: number
  vipExpireTime: string
  lastLoginTime: string
  createTime: string
}

/** 管理员 */
export interface Admin {
  id: number
  username: string
  nickname: string
  avatar: string
  role: string
  status: number
  createTime: string
}

/** VIP套餐 */
export interface VipPlan {
  id: number
  name: string
  duration: number
  price: number
  originalPrice: number
  description: string
  sort: number
  status: number
}

/** VIP订单 */
export interface VipOrder {
  id: number
  orderNo: string
  userId: number
  planId: number
  planName: string
  amount: number
  duration: number
  status: number
  paymentMethod: string
  paidTime: string
  vipStartTime: string
  vipEndTime: string
  createTime: string
}

/** AI配置 */
export interface AiConfig {
  id: number
  provider: string
  baseUrl: string
  apiKey: string
  model: string
  maxTokens: number
  temperature: number
  status: number
}

/** AI调用日志 */
export interface AiCallLog {
  id: number
  questionId: number
  callType: string
  mode: string
  prompt: string
  result: string
  tokensUsed: number
  costMs: number
  status: number
  errorMsg: string
  operatorId: number
  userId: number
  operatorName: string
  createTime: string
}

/** 仪表盘数据 */
export interface DashboardVO {
  totalUsers: number
  totalQuestions: number
  todayActive: number
  todayAnswers: number
  trends: TrendItem[]
}

/** 趋势项 */
export interface TrendItem {
  date: string
  count: number
}

/** 排行项 */
export interface RankItem {
  name: string
  count: number
}
