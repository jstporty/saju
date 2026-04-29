"use client";

import { useState } from "react";
import { ChevronDown } from "lucide-react";
import type { CategoryEntry } from "@/lib/types";

export function CategoryFortuneList({ items }: { items: CategoryEntry[] }) {
  return (
    <div className="bg-white rounded-2xl p-5 shadow-sm">
      <h3 className="font-semibold text-slate-900 mb-4">카테고리 운세</h3>
      <div className="divide-y divide-slate-100">
        {items.map((c) => (
          <Item key={c.category} entry={c} />
        ))}
      </div>
    </div>
  );
}

function Item({ entry }: { entry: CategoryEntry }) {
  const [open, setOpen] = useState(false);
  return (
    <div>
      <button
        onClick={() => setOpen((o) => !o)}
        className="w-full py-3.5 flex items-center justify-between text-left"
      >
        <div className="flex items-center gap-3">
          <span className="text-xl">{entry.icon ?? "✨"}</span>
          <span className="font-medium">{entry.label}</span>
        </div>
        <ChevronDown size={18} className={`transition ${open ? "rotate-180" : ""}`} />
      </button>
      {open && (
        <div className="pb-4 text-sm text-slate-700 leading-relaxed whitespace-pre-line">
          {entry.message ?? "운세 데이터를 준비 중입니다."}
        </div>
      )}
    </div>
  );
}
