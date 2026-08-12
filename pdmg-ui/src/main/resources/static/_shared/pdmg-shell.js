/**
 * PDMG UI shell — hash router aligned with tcf-ontology-service workbench (#/design).
 */
(function () {
  const STATIC_PROGRAM_IDS = new Set(["mgcoa9000", "mgcoa9001", "mgcoa9100", "mgcoa8888", "mgcoa5530", "mgcoa9999"]);
  const VIEW_SRC = {
    mgcoa5530: "/mgcoa5530/index.html",
    mgcoa8888: "/mgcoa8888/index.html",
    mgcoa9000: "/mgcoa9000/index.html",
    mgcoa9001: "/mgcoa9001/index.html",
    mgcoa9100: "/mgcoa9100/index.html",
    mgcoa9999: "/mgcoa9999/index.html",
    imagelog: "/imagelog/index.html",
    txparam: "/txparam/index.html",
    txcontrol: "/txcontrol/index.html",
    rtdiag: "/rtdiag/index.html",
    jwt: "/jwt/admin/login.html",
    "jwt-token": "/jwt/admin/token.html",
    "jwt-login-history": "/jwt/admin/login-history.html",
    "jwt-refresh-token": "/jwt/admin/refresh-token.html",
    "jwt-security-policy": "/jwt/admin/security-policy.html",
    "jwt-jwks": "/jwt/admin/jwks.html"
  };
  const META = {
    home: {
      title: "Home",
      desc: "pdmg-service 전문 테스트 허브와 Architecture Design Wizard 진입점입니다."
    },
    design: {
      title: "Architecture Design",
      desc: "tcf-ontology-service Workbench STEP 1~7 · ServiceId / Table / Application / Gate / Export"
    },
    mgcoa5530: { title: "mgcoa5530", desc: "마케팅희망고객 / 안내항목 목록 조회 전문 테스트" },
    mgcoa8888: { title: "mgcoa8888", desc: "이미지로그 조회/삭제 전문 테스트" },
    mgcoa9000: { title: "mgcoa9000", desc: "거래 파라미터 CRUD 전문 테스트" },
    mgcoa9001: { title: "mgcoa9001", desc: "거래통제 CRUD 전문 테스트" },
    mgcoa9100: { title: "mgcoa9100", desc: "런타임 진단 스냅샷 조회 전문 테스트" },
    mgcoa9999: { title: "mgcoa9999", desc: "영업팁 실적 목록 조회 전문 테스트" },
    imagelog: { title: "이미지로그 관리", desc: "TB_FW_IMAGE_LOG 조건검색 관리 화면" },
    txparam: { title: "거래 파라미터 관리", desc: "TB_MG_TX_PARAM 그리드 관리 화면" },
    txcontrol: { title: "거래통제 관리", desc: "TB_MG_TX_CONTROL 유형·대상·차단 관리 화면" },
    rtdiag: { title: "런타임 진단 가이드", desc: "0→6단계 순차 진단 · mgcoa9100S0 스냅샷" },
    jwt: { title: "JWT 로그인", desc: "pdmg-jwt 로그인 (브라우저 직접 호출)" },
    "jwt-token": { title: "JWT 토큰 현황", desc: "Access/Refresh 발급·목록·폐기" },
    "jwt-login-history": { title: "JWT 로그인 이력", desc: "로그인 성공/실패 이력 조회" },
    "jwt-refresh-token": { title: "JWT Refresh Token", desc: "Refresh Token 목록·세션" },
    "jwt-security-policy": { title: "JWT 보안정책", desc: "토큰 유효기간·정책 조회/수정" },
    "jwt-jwks": { title: "JWK 공개키", desc: "pdmg-jwt JWKS 엔드포인트 조회" }
  };

  const pageTitle = document.getElementById("pageTitle");
  const pageDesc = document.getElementById("pageDesc");
  const pageTop = document.getElementById("pageTop");
  const viewRoot = document.getElementById("viewRoot");
  const contentFrame = document.getElementById("contentFrame");
  const targetInfo = document.getElementById("targetInfo");

  let ontologyBaseUrl = "http://localhost:8098";
  let targetBaseUrl = "http://localhost:8080";
  let jwtBaseUrl = "http://localhost:8110";

  /** 로그인 없이 허용하는 셸 라우트 */
  const PUBLIC_ROUTES = new Set(["home", "design", "jwt"]);

  function uiUrl(path) {
    return typeof window.nsightUiUrl === "function" ? window.nsightUiUrl(path) : path;
  }

  function escapeHtml(v) {
    return String(v ?? "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function parseHash() {
    migrateLegacyHash();
    const raw = (location.hash || "#/home").replace(/^#\/?/, "");
    const [pathPart] = raw.split("?");
    const route = (pathPart || "home").split("/")[0] || "home";
    return { route };
  }

  /** #view=mgcoa5530 → #/mgcoa5530 */
  function migrateLegacyHash() {
    const h = location.hash || "";
    if (!h.startsWith("#view=")) return;
    const params = new URLSearchParams(h.slice(1));
    const view = params.get("view") || "dashboard";
    const section = params.get("section") || "";
    if (view === "dashboard") {
      location.replace(section ? `#/home?section=${encodeURIComponent(section)}` : "#/home");
    } else {
      location.replace(`#/${encodeURIComponent(view)}`);
    }
  }

  function setActiveNav(route) {
    document.querySelectorAll(".wb-menu__link").forEach((a) => {
      a.classList.toggle("is-active", a.dataset.route === route);
    });
  }

  function setPageMeta(route) {
    const meta = META[route] || META.home;
    pageTitle.textContent = meta.title;
    pageDesc.textContent = meta.desc;
    document.title = `${meta.title} · PDMG UI`;
  }

  function showHome() {
    pageTop.hidden = false;
    viewRoot.hidden = false;
    viewRoot.classList.remove("wb-view--frame");
    contentFrame.hidden = true;
    if (contentFrame.getAttribute("src")) contentFrame.removeAttribute("src");
  }

  function showFrame(src) {
    pageTop.hidden = false;
    viewRoot.hidden = true;
    viewRoot.innerHTML = "";
    contentFrame.hidden = false;
    const next = uiUrl(src);
    if (contentFrame.getAttribute("src") !== next) {
      contentFrame.src = next;
    }
  }

  function showDesign() {
    pageTop.hidden = false;
    viewRoot.hidden = false;
    viewRoot.classList.add("wb-view--frame");
    contentFrame.hidden = true;
    if (contentFrame.getAttribute("src")) contentFrame.removeAttribute("src");
    const designUrl = `${ontologyBaseUrl.replace(/\/$/, "")}/workbench/index.html#/design?embed=1`;
    viewRoot.innerHTML = `
      <iframe class="pdmg-content-frame" title="Architecture Design Wizard" src="${escapeHtml(designUrl)}"></iframe>`;
  }

  function renderProgramCard(programId, items) {
    if (STATIC_PROGRAM_IDS.has(programId)) return "";
    const links = items
      .map((tx) => `<a href="${uiUrl("/" + programId + "/index.html")}">${escapeHtml(tx.id)} · ${escapeHtml(tx.name)}</a>`)
      .join("");
    return `
      <article class="wb-hub-card">
        <h3>${escapeHtml(programId)} <span class="group-tag">${items.length}건</span></h3>
        <p>${escapeHtml(items[0].description || "")}</p>
        <div class="links">${links}</div>
      </article>`;
  }

  async function renderHome() {
    showHome();
    viewRoot.innerHTML = `<p class="wb-empty">로딩 중…</p>`;
    try {
      const [txRes, configRes] = await Promise.all([
        fetch(uiUrl("/api/transactions")),
        fetch(uiUrl("/api/config"))
      ]);
      const transactions = await txRes.json();
      const config = await configRes.json();
      targetBaseUrl = config.targetBaseUrl || targetBaseUrl;
      ontologyBaseUrl = config.ontologyBaseUrl || ontologyBaseUrl;
      jwtBaseUrl = config.jwtBaseUrl || jwtBaseUrl;
      targetInfo.textContent = `pdmg ${targetBaseUrl} · jwt ${jwtBaseUrl} · ontology ${ontologyBaseUrl}`;

      const grouped = new Map();
      transactions.forEach((tx) => {
        if (!grouped.has(tx.programId)) grouped.set(tx.programId, []);
        grouped.get(tx.programId).push(tx);
      });
      const dynamicCards = [...grouped]
        .map(([programId, items]) => renderProgramCard(programId, items))
        .filter(Boolean)
        .join("");

      viewRoot.innerHTML = `
        <section class="wb-grid">
          <a class="wb-stat" href="#/design">
            <div class="wb-stat__label">Architecture Design</div>
            <div class="wb-stat__value">07</div>
            <div class="wb-stat__hint">Wizard · ServiceId / Table / Gate →</div>
          </a>
          <a class="wb-stat" href="#/mgcoa5530">
            <div class="wb-stat__label">전문 테스트</div>
            <div class="wb-stat__value">${STATIC_PROGRAM_IDS.size}</div>
            <div class="wb-stat__hint">등록 Program →</div>
          </a>
          <a class="wb-stat" href="#/imagelog">
            <div class="wb-stat__label">이미지로그</div>
            <div class="wb-stat__value">관리</div>
            <div class="wb-stat__hint">조건검색 UI →</div>
          </a>
          <a class="wb-stat" href="#/txparam">
            <div class="wb-stat__label">거래 파라미터</div>
            <div class="wb-stat__value">관리</div>
            <div class="wb-stat__hint">그리드 UI →</div>
          </a>
          <a class="wb-stat" href="#/txcontrol">
            <div class="wb-stat__label">거래통제</div>
            <div class="wb-stat__value">관리</div>
            <div class="wb-stat__hint">유형·차단 UI →</div>
          </a>
          <a class="wb-stat" href="#/jwt-token">
            <div class="wb-stat__label">JWT</div>
            <div class="wb-stat__value">포털</div>
            <div class="wb-stat__hint">pdmg-jwt 직결 →</div>
          </a>
        </section>

        <section class="wb-split" style="margin-top:1rem">
          <div class="wb-panel">
            <h2>Architecture Design</h2>
            <p class="wb-empty">tcf-ontology-service Workbench와 동일 Wizard입니다. Done 시 Dashboard Designs(PROPOSED)에 저장됩니다.</p>
            <div class="wb-actions">
              <a class="wb-btn primary" href="#/design">07 · Design 열기</a>
              <a class="wb-btn" href="${escapeHtml(ontologyBaseUrl)}/workbench/index.html#/dashboard?view=designs" target="_blank" rel="noopener">Dashboard Designs ↗</a>
            </div>
          </div>
          <div class="wb-panel">
            <h2>대상</h2>
            <p class="wb-empty">전문 테스트 → <code>${escapeHtml(targetBaseUrl)}</code></p>
            <p class="wb-empty">JWT → <code>${escapeHtml(jwtBaseUrl)}</code></p>
            <p class="wb-empty">Ontology → <code>${escapeHtml(ontologyBaseUrl)}</code></p>
          </div>
        </section>

        <section class="wb-panel" style="margin-top:1rem">
          <h2>서비스 카드</h2>
          <p class="wb-empty">프로그램별 전문 테스트·관리 화면</p>
          <div class="wb-hub-grid" id="programGrid">
            <article class="wb-hub-card">
              <h3>JWT <span class="group-tag">인증</span></h3>
              <p>pdmg-jwt 로그인·토큰 현황·Refresh·보안정책·JWKS. 브라우저가 :8110 을 직접 호출합니다.</p>
              <div class="links">
                <a href="#/jwt">로그인</a>
                <a href="#/jwt-token">토큰 현황</a>
                <a href="#/jwt-login-history">로그인 이력</a>
                <a href="#/jwt-refresh-token">Refresh Token</a>
                <a href="#/jwt-security-policy">보안정책</a>
                <a href="#/jwt-jwks">JWK 공개키</a>
              </div>
            </article>
            <article class="wb-hub-card">
              <h3>mgcoa9000 <span class="group-tag">거래 파라미터</span></h3>
              <p>TB_MG_TX_PARAM CRUD. 검색·추가·수정·삭제 관리 화면과 전문 테스트를 제공합니다.</p>
              <div class="links">
                <a href="#/mgcoa9000">전문 테스트 (S0/C0/U0/D0)</a>
                <a href="#/txparam">관리 화면 (그리드 UI)</a>
              </div>
            </article>
            <article class="wb-hub-card">
              <h3>mgcoa9001 <span class="group-tag">거래통제</span></h3>
              <p>TB_MG_TX_CONTROL CRUD. OM 유형(GLOBAL/BUSINESS/SERVICE/…)·대상·차단 규칙 관리.</p>
              <div class="links">
                <a href="#/mgcoa9001">전문 테스트 (S0/C0/U0/D0)</a>
                <a href="#/txcontrol">관리 화면 (거래통제 UI)</a>
              </div>
            </article>
            <article class="wb-hub-card">
              <h3>mgcoa9100 <span class="group-tag">런타임 진단</span></h3>
              <p>OM 진단 순서 가이드 대응. JVM/Thread/DB Pool 스냅샷·PRIMARY 원인판정·0→6단계 위저드.</p>
              <div class="links">
                <a href="#/mgcoa9100">전문 테스트 (S0)</a>
                <a href="#/rtdiag">진단 가이드 (순차 위저드)</a>
              </div>
            </article>
            <article class="wb-hub-card">
              <h3>mgcoa8888 <span class="group-tag">이미지로그</span></h3>
              <p>TB_FW_IMAGE_LOG 조회/삭제. withinSeconds·minElapsedSeconds·예외 조건 지원.</p>
              <div class="links">
                <a href="#/mgcoa8888">전문 테스트 (S0/D0)</a>
                <a href="#/imagelog">관리 화면 (조건검색 UI)</a>
              </div>
            </article>
            <article class="wb-hub-card">
              <h3>mgcoa5530 <span class="group-tag">조회</span></h3>
              <p>마케팅희망고객 / 안내항목 목록 조회. Handler → Facade → Service.</p>
              <div class="links"><a href="#/mgcoa5530">전문 테스트 (S0)</a></div>
            </article>
            <article class="wb-hub-card">
              <h3>mgcoa9999 <span class="group-tag">조회</span></h3>
              <p>영업팁 실적 목록 조회. dto.salzTipKdc 를 비우면 전체 조회.</p>
              <div class="links"><a href="#/mgcoa9999">전문 테스트 (S0)</a></div>
            </article>
            ${dynamicCards}
          </div>
        </section>`;
    } catch (e) {
      viewRoot.innerHTML = `<div class="wb-error">Home 로딩 실패: ${escapeHtml(e.message)}</div>`;
      if (window.PdmgErrorPopup) {
        PdmgErrorPopup.showSimple("거래 목록 로드 실패: " + e.message);
      }
    }
  }

  async function ensureConfig() {
    try {
      const config = await fetch(uiUrl("/api/config")).then((r) => r.json());
      targetBaseUrl = config.targetBaseUrl || targetBaseUrl;
      ontologyBaseUrl = config.ontologyBaseUrl || ontologyBaseUrl;
      jwtBaseUrl = config.jwtBaseUrl || jwtBaseUrl;
      targetInfo.textContent = `pdmg ${targetBaseUrl} · jwt ${jwtBaseUrl} · ontology ${ontologyBaseUrl}`;
    } catch (_) {
      targetInfo.textContent = `pdmg ${targetBaseUrl} · jwt ${jwtBaseUrl} · ontology ${ontologyBaseUrl}`;
    }
  }

  function requireShellAuth(route) {
    if (PUBLIC_ROUTES.has(route)) {
      return true;
    }
    if (window.PdmgServiceClient && typeof window.PdmgServiceClient.isAccessTokenValid === "function") {
      if (window.PdmgServiceClient.isAccessTokenValid()) {
        return true;
      }
      window.PdmgServiceClient.redirectToLogin();
      return false;
    }
    try {
      const raw = sessionStorage.getItem("pdmg.jwt.session");
      const session = raw ? JSON.parse(raw) : null;
      if (session && session.accessToken) {
        return true;
      }
    } catch (_e) {
      /* ignore */
    }
    location.hash = "#/jwt";
    return false;
  }

  async function render() {
    const { route } = parseHash();
    const safe = META[route] ? route : "home";
    if (!requireShellAuth(safe)) {
      return;
    }
    setActiveNav(safe);
    setPageMeta(safe);

    if (safe === "home") {
      await renderHome();
      return;
    }
    if (safe === "design") {
      await ensureConfig();
      setPageMeta("design");
      showDesign();
      return;
    }
    if (VIEW_SRC[safe]) {
      await ensureConfig();
      showFrame(VIEW_SRC[safe]);
      return;
    }
    location.hash = "#/home";
  }

  window.addEventListener("click", (ev) => {
    const link = ev.target.closest("a[href^='#/']");
    if (!link) return;
    // let hashchange handle navigation
  });

  window.addEventListener("hashchange", () => {
    render();
  });

  if (!location.hash || location.hash === "#") {
    location.hash = "#/home";
  } else {
    render();
  }
})();
