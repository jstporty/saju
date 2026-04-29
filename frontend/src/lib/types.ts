// Shared API types — backend DTO와 1:1 매핑

export type ChartCreateRequest = {
  subjectName: string;
  subjectKind: "SELF" | "OTHER";
  birthDate: string;             // ISO yyyy-MM-dd
  birthTime: string | null;      // HH:mm or null = 시간 모름
  birthTimeUnknown: boolean;
  calendarType: "SOLAR" | "LUNAR";
  isLeapMonth: boolean;
  gender: "MALE" | "FEMALE";
  jasiPolicy?: "PREV_DAY" | "NEXT_DAY" | null;
  birthLongitude?: number;
};

export type Pillar = { stem: string; branch: string; ganJi: string };

export type FourPillarsDto = {
  year: Pillar;
  month: Pillar;
  day: Pillar;
  hour: Pillar | null;
};

export type ElementsScore = {
  wood: number;
  fire: number;
  earth: number;
  metal: number;
  water: number;
};

export type ChartResponse = {
  id: string;
  subjectName: string;
  subjectKind: "SELF" | "OTHER";
  birth: {
    birthDate: string;
    birthTime: string | null;
    birthTimeUnknown: boolean;
    calendarType: string;
    isLeapMonth: boolean;
    gender: string;
  };
  fourPillars: FourPillarsDto;
  elementsScore: ElementsScore;
  tenGodsCount: Record<string, number>;
  keyMessage: string | null;
  warnings: string[];
  createdAt: string;
};

export type ChartSummary = {
  id: string;
  subjectName: string;
  subjectKind: "SELF" | "OTHER";
  birthDate: string;
  keyMessagePreview: string | null;
  createdAt: string;
};

export type CategoryEntry = {
  category: string;
  label: string;
  icon?: string;
  message: string | null;
};

export type CategoryFortunesResponse = { categories: CategoryEntry[] };

export type TodayFortuneResponse = {
  date: string;
  dayLabel: string;
  message: string | null;
  luckyColor?: string;
  luckyHour?: string;
  caution?: string;
};

export type ShareCreatedResponse = {
  token: string;
  shareUrl: string;
  expiresAt: string;
};

export type PublicChartDto = {
  subjectNameMasked: string;
  fourPillars: FourPillarsDto;
  elementsScore: ElementsScore;
  keyMessage: string | null;
  categories: CategoryEntry[];
};

export type UserMe = {
  id: string;
  email: string;
  name: string;
  profileImage: string | null;
  provider: string;
  roles: string[];
  createdAt: string;
  lastLoginAt: string | null;
};
