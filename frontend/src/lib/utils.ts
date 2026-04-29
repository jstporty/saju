import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

const STEM_KO: Record<string, string> = {
  甲: "갑", 乙: "을", 丙: "병", 丁: "정", 戊: "무",
  己: "기", 庚: "경", 辛: "신", 壬: "임", 癸: "계",
};

const BRANCH_KO: Record<string, string> = {
  子: "자", 丑: "축", 寅: "인", 卯: "묘", 辰: "진", 巳: "사",
  午: "오", 未: "미", 申: "신", 酉: "유", 戌: "술", 亥: "해",
};

export const stemKo = (h: string) => STEM_KO[h] ?? h;
export const branchKo = (h: string) => BRANCH_KO[h] ?? h;

const ELEMENT_KO: Record<string, string> = {
  wood: "목", fire: "화", earth: "토", metal: "금", water: "수",
};

export const elementKo = (e: string) => ELEMENT_KO[e] ?? e;

export const ELEMENT_COLORS: Record<string, string> = {
  wood: "#15803d",
  fire: "#dc2626",
  earth: "#a16207",
  metal: "#475569",
  water: "#1e40af",
};

/** 만 나이 계산 (KST 기준 자정으로 단순화). */
export function calcAge(birthDate: string): number {
  const birth = new Date(birthDate);
  const today = new Date();
  let age = today.getFullYear() - birth.getFullYear();
  const m = today.getMonth() - birth.getMonth();
  if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) age--;
  return age;
}
