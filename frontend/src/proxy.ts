import { NextResponse, type NextRequest } from "next/server";

/**
 * 비로그인 사용자가 보호된 라우트로 직접 진입할 경우 Landing(/)으로 redirect.
 * 단, /s/[token] 공유 페이지는 비로그인 허용.
 *
 * 인증 신호: SESSION 쿠키 존재 (Spring Session JDBC 발급).
 * Cross-subdomain 운영 시 도메인이 .saju.app로 들어오므로 쿠키도 동일 도메인에서 보임.
 * 로컬 개발(localhost)은 SameSite 정책상 직접 검증이 어려워, 클라이언트 사이드 fetch /me 결과로 보강.
 */
export function proxy(req: NextRequest) {
  const { pathname } = req.nextUrl;

  // 보호 라우트
  const isProtected =
    pathname.startsWith("/onboarding") ||
    pathname.startsWith("/chart/") ||
    pathname.startsWith("/history");

  if (!isProtected) return NextResponse.next();

  // 세션 쿠키 존재 여부로 빠른 차단 (위조 가능하지만 UX 가드 목적)
  const hasSession = req.cookies.has("SESSION");
  if (hasSession) return NextResponse.next();

  const url = req.nextUrl.clone();
  url.pathname = "/";
  url.searchParams.set("from", pathname);
  return NextResponse.redirect(url);
}

export const config = {
  matcher: ["/onboarding/:path*", "/chart/:path*", "/history/:path*"],
};
