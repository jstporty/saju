"use client";

import { useQuery } from "@tanstack/react-query";
import { useParams, useRouter } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import { fetchChart, fetchCategories, fetchToday } from "@/lib/queries";
import { ApiError } from "@/lib/api";
import { FourPillarsView } from "@/components/FourPillarsView";
import { ElementsDonut } from "@/components/ElementsDonut";
import { PersonalityCards } from "@/components/PersonalityCards";
import { CategoryFortuneList } from "@/components/CategoryFortuneList";
import { TodayFortuneCard } from "@/components/TodayFortuneCard";
import { ShareActionBar } from "@/components/ShareActionBar";

export default function ChartPage() {
  const router = useRouter();
  const { id } = useParams<{ id: string }>();

  const chartQ = useQuery({ queryKey: ["chart", id], queryFn: () => fetchChart(id), enabled: !!id });
  const categoriesQ = useQuery({
    queryKey: ["categories", id],
    queryFn: () => fetchCategories(id),
    enabled: !!id,
  });
  const todayQ = useQuery({
    queryKey: ["today", id],
    queryFn: () => fetchToday(id),
    enabled: !!id && chartQ.data?.subjectKind === "SELF",
    retry: false,
  });

  if (chartQ.isLoading) {
    return <FullscreenMsg msg="사주를 분석하는 중..." />;
  }
  if (chartQ.error) {
    if (chartQ.error instanceof ApiError && chartQ.error.status === 401) {
      router.replace("/");
      return null;
    }
    return <FullscreenMsg msg="차트를 불러올 수 없습니다." />;
  }
  if (!chartQ.data) return null;
  const chart = chartQ.data;

  return (
    <main className="flex-1 flex flex-col pb-24">
      <header className="sticky top-0 z-10 px-4 py-3 flex items-center gap-3 border-b border-slate-200 bg-white/95 backdrop-blur">
        <button onClick={() => router.push("/history")} className="p-1">
          <ArrowLeft size={20} />
        </button>
        <div className="flex-1">
          <h1 className="font-semibold text-sm">{chart.subjectName}님의 사주</h1>
          <p className="text-xs text-slate-500">
            {chart.birth.birthDate} {chart.birth.birthTimeUnknown ? "(시간 모름)" : chart.birth.birthTime}
          </p>
        </div>
      </header>

      <div className="flex-1 px-4 py-5 max-w-md w-full mx-auto space-y-4">
        {/* 핵심 메시지 (S4 first fold) */}
        {chart.keyMessage && (
          <div className="bg-white rounded-2xl p-5 shadow-sm">
            <h3 className="font-semibold mb-2 text-slate-900">핵심 메시지</h3>
            <p className="text-sm text-slate-700 leading-relaxed whitespace-pre-line">{chart.keyMessage}</p>
          </div>
        )}

        <FourPillarsView data={chart.fourPillars} />
        <ElementsDonut scores={chart.elementsScore} />
        <PersonalityCards tenGods={chart.tenGodsCount} />

        {/* S5-3: 카테고리 운세 */}
        {categoriesQ.data && <CategoryFortuneList items={categoriesQ.data.categories} />}

        {/* S5-4: 오늘의 운세 (SELF만) */}
        {chart.subjectKind === "SELF" && todayQ.data && <TodayFortuneCard data={todayQ.data} />}

        {chart.warnings.length > 0 && (
          <div className="text-xs text-amber-700 bg-amber-50 border border-amber-200 rounded-md p-3">
            {chart.warnings.map((w, i) => (
              <div key={i}>· {w}</div>
            ))}
          </div>
        )}
      </div>

      <ShareActionBar chartId={chart.id} />
    </main>
  );
}

function FullscreenMsg({ msg }: { msg: string }) {
  return (
    <main className="flex-1 flex items-center justify-center text-slate-500 text-sm">{msg}</main>
  );
}
