import { request } from "@/utils/request";
import type { PageResult } from "@/api/category";

export type VipPlan = {
  id: number;
  name: string;
  duration: number;
  price: number;
  originalPrice?: number;
  description?: string;
};

export type VipStatus = {
  isVip: boolean;
  expireTime?: string;
  plans?: VipPlan[];
};

export type VipOrder = {
  id: number;
  orderNo: string;
  planName?: string;
  amount: number;
  duration?: number;
  status: number;
  paidTime?: string;
  createTime?: string;
};

export const getVipPlans = () =>
  request<VipPlan[]>({
    url: "/api/app/vip/plans",
    method: "GET"
  });

export const getVipStatus = () =>
  request<VipStatus>({
    url: "/api/app/vip/status",
    method: "GET"
  });

export const createVipOrder = (planId: number) =>
  request<VipOrder>({
    url: "/api/app/vip/order",
    method: "POST",
    data: { planId }
  });

export const payVipOrder = (id: number, paymentMethod = "mock") =>
  request<VipOrder>({
    url: `/api/app/vip/order/${id}/pay`,
    method: "POST",
    data: { paymentMethod }
  });

export const getVipOrders = (pageNum = 1, pageSize = 10) =>
  request<PageResult<VipOrder>>({
    url: "/api/app/vip/orders",
    method: "GET",
    data: { pageNum, pageSize }
  });
