"use client";

import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from "recharts";
import type { ElementsScore } from "@/lib/types";
import { ELEMENT_COLORS, elementKo } from "@/lib/utils";

const ORDER: (keyof ElementsScore)[] = ["wood", "fire", "earth", "metal", "water"];

export function ElementsDonut({ scores }: { scores: ElementsScore }) {
  const data = ORDER.map((k) => ({ name: elementKo(k), value: scores[k], key: k }));
  const total = data.reduce((s, d) => s + d.value, 0);

  return (
    <div className="bg-white rounded-2xl p-5 shadow-sm">
      <h3 className="font-semibold text-slate-900 mb-1">5가지 에너지</h3>
      <p className="text-xs text-slate-500 mb-4">오행 분포 (총 {total})</p>
      <div className="h-56">
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie data={data} dataKey="value" innerRadius={60} outerRadius={90} paddingAngle={2}>
              {data.map((d) => (
                <Cell key={d.key} fill={ELEMENT_COLORS[d.key]} />
              ))}
            </Pie>
            <Tooltip formatter={(v, name) => [`${v}점`, name as string]} />
          </PieChart>
        </ResponsiveContainer>
      </div>
      <div className="flex flex-wrap justify-center gap-2 mt-3">
        {data.map((d) => (
          <span key={d.key} className="text-xs px-2 py-1 rounded-full bg-slate-100 flex items-center gap-1">
            <span className="w-2 h-2 rounded-full" style={{ background: ELEMENT_COLORS[d.key] }} />
            {d.name} {d.value}
          </span>
        ))}
      </div>
    </div>
  );
}
