/**
 * 统一的TypeScript类型定义
 * 集中管理所有API相关的类型
 */

// ==================== 通用类型 ====================

/** 分页结果 */
export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}

/** 分页请求参数 */
export interface PageParams {
  pageNum?: number;
  pageSize?: number;
}

/** API响应结构 */
export interface ApiResponse<T = any> {
  code: number;
  msg: string;
  data: T;
}

// ==================== 用户相关 ====================

/** 用户信息 */
export interface UserInfo {
  id: number;
  nickname: string;
  avatar: string;
  phone: string;
  isVip: number;
  vipExpireTime?: string;
}

/** 登录响应 */
export interface LoginVO {
  token: string;
  userInfo: UserInfo;
}

// ==================== 题库相关 ====================

/** 分类信息 */
export interface CategoryItem {
  id: number;
  name: string;
  icon?: string;
  bankCount?: number;
}

/** 题库简要信息 */
export interface BankSimple {
  id: number;
  name: string;
  cover?: string;
  questionCount?: number;
  practiceCount?: number;
}

/** 题库详情 */
export interface BankDetail extends BankSimple {
  categoryId: number;
  description?: string;
  examTime?: number;
  passScore?: number;
  isVip?: number;
}

// ==================== 题目相关 ====================

/** 题目类型枚举 */
export enum QuestionType {
  /** 单选题 */
  SINGLE = 1,
  /** 多选题 */
  MULTIPLE = 2,
  /** 判断题 */
  JUDGE = 3,
  /** 填空题 */
  FILL = 4,
  /** 简答题 */
  SHORT_ANSWER = 5
}

/** 题目难度枚举 */
export enum Difficulty {
  EASY = 1,
  MEDIUM = 2,
  HARD = 3
}

/** 题目选项 */
export interface QuestionOption {
  label: string;
  content: string;
}

/** 题目详情 */
export interface QuestionVO {
  id: number;
  bankId: number;
  type: QuestionType | number;
  content: string;
  answer?: string;
  analysis?: string;
  difficulty?: Difficulty | number;
  options?: QuestionOption[];
  isFavorite?: boolean;
  userAnswer?: string;
  isCorrect?: number;
}

// ==================== 练习相关 ====================

/** 练习模式枚举 */
export enum PracticeMode {
  /** 顺序练习 */
  SEQUENCE = 'sequence',
  /** 随机练习 */
  RANDOM = 'random',
  /** 错题重练 */
  WRONG = 'wrong'
}

/** 练习记录 */
export interface PracticeRecord {
  id: number;
  bankId: number;
  mode: PracticeMode | string;
  totalCount: number;
  answerCount: number;
  correctCount: number;
  status: number;
  lastIndex: number;
}

/** 练习结果 */
export interface PracticeResult {
  recordId: number;
  totalCount: number;
  answerCount: number;
  correctCount: number;
  correctRate: number;
  duration: number;
}

/** 练习进度 */
export interface PracticeProgress {
  answerCount: number;
  totalCount: number;
  correctCount: number;
  lastIndex?: number;
}

// ==================== 考试相关 ====================

/** 考试题目选项 */
export interface ExamQuestionOption {
  label: string;
  content: string;
}

/** 考试题目 */
export interface ExamQuestion {
  id: number;
  type: QuestionType | number;
  content: string;
  options: ExamQuestionOption[];
}

/** 开始考试结果 */
export interface StartExamResult {
  examId: number;
  totalCount: number;
  examTime: number;
  leftSeconds?: number;
  questions: ExamQuestion[];
  answers?: ExamAnswerTemp[];
}

/** 临时答案 */
export interface ExamAnswerTemp {
  questionId: number;
  answer: string;
}

/** 进行中的考试 */
export interface ExamOngoing {
  exists: boolean;
  expired: boolean;
  examId?: number;
  leftSeconds?: number;
}

/** 提交答案 */
export interface SubmitExamAnswer {
  questionId: number;
  answer: string;
}

/** 答题详情 */
export interface ExamAnswerDetail {
  questionId: number;
  content: string;
  userAnswer: string;
  correctAnswer: string;
  analysis?: string;
  isCorrect: boolean;
}

/** 考试结果 */
export interface ExamResult {
  examId: number;
  score: number;
  totalCount: number;
  correctCount: number;
  correctRate: number;
  duration: number;
  passScore: number;
  isPass: boolean;
  details: ExamAnswerDetail[];
}

// ==================== 记录相关 ====================

/** 记录类型 */
export type RecordType = 'exam' | 'practice';

/** 记录项 */
export interface RecordItem {
  id: number;
  bankName: string;
  mode: string;
  totalCount: number;
  correctCount: number;
  correctRate: number;
  createTime: string;
  type: RecordType | string;
}

// ==================== 首页相关 ====================

/** 学习统计 */
export interface StudyStats {
  totalDays: number;
  totalAnswered: number;
  correctRate: number;
  todayAnswered: number;
}

/** 首页数据 */
export interface HomeVO {
  categories: CategoryItem[];
  hotBanks: BankSimple[];
  dailyQuestion?: QuestionVO | null;
  studyStats?: StudyStats;
}

// ==================== 收藏 & 错题 ====================

/** 收藏的题目 */
export interface FavoriteQuestion {
  id: number;
  questionId: number;
  question: QuestionVO;
  createTime: string;
}

/** 错题记录 */
export interface WrongQuestion {
  id: number;
  questionId: number;
  bankId: number;
  wrongCount: number;
  lastWrongTime: string;
  question: QuestionVO;
}

// ==================== VIP相关 ====================

/** VIP套餐 */
export interface VipPackage {
  id: number;
  name: string;
  days: number;
  price: number;
  originalPrice?: number;
  description?: string;
}

/** VIP订单状态 */
export enum OrderStatus {
  /** 待支付 */
  PENDING = 0,
  /** 已支付 */
  PAID = 1,
  /** 已取消 */
  CANCELLED = 2,
  /** 已退款 */
  REFUNDED = 3
}

/** VIP订单 */
export interface VipOrder {
  id: number;
  orderNo: string;
  packageId: number;
  packageName: string;
  amount: number;
  status: OrderStatus | number;
  createTime: string;
  payTime?: string;
}

// ==================== AI相关 ====================

/** AI消息角色 */
export type AiRole = 'user' | 'assistant';

/** AI聊天消息 */
export interface AiMessage {
  role: AiRole;
  content: string;
  timestamp?: number;
}

/** AI分析请求 */
export interface AiAnalysisRequest {
  questionId: number;
  userAnswer?: string;
}

/** AI分析结果 */
export interface AiAnalysisResult {
  analysis: string;
  suggestions?: string[];
}
