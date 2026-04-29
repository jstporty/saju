const TEN_GODS_KO: Record<string, { label: string; emoji: string }> = {
  비견: { label: "독립심", emoji: "🦁" },
  겁재: { label: "추진력", emoji: "🔥" },
  식신: { label: "표현력", emoji: "🎨" },
  상관: { label: "재치", emoji: "🎭" },
  편재: { label: "사업력", emoji: "💼" },
  정재: { label: "성실함", emoji: "💎" },
  편관: { label: "리더십", emoji: "👑" },
  정관: { label: "책임감", emoji: "🏛️" },
  편인: { label: "직관력", emoji: "🔮" },
  정인: { label: "지혜", emoji: "📚" },
};

export function PersonalityCards({ tenGods }: { tenGods: Record<string, number> }) {
  const top = Object.entries(tenGods)
    .filter(([, v]) => v > 0)
    .sort(([, a], [, b]) => b - a)
    .slice(0, 6);

  return (
    <div className="bg-white rounded-2xl p-5 shadow-sm">
      <h3 className="font-semibold text-slate-900 mb-4">성격 카드 6장</h3>
      <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
        {top.map(([god, count]) => {
          const meta = TEN_GODS_KO[god] ?? { label: god, emoji: "✨" };
          return (
            <div key={god} className="border border-slate-200 rounded-lg p-3 text-center">
              <div className="text-3xl mb-1">{meta.emoji}</div>
              <div className="font-medium text-sm">{meta.label}</div>
              <div className="text-xs text-slate-500 mt-1">{god} · {count}</div>
            </div>
          );
        })}
        {top.length === 0 && (
          <div className="col-span-3 text-center text-slate-400 py-6 text-sm">
            성격 데이터를 분석할 수 없습니다
          </div>
        )}
      </div>
    </div>
  );
}
