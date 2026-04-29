"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { ArrowLeft, Trash2, Plus } from "lucide-react";
import { listCharts, deleteChart } from "@/lib/queries";
import { ApiError } from "@/lib/api";

export default function HistoryPage() {
  const router = useRouter();
  const qc = useQueryClient();

  const { data, isLoading, error } = useQuery({
    queryKey: ["charts"],
    queryFn: () => listCharts(50),
  });

  const del = useMutation({
    mutationFn: (id: string) => deleteChart(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["charts"] }),
  });

  if (error instanceof ApiError && error.status === 401) {
    router.replace("/");
    return null;
  }

  return (
    <main className="flex-1 flex flex-col">
      <header className="px-4 py-3 flex items-center gap-3 border-b border-slate-200 bg-white">
        <button onClick={() => router.back()} className="p-1">
          <ArrowLeft size={20} />
        </button>
        <h1 className="font-semibold flex-1">내 사주 이력</h1>
        <Link href="/onboarding" className="p-1">
          <Plus size={20} />
        </Link>
      </header>

      <div className="flex-1 px-4 py-5 max-w-md w-full mx-auto space-y-3">
        {isLoading && <p className="text-center text-slate-400 text-sm py-12">불러오는 중...</p>}
        {data && data.length === 0 && (
          <div className="text-center py-12">
            <p className="text-slate-500 text-sm mb-4">아직 분석한 사주가 없어요</p>
            <Link
              href="/onboarding"
              className="inline-block bg-indigo-600 text-white px-6 py-2.5 rounded-md text-sm font-medium"
            >
              첫 사주 보기
            </Link>
          </div>
        )}
        {data?.map((c) => (
          <div key={c.id} className="bg-white rounded-xl p-4 shadow-sm flex items-center gap-3">
            <button onClick={() => router.push(`/chart/${c.id}`)} className="flex-1 text-left">
              <div className="flex items-baseline gap-2 mb-1">
                <span className="font-medium">{c.subjectName}</span>
                <span className="text-xs px-1.5 py-0.5 bg-slate-100 rounded text-slate-500">
                  {c.subjectKind === "SELF" ? "본인" : "지인"}
                </span>
              </div>
              <div className="text-xs text-slate-500">{c.birthDate}</div>
              {c.keyMessagePreview && (
                <div className="text-xs text-slate-600 mt-1.5 line-clamp-1">{c.keyMessagePreview}</div>
              )}
            </button>
            <button
              onClick={() => {
                if (confirm(`'${c.subjectName}'님의 사주를 삭제할까요?\n공유 링크도 함께 폐기됩니다.`)) {
                  del.mutate(c.id);
                }
              }}
              className="p-2 text-slate-400 hover:text-red-600"
            >
              <Trash2 size={16} />
            </button>
          </div>
        ))}
      </div>
    </main>
  );
}
