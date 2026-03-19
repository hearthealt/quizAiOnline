/** 分页结果 */
export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
}

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
  examTime: number
  examQuestionCount: number
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
  baseUrl: string
  apiKey: string
  model: string
  promptAnalysis: string
  promptAnswer: string
  promptBoth: string
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
