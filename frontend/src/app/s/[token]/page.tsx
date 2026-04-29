import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { FourPillarsView } from "@/components/FourPillarsView";
import { ElementsDonut } from "@/components/ElementsDonut";
import { CategoryFortuneList } from "@/components/CategoryFortuneList";
import type { PublicChartDto } from "@/lib/types";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080";
const APP_URL = process.env.NEXT_PUBLIC_APP_URL ?? "http://localhost:3000";

async function loadShare(token: string): Promise<PublicChartDto | null> {
  try {
    const res = await fetch(`${API_BASE}/api/v1/shares/${token}`, { cache: "no-store" });
    if (!res.ok) return null;
    const json = await res.json();
    return (json?.data ?? json) as PublicChartDto;
  } catch {
    return null;
  }
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ token: string }>;
}): Promise<Metadata> {
  const { token } = await params;
  const data = await loadShare(token);
  if (!data) return { title: "공유된 사주를 찾을 수 없습니다" };

  return {
    title: `${data.subjectNameMasked}님의 사주 — 공유`,
    description: data.keyMessage?.slice(0, 80) ?? "MZ를 위한 데이터화된 사주",
    openGraph: {
      title: `${data.subjectNameMasked}님의 사주`,
      description: data.keyMessage?.slice(0, 80) ?? "데이터화된 사주",
      url: `${APP_URL}/s/${token}`,
      images: [{ url: `${APP_URL}/api/og/${token}`, width: 1200, height: 630 }],
    },
    twitter: { card: "summary_large_image" },
  };
}

export default async function PublicSharePage({
  params,
}: {
  params: Promise<{ token: string }>;
}) {
  const { token } = await params;
  const data = await loadShare(token);
  if (!data) notFound();

  return (
    <main className="flex-1 flex flex-col pb-12">
      <header className="px-4 py-3 border-b border-slate-200 bg-white">
        <h1 className="font-semibold text-sm">{data.subjectNameMasked}님의 사주</h1>
        <p className="text-xs text-slate-500">공유 받은 사주 — 일부 정보는 가려져 있습니다</p>
      </header>

      <div className="flex-1 px-4 py-5 max-w-md w-full mx-auto space-y-4">
        {data.keyMessage && (
          <div className="bg-white rounded-2xl p-5 shadow-sm">
            <h3 className="font-semibold mb-2 text-slate-900">핵심 메시지</h3>
            <p className="text-sm text-slate-700 leading-relaxed whitespace-pre-line">{data.keyMessage}</p>
          </div>
        )}

        <FourPillarsView data={data.fourPillars} />
        <ElementsDonut scores={data.elementsScore} />
        {data.categories.length > 0 && <CategoryFortuneList items={data.categories} />}

        <Link
          href="/"
          className="block w-full bg-indigo-600 text-white text-center font-semibold py-3.5 rounded-md hover:bg-indigo-700 transition mt-6"
        >
          나도 사주 보러 가기
        </Link>
      </div>
    </main>
  );
}
