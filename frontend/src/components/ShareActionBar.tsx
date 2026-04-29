"use client";

import { useState } from "react";
import { Share2, History as HistoryIcon, UserPlus } from "lucide-react";
import { useRouter } from "next/navigation";
import { useMutation } from "@tanstack/react-query";
import { createShare } from "@/lib/queries";

export function ShareActionBar({ chartId }: { chartId: string }) {
  const router = useRouter();
  const [copied, setCopied] = useState(false);

  const mutation = useMutation({
    mutationFn: () => createShare(chartId),
    onSuccess: async (res) => {
      try {
        await navigator.clipboard.writeText(res.shareUrl);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
      } catch {
        prompt("아래 링크를 복사하세요:", res.shareUrl);
      }
    },
  });

  return (
    <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-slate-200 p-3 flex gap-2 sm:max-w-md sm:mx-auto">
      <button
        onClick={() => mutation.mutate()}
        disabled={mutation.isPending}
        className="flex-1 bg-indigo-600 text-white py-3 rounded-md font-semibold flex items-center justify-center gap-2 disabled:opacity-60"
      >
        <Share2 size={16} />
        {mutation.isPending ? "생성 중..." : copied ? "링크 복사됨!" : "공유하기"}
      </button>
      <button
        onClick={() => router.push("/onboarding")}
        className="px-3 py-3 border border-slate-300 rounded-md flex items-center justify-center"
        title="다른 사람"
      >
        <UserPlus size={16} />
      </button>
      <button
        onClick={() => router.push("/history")}
        className="px-3 py-3 border border-slate-300 rounded-md flex items-center justify-center"
        title="이력"
      >
        <HistoryIcon size={16} />
      </button>
    </div>
  );
}
