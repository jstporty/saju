import type { FourPillarsDto, Pillar } from "@/lib/types";
import { stemKo, branchKo } from "@/lib/utils";

const COLS: { key: keyof FourPillarsDto; label: string }[] = [
  { key: "year", label: "연주" },
  { key: "month", label: "월주" },
  { key: "day", label: "일주" },
  { key: "hour", label: "시주" },
];

export function FourPillarsView({ data }: { data: FourPillarsDto }) {
  return (
    <div className="bg-white rounded-2xl p-5 shadow-sm">
      <h3 className="font-semibold text-slate-900 mb-4">사주 4기둥</h3>
      <div className="grid grid-cols-4 gap-2 text-center">
        {COLS.map(({ key, label }) => {
          const p: Pillar | null = data[key] as Pillar | null;
          return (
            <div key={key} className="border border-slate-200 rounded-lg py-3">
              <div className="text-xs text-slate-500 mb-1">{label}</div>
              {p ? (
                <>
                  <div className="text-2xl font-semibold text-indigo-700">{p.stem}</div>
                  <div className="text-2xl font-semibold text-rose-600">{p.branch}</div>
                  <div className="text-xs text-slate-500 mt-1">{stemKo(p.stem)}{branchKo(p.branch)}</div>
                </>
              ) : (
                <div className="text-slate-400 py-3 text-xs">시간 모름</div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
