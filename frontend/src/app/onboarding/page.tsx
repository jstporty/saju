"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useMutation } from "@tanstack/react-query";
import { ArrowLeft } from "lucide-react";
import { createChart } from "@/lib/queries";
import { ApiError } from "@/lib/api";
import { calcAge } from "@/lib/utils";
import type { ChartCreateRequest } from "@/lib/types";

type Form = {
  subjectKind: "SELF" | "OTHER";
  subjectName: string;
  birthDate: string;
  birthTime: string;
  birthTimeUnknown: boolean;
  calendarType: "SOLAR" | "LUNAR";
  isLeapMonth: boolean;
  gender: "MALE" | "FEMALE";
};

export default function OnboardingPage() {
  const router = useRouter();
  const [form, setForm] = useState<Form>({
    subjectKind: "SELF",
    subjectName: "",
    birthDate: "",
    birthTime: "",
    birthTimeUnknown: false,
    calendarType: "SOLAR",
    isLeapMonth: false,
    gender: "MALE",
  });
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [showJasiModal, setShowJasiModal] = useState(false);
  const [showMinorModal, setShowMinorModal] = useState(false);
  const [pendingPolicy, setPendingPolicy] = useState<"PREV_DAY" | "NEXT_DAY" | null>(null);

  const update = <K extends keyof Form>(k: K, v: Form[K]) => setForm((f) => ({ ...f, [k]: v }));

  const mutation = useMutation({
    mutationFn: (req: ChartCreateRequest) => createChart(req),
    onSuccess: (chart) => router.push(`/chart/${chart.id}`),
    onError: (e: unknown) => {
      if (e instanceof ApiError) setErrorMsg(`${e.code}: ${e.message}`);
      else setErrorMsg("요청 실패. 다시 시도해주세요.");
    },
  });

  const validateAndSubmit = (jasiPolicy: "PREV_DAY" | "NEXT_DAY" | null = null) => {
    setErrorMsg(null);

    if (!form.subjectName.trim()) {
      setErrorMsg("이름을 입력해주세요");
      return;
    }
    if (!form.birthDate) {
      setErrorMsg("생년월일을 입력해주세요");
      return;
    }
    if (calcAge(form.birthDate) < 14) {
      setShowMinorModal(true);
      return;
    }
    if (!form.birthTimeUnknown && !form.birthTime) {
      setErrorMsg("생시를 입력하거나 '시간 모름'을 체크하세요");
      return;
    }
    // 자시 모달 강제 (00:00 ~ 00:59)
    if (!form.birthTimeUnknown && form.birthTime && !jasiPolicy) {
      const [h] = form.birthTime.split(":").map(Number);
      if (h === 0) {
        setShowJasiModal(true);
        return;
      }
    }

    const req: ChartCreateRequest = {
      subjectName: form.subjectName.trim(),
      subjectKind: form.subjectKind,
      birthDate: form.birthDate,
      birthTime: form.birthTimeUnknown ? null : form.birthTime,
      birthTimeUnknown: form.birthTimeUnknown,
      calendarType: form.calendarType,
      isLeapMonth: form.calendarType === "LUNAR" ? form.isLeapMonth : false,
      gender: form.gender,
      jasiPolicy: jasiPolicy ?? null,
    };
    mutation.mutate(req);
  };

  return (
    <main className="flex-1 flex flex-col">
      <header className="px-4 py-4 flex items-center gap-3 border-b border-slate-200 bg-white">
        <button onClick={() => router.back()} className="p-1">
          <ArrowLeft size={20} />
        </button>
        <h1 className="font-semibold">사주 정보 입력</h1>
      </header>

      <div className="flex-1 px-6 py-8 max-w-md w-full mx-auto space-y-6">
        {/* SELF / OTHER tabs */}
        <div className="flex gap-2 bg-slate-100 p-1 rounded-lg">
          {(["SELF", "OTHER"] as const).map((k) => (
            <button
              key={k}
              onClick={() => update("subjectKind", k)}
              className={`flex-1 py-2 rounded-md text-sm font-medium transition ${
                form.subjectKind === k ? "bg-white shadow text-slate-900" : "text-slate-500"
              }`}
            >
              {k === "SELF" ? "나 자신" : "다른 사람"}
            </button>
          ))}
        </div>

        <Field label={form.subjectKind === "SELF" ? "이름" : "상대방 이름"}>
          <input
            type="text"
            maxLength={50}
            placeholder="홍길동"
            value={form.subjectName}
            onChange={(e) => update("subjectName", e.target.value)}
            className="w-full px-3 py-2.5 border border-slate-300 rounded-md text-sm"
          />
        </Field>

        <Field label="생년월일">
          <input
            type="date"
            value={form.birthDate}
            onChange={(e) => update("birthDate", e.target.value)}
            min="1900-01-01"
            max={new Date().toISOString().slice(0, 10)}
            className="w-full px-3 py-2.5 border border-slate-300 rounded-md text-sm"
          />
        </Field>

        <Field label="달력">
          <div className="flex gap-2">
            {(["SOLAR", "LUNAR"] as const).map((c) => (
              <button
                key={c}
                onClick={() => update("calendarType", c)}
                className={`flex-1 py-2 border rounded-md text-sm ${
                  form.calendarType === c ? "border-indigo-600 bg-indigo-50 text-indigo-700" : "border-slate-300"
                }`}
              >
                {c === "SOLAR" ? "양력" : "음력"}
              </button>
            ))}
          </div>
          {form.calendarType === "LUNAR" && (
            <label className="flex items-center gap-2 mt-2 text-sm text-slate-600">
              <input
                type="checkbox"
                checked={form.isLeapMonth}
                onChange={(e) => update("isLeapMonth", e.target.checked)}
              />
              윤달
            </label>
          )}
        </Field>

        <Field label="생시">
          <input
            type="time"
            value={form.birthTime}
            disabled={form.birthTimeUnknown}
            onChange={(e) => update("birthTime", e.target.value)}
            className="w-full px-3 py-2.5 border border-slate-300 rounded-md text-sm disabled:bg-slate-100"
          />
          <label className="flex items-center gap-2 mt-2 text-sm text-slate-600">
            <input
              type="checkbox"
              checked={form.birthTimeUnknown}
              onChange={(e) => {
                update("birthTimeUnknown", e.target.checked);
                if (e.target.checked) update("birthTime", "");
              }}
            />
            시간을 모릅니다 (시주 제외 분석)
          </label>
        </Field>

        <Field label="성별">
          <div className="flex gap-2">
            {(["MALE", "FEMALE"] as const).map((g) => (
              <button
                key={g}
                onClick={() => update("gender", g)}
                className={`flex-1 py-2 border rounded-md text-sm ${
                  form.gender === g ? "border-indigo-600 bg-indigo-50 text-indigo-700" : "border-slate-300"
                }`}
              >
                {g === "MALE" ? "남성" : "여성"}
              </button>
            ))}
          </div>
        </Field>

        {errorMsg && <p className="text-sm text-red-600">{errorMsg}</p>}

        <button
          onClick={() => validateAndSubmit()}
          disabled={mutation.isPending}
          className="w-full bg-indigo-600 text-white font-semibold py-3.5 rounded-md hover:bg-indigo-700 transition disabled:opacity-60"
        >
          {mutation.isPending ? "분석 중..." : "사주 보기"}
        </button>
      </div>

      {/* 자시 모달 */}
      {showJasiModal && (
        <Modal title="자시(子時) 어떤 날로 처리할까요?" onClose={() => setShowJasiModal(false)}>
          <p className="text-sm text-slate-600 mb-4">
            00:00 ~ 00:59 출생은 전통 명리학 해석이 갈립니다. 어느 날의 자시로 보고 싶으신가요?
          </p>
          <div className="space-y-2">
            <button
              onClick={() => {
                setShowJasiModal(false);
                setPendingPolicy("PREV_DAY");
                validateAndSubmit("PREV_DAY");
              }}
              className="w-full py-3 border border-slate-300 rounded-md text-left px-4 hover:bg-slate-50"
            >
              <div className="font-medium">야자시 (전날 자시)</div>
              <div className="text-xs text-slate-500 mt-0.5">전날에 속한 시간으로 봄 (전통)</div>
            </button>
            <button
              onClick={() => {
                setShowJasiModal(false);
                setPendingPolicy("NEXT_DAY");
                validateAndSubmit("NEXT_DAY");
              }}
              className="w-full py-3 border border-slate-300 rounded-md text-left px-4 hover:bg-slate-50"
            >
              <div className="font-medium">자시 (당일)</div>
              <div className="text-xs text-slate-500 mt-0.5">당일의 첫 시간으로 봄 (현대)</div>
            </button>
          </div>
        </Modal>
      )}

      {/* 미성년자 차단 모달 */}
      {showMinorModal && (
        <Modal title="만 14세 이상부터 이용 가능합니다" onClose={() => setShowMinorModal(false)}>
          <p className="text-sm text-slate-600 mb-4">
            정보통신망법에 따라 만 14세 미만은 가입이 제한됩니다.
          </p>
          <button
            onClick={() => setShowMinorModal(false)}
            className="w-full py-3 bg-indigo-600 text-white rounded-md font-medium"
          >
            확인
          </button>
        </Modal>
      )}

      {/* unused suppressor */}
      <span className="hidden">{pendingPolicy}</span>
    </main>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <label className="block text-sm font-medium text-slate-700 mb-1.5">{label}</label>
      {children}
    </div>
  );
}

function Modal({
  title,
  children,
  onClose,
}: {
  title: string;
  children: React.ReactNode;
  onClose: () => void;
}) {
  return (
    <div className="fixed inset-0 bg-black/50 flex items-end sm:items-center justify-center z-50" onClick={onClose}>
      <div
        className="bg-white w-full sm:max-w-md rounded-t-2xl sm:rounded-2xl p-6 max-h-[80vh] overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="font-semibold text-lg mb-3">{title}</h2>
        {children}
      </div>
    </div>
  );
}
