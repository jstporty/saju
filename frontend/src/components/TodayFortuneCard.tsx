import type { TodayFortuneResponse } from "@/lib/types";

export function TodayFortuneCard({ data }: { data: TodayFortuneResponse }) {
  return (
    <div className="bg-gradient-to-br from-indigo-600 to-violet-700 rounded-2xl p-5 text-white shadow-sm">
      <div className="flex items-baseline justify-between mb-3">
        <h3 className="font-semibold">오늘의 기운</h3>
        <span className="text-xs text-indigo-100">{data.dayLabel}</span>
      </div>
      <p className="text-sm leading-relaxed mb-4 whitespace-pre-line min-h-[3em]">
        {data.message ?? "오늘의 운세 데이터를 준비 중입니다."}
      </p>
      {(data.luckyColor || data.luckyHour || data.caution) && (
        <div className="grid grid-cols-3 gap-2 text-xs">
          {data.luckyColor && (
            <Stat label="행운의 색" value={data.luckyColor} />
          )}
          {data.luckyHour && (
            <Stat label="행운의 시간" value={data.luckyHour} />
          )}
          {data.caution && (
            <Stat label="주의" value={data.caution} />
          )}
        </div>
      )}
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="bg-white/10 rounded-lg p-2.5">
      <div className="text-indigo-200 text-[10px] mb-0.5">{label}</div>
      <div className="font-medium text-xs leading-tight">{value}</div>
    </div>
  );
}
