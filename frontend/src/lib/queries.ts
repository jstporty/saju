import { api } from "./api";
import type {
  CategoryFortunesResponse,
  ChartCreateRequest,
  ChartResponse,
  ChartSummary,
  PublicChartDto,
  ShareCreatedResponse,
  TodayFortuneResponse,
  UserMe,
} from "./types";

// Auth
export const fetchMe = () => api<UserMe>("/api/v1/auth/me");
export const deleteAccount = () => api<void>("/api/v1/auth/me", { method: "DELETE" });

// Charts
export const createChart = (req: ChartCreateRequest) =>
  api<ChartResponse>("/api/v1/charts", { method: "POST", body: req });
export const fetchChart = (id: string) => api<ChartResponse>(`/api/v1/charts/${id}`);
export const listCharts = (size = 20) =>
  api<ChartSummary[]>(`/api/v1/charts?size=${size}`);
export const deleteChart = (id: string) =>
  api<void>(`/api/v1/charts/${id}`, { method: "DELETE" });

// Fortunes
export const fetchCategories = (id: string) =>
  api<CategoryFortunesResponse>(`/api/v1/charts/${id}/categories`);
export const fetchToday = (id: string) =>
  api<TodayFortuneResponse>(`/api/v1/charts/${id}/today`);

// Shares
export const createShare = (chartId: string) =>
  api<ShareCreatedResponse>("/api/v1/shares", { method: "POST", body: { chartId } });
export const fetchPublicShare = (token: string) =>
  api<PublicChartDto>(`/api/v1/shares/${token}`);
