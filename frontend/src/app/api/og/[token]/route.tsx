import { ImageResponse } from "@vercel/og";
import { NextRequest } from "next/server";

export const runtime = "edge";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080";

export async function GET(
  _: NextRequest,
  { params }: { params: Promise<{ token: string }> }
) {
  const { token } = await params;

  let title = "사주";
  let subtitle = "당신의 사주, 한눈에";
  let pillars: string[] = [];

  try {
    const res = await fetch(`${API_BASE}/api/v1/shares/${token}`, { cache: "no-store" });
    if (res.ok) {
      const json = await res.json();
      const data = json?.data ?? json;
      title = `${data.subjectNameMasked}님의 사주`;
      subtitle = data.keyMessage?.slice(0, 50) ?? subtitle;
      const fp = data.fourPillars;
      if (fp) {
        pillars = [
          fp.year ? `${fp.year.stem}${fp.year.branch}` : "",
          fp.month ? `${fp.month.stem}${fp.month.branch}` : "",
          fp.day ? `${fp.day.stem}${fp.day.branch}` : "",
          fp.hour ? `${fp.hour.stem}${fp.hour.branch}` : "—",
        ];
      }
    }
  } catch {
    // fallthrough to default
  }

  return new ImageResponse(
    (
      <div
        style={{
          width: "100%",
          height: "100%",
          display: "flex",
          flexDirection: "column",
          background: "linear-gradient(135deg, #1e1b4b 0%, #312e81 50%, #4338ca 100%)",
          color: "white",
          padding: 60,
          fontFamily: "sans-serif",
        }}
      >
        <div style={{ fontSize: 32, opacity: 0.7, marginBottom: 20 }}>사주 · 四柱</div>
        <div style={{ fontSize: 64, fontWeight: 700, marginBottom: 20 }}>{title}</div>
        <div style={{ fontSize: 28, opacity: 0.85, lineHeight: 1.4, marginBottom: 40 }}>
          {subtitle}
        </div>
        {pillars.length > 0 && (
          <div style={{ display: "flex", gap: 16, marginTop: "auto" }}>
            {pillars.map((p, i) => (
              <div
                key={i}
                style={{
                  flex: 1,
                  background: "rgba(255,255,255,0.1)",
                  borderRadius: 16,
                  padding: 24,
                  fontSize: 56,
                  fontWeight: 600,
                  textAlign: "center",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                }}
              >
                {p}
              </div>
            ))}
          </div>
        )}
      </div>
    ),
    { width: 1200, height: 630 }
  );
}
