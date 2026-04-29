import Link from "next/link";

export default function NotFound() {
  return (
    <main className="flex-1 flex flex-col items-center justify-center px-6 text-center">
      <div className="text-5xl mb-4">😔</div>
      <h1 className="text-xl font-semibold mb-2">공유 링크를 찾을 수 없습니다</h1>
      <p className="text-sm text-slate-500 mb-6">만료되었거나 삭제된 링크일 수 있어요.</p>
      <Link href="/" className="bg-indigo-600 text-white px-6 py-3 rounded-md font-medium">
        홈으로
      </Link>
    </main>
  );
}
