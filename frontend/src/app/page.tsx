"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { API_BASE } from "@/lib/api";
import { useAuthStore } from "@/store/authStore";

export default function LandingPage() {
  const router = useRouter();
  const { user, loading } = useAuthStore();

  // 로그인 상태면 즉시 redirect
  useEffect(() => {
    if (!loading && user) {
      router.replace("/onboarding");
    }
  }, [loading, user, router]);

  const handleKakao = () => {
    window.location.href = `${API_BASE}/oauth2/authorization/kakao`;
  };

  return (
    <main className="flex flex-1 flex-col items-center justify-center px-6 py-12 bg-gradient-to-b from-indigo-950 via-slate-900 to-indigo-950 text-white">
      <div className="flex flex-col items-center max-w-md w-full text-center">
        <span className="text-5xl font-bold tracking-tight mb-2">사주</span>
        <span className="text-slate-300 mb-12 text-sm">四柱</span>

        <h1 className="text-3xl sm:text-4xl font-bold mb-3 leading-tight">
          당신의 사주,<br />한눈에
        </h1>
        <p className="text-slate-300 mb-12 text-sm">
          5가지 에너지 · 성격 카드 · 오늘의 기운
        </p>

        <button
          onClick={handleKakao}
          disabled={loading}
          className="w-full bg-[#FEE500] text-[#191919] font-semibold py-4 rounded-lg hover:brightness-95 active:scale-[0.99] transition disabled:opacity-50"
        >
          {loading ? "확인 중..." : "카카오로 시작하기"}
        </button>

        <p className="text-xs text-slate-400 mt-8 leading-relaxed">
          시작하면{" "}
          <a href="#" className="underline">이용약관</a>
          {" · "}
          <a href="#" className="underline">개인정보처리방침</a>
          에 동의한 것으로 봅니다
        </p>
      </div>
    </main>
  );
}
