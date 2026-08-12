(function () {
  const root = document.getElementById("viewRoot");
  const pageTitle = document.getElementById("pageTitle");
  const pageDesc = document.getElementById("pageDesc");
  const toast = document.getElementById("toast");
  const evidenceDrawer = document.getElementById("evidenceDrawer");
  const evidenceBody = document.getElementById("evidenceBody");
  const drawerBackdrop = document.getElementById("drawerBackdrop");

  const META = {
    home: {
      title: "Architect Home",
      desc: "Ontology 상태와 아키텍처 품질을 한눈에 확인합니다."
    },
    catalog: {
      title: "Architecture Catalog",
      desc: "Ontology Graph에 등록된 Architecture Object(Concept) 전체 목록을 조회합니다."
    },
    dashboard: {
      title: "Dashboard Detail",
      desc: "Concepts / Relations / Programs / ServiceIds / Rules FAIL 상세를 조회합니다."
    },
    search: {
      title: "Architecture Search",
      desc: "ServiceId / Program / Handler / Table로 구조를 조회합니다."
    },
    impact: {
      title: "Impact Analysis",
      desc: "변경 대상(Table) 기준 End-to-End 영향도를 분석합니다."
    },
    gate: {
      title: "Architecture Gate",
      desc: "Architecture Rule(RULE-001~006) 준수 여부를 검증합니다."
    },
    design: {
      title: "Architecture Design",
      desc: "ServiceId / Table / Application / Policy를 Wizard로 설계하고 Cursor Context를 만듭니다."
    },
    qna: {
      title: "Architecture QnA",
      desc: "exearchidoc Architecture Knowledge 문서를 근거로 질문에 답합니다."
    },
    knowledge: {
      title: "Architecture Knowledge",
      desc: "docs/knowledge/exearchidoc 아키텍처 지식 문서를 탐색합니다."
    }
  };

  function showToast(msg) {
    toast.hidden = !msg;
    toast.textContent = msg || "";
  }

  function escapeHtml(v) {
    return String(v ?? "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function parseHash() {
    const raw = (location.hash || "#/home").replace(/^#\/?/, "");
    const [pathPart, queryPart] = raw.split("?");
    const route = (pathPart || "home").split("/")[0] || "home";
    const params = new URLSearchParams(queryPart || "");
    return { route, params };
  }

  function navigate(route, params = {}) {
    const qs = new URLSearchParams(params).toString();
    location.hash = `#/${route}${qs ? "?" + qs : ""}`;
  }

  function setActiveNav(route) {
    const nav = route === "dashboard" ? "home" : route;
    document.querySelectorAll(".wb-menu__link").forEach((a) => {
      a.classList.toggle("is-active", a.dataset.route === nav);
    });
  }

  function openEvidence(payload) {
    const rows = [];
    const push = (k, v) => {
      if (v !== undefined && v !== null && v !== "") rows.push(`<dt>${escapeHtml(k)}</dt><dd>${escapeHtml(v)}</dd>`);
    };
    if (!payload) {
      evidenceBody.innerHTML = '<p class="wb-empty">Evidence 없음</p>';
    } else if (payload.provenance) {
      const p = payload.provenance;
      push("Source Type", p.sourceType);
      push("Source System", p.sourceSystem);
      push("Source Path", p.sourcePath || p.sourceDocument);
      push("Discovered By", p.discoveredBy);
      push("Status", p.verificationStatus);
      push("Extracted At", p.extractedAt);
      evidenceBody.innerHTML = `<dl class="wb-kv">${rows.join("")}</dl><pre class="wb-chain">${escapeHtml(JSON.stringify(payload, null, 2))}</pre>`;
    } else {
      evidenceBody.innerHTML = `<pre class="wb-chain">${escapeHtml(JSON.stringify(payload, null, 2))}</pre>`;
    }
    evidenceDrawer.hidden = false;
    drawerBackdrop.hidden = false;
  }

  function closeEvidence() {
    evidenceDrawer.hidden = true;
    drawerBackdrop.hidden = true;
  }

  document.getElementById("evidenceClose").addEventListener("click", closeEvidence);
  drawerBackdrop.addEventListener("click", closeEvidence);

  document.getElementById("globalSearchForm").addEventListener("submit", (e) => {
    e.preventDefault();
    const q = document.getElementById("globalQuery").value.trim();
    if (!q) return;
    if (/^TB_/i.test(q) || q.includes("TABLE")) {
      navigate("impact", { table: q });
    } else {
      navigate("search", { q, type: "AUTO" });
    }
  });

  function detectType(q) {
    const raw = (q || "").trim();
    // ServiceId 11: mgcoa8888S0
    if (/^[a-z]{2}[a-z]{2}[a-z]\d{4}[SCUDAR][0-9A-Z]$/i.test(raw)) return "SERVICE";
    // Program 9: mgcoa8888
    if (/^[a-z]{2}[a-z]{2}[a-z]\d{4}$/i.test(raw)) return "PROGRAM";
    if (/^TB_/i.test(raw)) return "TABLE";
    if (/Handler$/i.test(raw) || raw.includes(".")) return "HANDLER";
    return "SERVICE";
  }

  /* ---------- HOME ---------- */
  async function renderHome() {
    root.innerHTML = `<p class="wb-empty">카탈로그 로딩 중…</p>`;
    try {
      const [catalog, consistency, rules, dash] = await Promise.all([
        OntologyApi.catalog(),
        OntologyApi.consistency().catch(() => null),
        OntologyApi.validateRules().catch(() => null),
        OntologyApi.dashboardSummary().catch(() => null)
      ]);
      const g = catalog.graph || {};
      const findings = (rules && (rules.findings || rules.results || rules.rules)) || [];
      const pass = Array.isArray(findings)
        ? findings.filter((f) => String(f.verdict || f.status || f.result || "").toUpperCase() === "PASS").length
        : (rules && rules.passed) || 0;
      const fail = Array.isArray(findings)
        ? findings.filter((f) => {
            const s = String(f.verdict || f.status || f.result || "").toUpperCase();
            return s === "FAIL" || s === "FAILED";
          }).length
        : (rules && (rules.failCount || rules.failed)) || 0;
      const designSessions = dash?.designSessions ?? 0;

      const byType = g.byType || {};
      const typeChips = Object.entries(byType)
        .filter(([, n]) => Number(n) > 0)
        .map(([t, n]) => `<a class="wb-pill" href="#/catalog?type=${encodeURIComponent(t)}">${escapeHtml(t)} · ${n}</a>`)
        .join(" ");

      root.innerHTML = `
        <section class="wb-grid">
          <a class="wb-stat wb-stat--link" href="#/dashboard?view=concepts">
            <div class="wb-stat__label">Concepts</div>
            <div class="wb-stat__value">${g.conceptCount ?? "–"}</div>
            <div class="wb-stat__hint">상세 보기 →</div>
          </a>
          <a class="wb-stat wb-stat--link" href="#/dashboard?view=relations">
            <div class="wb-stat__label">Relations</div>
            <div class="wb-stat__value">${g.relationCount ?? "–"}</div>
            <div class="wb-stat__hint">상세 보기 →</div>
          </a>
          <a class="wb-stat wb-stat--link" href="#/dashboard?view=programs">
            <div class="wb-stat__label">Programs</div>
            <div class="wb-stat__value">${g.programsInGraph ?? (catalog.programs || []).length}</div>
            <div class="wb-stat__hint">상세 보기 →</div>
          </a>
          <a class="wb-stat wb-stat--link" href="#/dashboard?view=services">
            <div class="wb-stat__label">ServiceIds</div>
            <div class="wb-stat__value">${g.servicesInGraph ?? (catalog.services || []).length}</div>
            <div class="wb-stat__hint">상세 보기 →</div>
          </a>
          <a class="wb-stat wb-stat--link" href="#/dashboard?view=designs">
            <div class="wb-stat__label">Designs (PROPOSED)</div>
            <div class="wb-stat__value">${designSessions}</div>
            <div class="wb-stat__hint">Wizard Done 저장분 →</div>
          </a>
          <a class="wb-stat wb-stat--link ${fail > 0 ? "wb-stat--fail" : ""}" href="#/dashboard?view=rules-fail">
            <div class="wb-stat__label">Rules FAIL</div>
            <div class="wb-stat__value">${fail}</div>
            <div class="wb-stat__hint">상세 보기 →</div>
          </a>
        </section>
        <section class="wb-panel" style="margin-top:1rem">
          <h2>Architecture Objects · ${g.conceptCount ?? "–"}</h2>
          <p class="wb-empty">Ontology Graph Concept 전체. 타입별 바로가기:</p>
          <div class="wb-actions" style="flex-wrap:wrap">${typeChips || "<span class='wb-empty'>타입 집계 없음</span>"}</div>
          <div class="wb-actions" style="margin-top:0.75rem">
            <a class="wb-btn primary" href="#/catalog">전체 Catalog 열기 (${g.conceptCount ?? "–"})</a>
          </div>
        </section>
        <section class="wb-split">
          <div class="wb-panel">
            <h2>Architecture Health</h2>
            <dl class="wb-kv">
              <dt>Rule PASS</dt><dd>${pass}</dd>
              <dt>Rule FAIL</dt><dd>${fail}</dd>
              <dt>Consistency</dt><dd>${consistency ? escapeHtml(JSON.stringify(consistency.status || consistency.ok || "loaded")) : "n/a"}</dd>
              <dt>Unified Catalog</dt><dd>${catalog.unified ? "true" : "false"}</dd>
            </dl>
          </div>
          <div class="wb-panel">
            <h2>Quick Actions</h2>
            <div class="wb-actions">
              <a class="wb-btn" href="#/catalog">Architecture Catalog</a>
              <a class="wb-btn" href="#/qna">Architecture QnA</a>
              <a class="wb-btn" href="#/knowledge">Architecture Knowledge</a>
              <a class="wb-btn" href="#/search?q=mgcoa8888S0&type=SERVICE">Service 구조 조회</a>
              <a class="wb-btn" href="#/impact?table=TB_FW_IMAGE_LOG">Table 영향도</a>
              <a class="wb-btn" href="#/gate">Architecture Gate</a>
              <a class="wb-btn" href="#/design?business=CO&type=QUERY">신규 설계</a>
            </div>
          </div>
        </section>
        <section class="wb-panel" style="margin-top:1rem">
          <h2>Golden Scenario</h2>
          <p>ServiceId <code>mgcoa8888S0</code> · Table <code>TB_FW_IMAGE_LOG</code></p>
        </section>`;
    } catch (e) {
      root.innerHTML = `<div class="wb-error">Home 로딩 실패: ${escapeHtml(e.message)}</div>`;
    }
  }

  /* ---------- DASHBOARD DETAIL ---------- */
  const DASHBOARD_VIEWS = [
    { id: "concepts", label: "Concepts" },
    { id: "relations", label: "Relations" },
    { id: "programs", label: "Programs" },
    { id: "services", label: "ServiceIds" },
    { id: "designs", label: "Designs (PROPOSED)" },
    { id: "rules-fail", label: "Rules FAIL" }
  ];

  async function renderDashboard(params) {
    const view = (params.get("view") || "concepts").toLowerCase();
    const q = (params.get("q") || "").trim();
    root.innerHTML = `<p class="wb-empty">Dashboard 상세 로딩 중…</p>`;
    try {
      const data = await OntologyApi.dashboardDetail(view, { q });
      const tabs = DASHBOARD_VIEWS.map((v) => {
        const href = `#/dashboard?view=${encodeURIComponent(v.id)}${q ? `&q=${encodeURIComponent(q)}` : ""}`;
        return `<a class="wb-pill ${v.id === view ? "is-on" : ""}" href="${href}">${escapeHtml(v.label)}</a>`;
      }).join(" ");

      const summaryChips = renderDashboardSummaryChips(data, view);
      const table = renderDashboardTable(view, data.items || []);

      root.innerHTML = `
        <section class="wb-panel">
          <div class="wb-actions" style="margin-bottom:0.75rem">
            <a class="wb-btn" href="#/home">← Dashboard Home</a>
          </div>
          <h2>${escapeHtml(data.title || view)}</h2>
          <p class="wb-empty">총 <strong>${escapeHtml(data.total ?? "–")}</strong>
            · 표시 <strong>${escapeHtml(data.count ?? (data.items || []).length)}</strong>
            ${data.ontologyCount != null ? ` · Ontology ${escapeHtml(data.ontologyCount)}` : ""}
            ${data.proposedCount != null ? ` · PROPOSED ${escapeHtml(data.proposedCount)}` : ""}
            ${data.sessionCount != null ? ` · sessions ${escapeHtml(data.sessionCount)}` : ""}
            ${data.status ? ` · status=<code>${escapeHtml(data.status)}</code>` : ""}
            ${data.passCount != null ? ` · PASS ${escapeHtml(data.passCount)}` : ""}
            ${data.failCount != null ? ` · FAIL ${escapeHtml(data.failCount)}` : ""}
          </p>
          ${data.note ? `<p class="wb-empty">${escapeHtml(data.note)}</p>` : ""}
          <div class="wb-actions" style="flex-wrap:wrap;margin-bottom:0.75rem">${tabs}</div>
          ${summaryChips}
          <form class="wb-form-row" id="dashFilter">
            <input name="q" value="${escapeHtml(q)}" placeholder="검색 (id / name / rule / predicate)">
            <button class="wb-btn" type="submit">Filter</button>
          </form>
          ${table}
        </section>`;

      document.getElementById("dashFilter").addEventListener("submit", (e) => {
        e.preventDefault();
        const fd = new FormData(e.target);
        navigate("dashboard", { view, q: String(fd.get("q") || "").trim() });
      });

      root.querySelectorAll("[data-concept]").forEach((btn) => {
        btn.addEventListener("click", async () => {
          try {
            openEvidence(await OntologyApi.concept(btn.getAttribute("data-concept")));
          } catch (err) {
            showToast(err.message);
          }
        });
      });
      root.querySelectorAll("[data-json]").forEach((btn) => {
        btn.addEventListener("click", () => {
          try {
            openEvidence(JSON.parse(decodeURIComponent(btn.getAttribute("data-json"))));
          } catch (err) {
            showToast(err.message);
          }
        });
      });
    } catch (e) {
      root.innerHTML = `<div class="wb-error">Dashboard 상세 로딩 실패: ${escapeHtml(e.message)}</div>`;
    }
  }

  function renderDashboardSummaryChips(data, view) {
    if (view === "concepts" && data.byType) {
      return `<div class="wb-actions" style="flex-wrap:wrap;margin-bottom:0.75rem">${Object.entries(data.byType)
        .filter(([, n]) => Number(n) > 0)
        .map(([t, n]) => `<a class="wb-pill" href="#/catalog?type=${encodeURIComponent(t)}">${escapeHtml(t)} · ${n}</a>`)
        .join("")}</div>`;
    }
    if (view === "relations" && data.byPredicate) {
      return `<div class="wb-actions" style="flex-wrap:wrap;margin-bottom:0.75rem">${Object.entries(data.byPredicate)
        .map(([t, n]) => `<span class="wb-pill">${escapeHtml(t)} · ${n}</span>`)
        .join("")}</div>`;
    }
    return "";
  }

  function renderDashboardTable(view, items) {
    if (view === "concepts") {
      const rows = items.map((o) => {
        const link = catalogDeepLink(o);
        const name = link
          ? `<a href="${link}"><code>${escapeHtml(o.name || o.id)}</code></a>`
          : `<code>${escapeHtml(o.name || o.id)}</code>`;
        return `<tr>
          <td><span class="wb-pill">${escapeHtml(o.type)}</span></td>
          <td>${name}</td>
          <td><code class="wb-muted">${escapeHtml(o.id)}</code></td>
          <td>${escapeHtml(o.verificationStatus || "UNRESOLVED")}</td>
          <td>${escapeHtml(o.outgoingCount ?? 0)} / ${escapeHtml(o.incomingCount ?? 0)}</td>
          <td><button type="button" class="wb-btn" data-concept="${escapeHtml(o.id)}">Detail</button></td>
        </tr>`;
      }).join("");
      return tableWrap(["Type", "Name", "Concept Id", "Evidence", "Out/In", ""], rows);
    }

    if (view === "relations") {
      const rows = items.map((o) => `<tr>
        <td><span class="wb-pill">${escapeHtml(o.predicate)}</span></td>
        <td><code>${escapeHtml(o.fromName)}</code><div class="wb-muted">${escapeHtml(o.fromType)}</div></td>
        <td><code>${escapeHtml(o.toName)}</code><div class="wb-muted">${escapeHtml(o.toType)}</div></td>
        <td>${escapeHtml(o.graphType)}</td>
        <td>${escapeHtml(o.verificationStatus || "UNRESOLVED")}</td>
        <td><button type="button" class="wb-btn" data-json="${encodeURIComponent(JSON.stringify(o))}">Detail</button></td>
      </tr>`).join("");
      return tableWrap(["Predicate", "From", "To", "Graph", "Evidence", ""], rows);
    }

    if (view === "programs") {
      const rows = items.map((o) => {
        const services = Array.isArray(o.services) ? o.services.join(", ") : "";
        const proposed = o.source === "DESIGN_WIZARD" || o.verificationStatus === "PROPOSED";
        const detailBtn = proposed
          ? `<button type="button" class="wb-btn" data-json="${encodeURIComponent(JSON.stringify(o))}">Detail</button>`
          : `<button type="button" class="wb-btn" data-concept="${escapeHtml(o.id)}">Detail</button>`;
        return `<tr>
          <td><a href="#/search?q=${encodeURIComponent(o.programId)}&type=PROGRAM"><code>${escapeHtml(o.programId)}</code></a>
            ${proposed ? `<div class="wb-muted">DESIGN_WIZARD</div>` : ""}</td>
          <td>${escapeHtml(o.title || "")}</td>
          <td>${escapeHtml(o.businessCode || "")}/${escapeHtml(o.functionCode || "")}</td>
          <td><code>${escapeHtml(o.table || "UNRESOLVED")}</code></td>
          <td>${escapeHtml(o.serviceCount ?? 0)}<div class="wb-muted">${escapeHtml(services)}</div></td>
          <td><span class="wb-pill ${proposed ? "is-on" : ""}">${escapeHtml(o.verificationStatus || "UNRESOLVED")}</span></td>
          <td>${detailBtn}</td>
        </tr>`;
      }).join("");
      return tableWrap(["Program", "Title", "Biz/Fn", "Table", "Services", "Evidence", ""], rows);
    }

    if (view === "services") {
      const rows = items.map((o) => {
        const proposed = o.source === "DESIGN_WIZARD" || o.verificationStatus === "PROPOSED";
        const detailBtn = proposed
          ? `<button type="button" class="wb-btn" data-json="${encodeURIComponent(JSON.stringify(o))}">Detail</button>`
          : `<button type="button" class="wb-btn" data-concept="${escapeHtml(o.id)}">Detail</button>`;
        return `<tr>
        <td><a href="#/search?q=${encodeURIComponent(o.serviceId)}&type=SERVICE"><code>${escapeHtml(o.serviceId)}</code></a>
          ${proposed ? `<div class="wb-muted">DESIGN_WIZARD</div>` : ""}</td>
        <td>${escapeHtml(o.op || "")}</td>
        <td><code>${escapeHtml(o.programId)}</code></td>
        <td><code>${escapeHtml(o.handler || "UNRESOLVED")}</code></td>
        <td><span class="wb-pill ${proposed ? "is-on" : ""}">${escapeHtml(o.verificationStatus || "UNRESOLVED")}</span></td>
        <td>${detailBtn}</td>
      </tr>`;
      }).join("");
      return tableWrap(["ServiceId", "Op", "Program", "Handler", "Evidence", ""], rows);
    }

    if (view === "designs") {
      const rows = items.map((o) => {
        const name = o.name || o.physicalName || o.tableName || o.title || o.sessionId || "";
        return `<tr>
          <td><span class="wb-pill is-on">${escapeHtml(o.kind || "")}</span></td>
          <td><code>${escapeHtml(name)}</code></td>
          <td><code>${escapeHtml(o.serviceId || "")}</code></td>
          <td><code>${escapeHtml(o.programId || "")}</code></td>
          <td><span class="wb-pill is-on">${escapeHtml(o.verificationStatus || "PROPOSED")}</span></td>
          <td>${escapeHtml(o.gateStatus || o.status || "")}</td>
          <td><code class="wb-muted">${escapeHtml(o.sessionId || "")}</code></td>
          <td><button type="button" class="wb-btn" data-json="${encodeURIComponent(JSON.stringify(o))}">Detail</button></td>
        </tr>`;
      }).join("");
      return `
        ${tableWrap(["Kind", "Name", "ServiceId", "Program", "Status", "Gate", "Session", ""], rows)}
        <p class="wb-empty" style="margin-top:0.75rem"><a class="wb-btn" href="#/design">Architecture Design 계속</a></p>`;
    }

    // rules-fail
    const rows = items.map((o) => {
      const pill = "wb-pill wb-pill--fail";
      return `<tr>
        <td><code>${escapeHtml(o.ruleId || "")}</code></td>
        <td><code>${escapeHtml(o.target || "")}</code></td>
        <td>${escapeHtml(o.message || "")}</td>
        <td><span class="${pill}">${escapeHtml(o.verdict || "FAIL")}</span></td>
        <td><button type="button" class="wb-btn" data-json="${encodeURIComponent(JSON.stringify(o))}">Detail</button></td>
      </tr>`;
    }).join("");
    return `
      ${tableWrap(["Rule", "Target", "Message", "Verdict", ""], rows)}
      <p class="wb-empty" style="margin-top:0.75rem"><a class="wb-btn" href="#/gate">Architecture Gate 전체 보기</a></p>`;
  }

  function tableWrap(headers, rows) {
    return `<div class="wb-table-wrap"><table class="wb-table">
      <thead><tr>${headers.map((h) => `<th>${escapeHtml(h)}</th>`).join("")}</tr></thead>
      <tbody>${rows || `<tr><td colspan="${headers.length}">항목 없음</td></tr>`}</tbody>
    </table></div>`;
  }

  /* ---------- CATALOG (101 Architecture Objects) ---------- */
  async function renderCatalog(params) {
    const type = (params.get("type") || "ALL").toUpperCase();
    const q = (params.get("q") || "").trim();
    root.innerHTML = `<p class="wb-empty">Architecture Objects 로딩 중…</p>`;
    try {
      const data = await OntologyApi.concepts({ type: type === "ALL" ? "" : type, q });
      const byType = data.byType || {};
      const objects = data.objects || [];
      const typeOpts = ["ALL", ...Object.keys(byType)]
        .map((t) => {
          const n = t === "ALL" ? data.totalConcepts : byType[t];
          return `<option value="${escapeHtml(t)}" ${t === type ? "selected" : ""}>${escapeHtml(t)} (${n ?? 0})</option>`;
        })
        .join("");

      const typeSummary = Object.entries(byType)
        .map(([t, n]) => `<button type="button" class="wb-pill ${t === type ? "is-on" : ""}" data-type="${escapeHtml(t)}">${escapeHtml(t)} · ${n}</button>`)
        .join("");

      const rows = objects
        .map((o) => {
          const link = catalogDeepLink(o);
          const nameCell = link
            ? `<a href="${link}"><code>${escapeHtml(o.name || o.id)}</code></a>`
            : `<code>${escapeHtml(o.name || o.id)}</code>`;
          return `<tr>
            <td><span class="wb-pill">${escapeHtml(o.type)}</span></td>
            <td>${nameCell}</td>
            <td><code class="wb-muted">${escapeHtml(o.id)}</code></td>
            <td>${escapeHtml(o.verificationStatus || "UNRESOLVED")}</td>
            <td>${escapeHtml(o.outgoingCount ?? 0)} / ${escapeHtml(o.incomingCount ?? 0)}</td>
            <td>
              <button type="button" class="wb-btn" data-concept="${escapeHtml(o.id)}">Detail</button>
            </td>
          </tr>`;
        })
        .join("");

      root.innerHTML = `
        <section class="wb-panel">
          <h2>Ontology Architecture Objects</h2>
          <p class="wb-empty">총 Concept <strong>${escapeHtml(data.totalConcepts)}</strong> · Relation <strong>${escapeHtml(data.totalRelations)}</strong>
            · 현재 표시 <strong>${escapeHtml(data.count)}</strong></p>
          <div class="wb-actions" style="flex-wrap:wrap;margin-bottom:0.75rem">${typeSummary}</div>
          <form class="wb-form-row" id="catalogFilter">
            <label>Type<select name="type">${typeOpts}</select></label>
            <input name="q" value="${escapeHtml(q)}" placeholder="id / name 검색 (예: mgcoa, TB_, Handler)">
            <button class="wb-btn" type="submit">Filter</button>
          </form>
          <div class="wb-table-wrap">
            <table class="wb-table">
              <thead>
                <tr>
                  <th>Type</th><th>Name</th><th>Concept Id</th><th>Evidence</th><th>Out/In</th><th></th>
                </tr>
              </thead>
              <tbody>${rows || '<tr><td colspan="6">객체 없음</td></tr>'}</tbody>
            </table>
          </div>
        </section>`;

      document.getElementById("catalogFilter").addEventListener("submit", (e) => {
        e.preventDefault();
        const fd = new FormData(e.target);
        navigate("catalog", {
          type: String(fd.get("type") || "ALL"),
          q: String(fd.get("q") || "").trim()
        });
      });
      root.querySelectorAll("[data-type]").forEach((btn) => {
        btn.addEventListener("click", () => navigate("catalog", { type: btn.getAttribute("data-type"), q }));
      });
      root.querySelectorAll("[data-concept]").forEach((btn) => {
        btn.addEventListener("click", async () => {
          const id = btn.getAttribute("data-concept");
          try {
            const detail = await OntologyApi.concept(id);
            openEvidence(detail);
          } catch (err) {
            showToast(`Concept 조회 실패: ${err.message}`);
          }
        });
      });
    } catch (e) {
      root.innerHTML = `<div class="wb-error">Catalog 로딩 실패: ${escapeHtml(e.message)}</div>`;
    }
  }

  function catalogDeepLink(o) {
    const type = String(o.type || "").toUpperCase();
    const name = o.name || "";
    if (type === "SERVICE_ID") return `#/search?q=${encodeURIComponent(name)}&type=SERVICE`;
    if (type === "PROGRAM") return `#/search?q=${encodeURIComponent(name)}&type=PROGRAM`;
    if (type === "TABLE") return `#/impact?table=${encodeURIComponent(name)}`;
    if (type === "COMPONENT" && /Handler$/i.test(name)) {
      return `#/search?q=${encodeURIComponent(name)}&type=HANDLER`;
    }
    return null;
  }

  /* ---------- SEARCH ---------- */
  async function renderSearch(params) {
    const q = (params.get("q") || "").trim();
    const type = (params.get("type") || "AUTO").toUpperCase();
    root.innerHTML = `
      <form class="wb-form-row" id="searchForm">
        <label>검색유형
          <select name="type">
            <option value="AUTO"${type === "AUTO" ? " selected" : ""}>AUTO</option>
            <option value="SERVICE"${type === "SERVICE" ? " selected" : ""}>SERVICE</option>
            <option value="PROGRAM"${type === "PROGRAM" ? " selected" : ""}>PROGRAM</option>
            <option value="HANDLER"${type === "HANDLER" ? " selected" : ""}>HANDLER</option>
            <option value="TABLE"${type === "TABLE" ? " selected" : ""}>TABLE</option>
          </select>
        </label>
        <input name="q" value="${escapeHtml(q)}" placeholder="mgcoa8888S0" required>
        <button class="wb-btn" type="submit">조회</button>
      </form>
      <div id="searchResult"><p class="wb-empty">${q ? "조회 중…" : "검색어를 입력하세요. Golden: mgcoa8888S0"}</p></div>`;

    document.getElementById("searchForm").addEventListener("submit", (e) => {
      e.preventDefault();
      const fd = new FormData(e.target);
      navigate("search", { q: String(fd.get("q") || "").trim(), type: String(fd.get("type") || "AUTO") });
    });

    if (!q) return;
    const box = document.getElementById("searchResult");
    const resolved = type === "AUTO" ? detectType(q) : type;
    try {
      if (resolved === "TABLE") {
        navigate("impact", { table: q });
        return;
      }
      if (resolved === "PROGRAM") {
        const data = await OntologyApi.programServices(q);
        box.innerHTML = renderProgramResult(data);
        return;
      }
      if (resolved === "HANDLER") {
        const data = await OntologyApi.handlerServices(q);
        box.innerHTML = renderHandlerResult(data);
        return;
      }
      const [structure, tables, rules] = await Promise.all([
        OntologyApi.serviceStructure(q),
        OntologyApi.serviceTables(q).catch(() => null),
        OntologyApi.validateRules().catch(() => null)
      ]);
      box.innerHTML = renderServiceResult(structure, tables, rules);
      wireSearchTabs(structure, tables, rules);
    } catch (e) {
      box.innerHTML = `<div class="wb-error">${escapeHtml(e.message)}
        <p>미등록이라면 mapping seed / YAML 적재를 확인하세요.</p></div>`;
    }
  }

  function classificationText(classification) {
    if (!Array.isArray(classification) || !classification.length) return "(없음)";
    return classification
      .map((s) => `${(s.from || "").replace(/^.*:/, "")} -${s.predicate}-> ${(s.to || "").replace(/^.*:/, "")}`)
      .join("\n");
  }

  function structureChain(summary, structure) {
    if (Array.isArray(summary) && summary.length) return summary.join(" → ");
    if (!Array.isArray(structure)) return "";
    const names = [];
    structure.forEach((s, i) => {
      if (i === 0 && s.fromName) names.push(s.fromName);
      if (s.toName) names.push(s.toName);
    });
    return names.join(" → ");
  }

  function firstProvenance(structure) {
    if (!Array.isArray(structure)) return null;
    const hit = structure.find((s) => s.provenance);
    return hit ? { step: hit, provenance: hit.provenance } : null;
  }

  function renderServiceResult(structure, tables, rules) {
    const sid = structure.serviceId || structure.concept?.name || "";
    const chain = structureChain(structure.summary, structure.structure);
    const cls = classificationText(structure.classification);
    return `
      <div class="wb-panel">
        <div style="display:flex;justify-content:space-between;gap:1rem;align-items:center;flex-wrap:wrap">
          <h2 style="margin:0">ServiceId : <span class="wb-mono">${escapeHtml(sid)}</span></h2>
          <span class="wb-pill wb-pill--ok">GRAPH</span>
        </div>
        <h3>업무분류</h3>
        <pre class="wb-chain">${escapeHtml(cls)}</pre>
        <h3>처리 구조</h3>
        <pre class="wb-chain">${escapeHtml(chain || "(empty)")}</pre>
        <div class="wb-actions">
          <a class="wb-btn" href="#/impact?table=${encodeURIComponent((tables?.tables?.[0]?.name) || "TB_FW_IMAGE_LOG")}">영향도</a>
          <button type="button" class="wb-btn wb-btn--ghost" id="btnEvidence">Evidence</button>
        </div>
        <div class="wb-tabs" id="searchTabs">
          <button type="button" class="wb-tab is-active" data-tab="overview">Overview</button>
          <button type="button" class="wb-tab" data-tab="structure">Structure</button>
          <button type="button" class="wb-tab" data-tab="tables">Tables</button>
          <button type="button" class="wb-tab" data-tab="runtime">Runtime</button>
          <button type="button" class="wb-tab" data-tab="rules">Rules</button>
          <button type="button" class="wb-tab" data-tab="evidence">Evidence</button>
        </div>
        <div id="tabPane"></div>
      </div>`;
  }

  function wireSearchTabs(structure, tables, rules) {
    const pane = document.getElementById("tabPane");
    const evidence = firstProvenance(structure.structure);
    document.getElementById("btnEvidence")?.addEventListener("click", () => openEvidence(evidence || structure.concept));
    const renderTab = async (tab) => {
      document.querySelectorAll("#searchTabs .wb-tab").forEach((b) => b.classList.toggle("is-active", b.dataset.tab === tab));
      if (tab === "overview") {
        pane.innerHTML = `<dl class="wb-kv">
          <dt>serviceId</dt><dd>${escapeHtml(structure.serviceId)}</dd>
          <dt>conceptId</dt><dd>${escapeHtml(structure.concept?.id)}</dd>
          <dt>summary</dt><dd>${escapeHtml((structure.summary || []).join(" → "))}</dd>
        </dl>`;
      } else if (tab === "structure") {
        pane.innerHTML = `<pre class="wb-chain">${escapeHtml(JSON.stringify(structure.structure || [], null, 2))}</pre>`;
      } else if (tab === "tables") {
        const list = tables?.tables || structure.tables || [];
        pane.innerHTML = list.length
          ? `<ul>${list.map((t) => `<li><a href="#/impact?table=${encodeURIComponent(t.name || t)}">${escapeHtml(t.name || t)}</a> <span class="wb-pill">${escapeHtml(t.type || "TABLE")}</span></li>`).join("")}</ul>`
          : `<p class="wb-empty">테이블 없음</p>`;
      } else if (tab === "runtime") {
        pane.innerHTML = `<p class="wb-empty">RUNTIME TX chain 로딩…</p>`;
        try {
          const rt = await OntologyApi.runtimeTxChain();
          pane.innerHTML = `<pre class="wb-chain">${escapeHtml((rt.summary || []).join(" → "))}</pre>`;
        } catch (e) {
          pane.innerHTML = `<div class="wb-error">${escapeHtml(e.message)}</div>`;
        }
      } else if (tab === "rules") {
        pane.innerHTML = renderRulesTable(rules);
      } else if (tab === "evidence") {
        pane.innerHTML = `<button type="button" class="wb-btn" id="openEv2">Evidence Drawer 열기</button>
          <pre class="wb-chain">${escapeHtml(JSON.stringify(evidence || structure.concept || {}, null, 2))}</pre>`;
        document.getElementById("openEv2")?.addEventListener("click", () => openEvidence(evidence || structure.concept));
      }
    };
    document.querySelectorAll("#searchTabs .wb-tab").forEach((btn) => {
      btn.addEventListener("click", () => renderTab(btn.dataset.tab));
    });
    renderTab("overview");
  }

  function renderProgramResult(data) {
    const services = data.services || [];
    return `<div class="wb-panel">
      <h2>Program : ${escapeHtml(data.program?.name || data.program?.id)}</h2>
      <ul>${services.map((s) => `<li><a href="#/search?q=${encodeURIComponent(s.name || s.id)}&type=SERVICE">${escapeHtml(s.name || s.id)}</a></li>`).join("") || "<li class='wb-empty'>서비스 없음</li>"}</ul>
    </div>`;
  }

  function renderHandlerResult(data) {
    const services = data.services || [];
    return `<div class="wb-panel">
      <h2>Handler : ${escapeHtml(data.handler?.name || data.handler?.id)}</h2>
      <ul>${services.map((s) => `<li><a href="#/search?q=${encodeURIComponent(s.name || s.id)}&type=SERVICE">${escapeHtml(s.name || s.id)}</a></li>`).join("") || "<li class='wb-empty'>서비스 없음</li>"}</ul>
      <button type="button" class="wb-btn wb-btn--ghost" id="handlerEv">Evidence</button>
    </div>`;
  }

  /* ---------- IMPACT ---------- */
  async function renderImpact(params) {
    const table = (params.get("table") || "").trim();
    root.innerHTML = `
      <form class="wb-form-row" id="impactForm">
        <label>변경대상유형
          <select name="kind"><option value="TABLE" selected>TABLE</option></select>
        </label>
        <input name="table" value="${escapeHtml(table)}" placeholder="TB_FW_IMAGE_LOG" required>
        <button class="wb-btn" type="submit">분석</button>
      </form>
      <div id="impactResult"><p class="wb-empty">${table ? "분석 중…" : "Golden: TB_FW_IMAGE_LOG"}</p></div>`;

    document.getElementById("impactForm").addEventListener("submit", (e) => {
      e.preventDefault();
      const fd = new FormData(e.target);
      navigate("impact", { table: String(fd.get("table") || "").trim() });
    });
    if (!table) return;

    const box = document.getElementById("impactResult");
    try {
      const data = await OntologyApi.impactTable(table);
      const t = data.table || {};
      if (String(t.type).toUpperCase() !== "TABLE") {
        box.innerHTML = `<div class="wb-error">데이터 오류: table.type=${escapeHtml(t.type)} (TABLE이어야 함). alias 충돌 가능.</div>
          <pre class="wb-chain">${escapeHtml(JSON.stringify(t, null, 2))}</pre>`;
        return;
      }
      const layers = [
        ["Mappers", data.affectedMappers],
        ["DAOs", data.affectedDaos],
        ["Services", data.affectedServices],
        ["Facades", data.affectedFacades],
        ["Handlers", data.affectedHandlers],
        ["ServiceIds", data.affectedServiceIds],
        ["Programs", data.affectedPrograms],
        ["Functions", data.affectedFunctions],
        ["Businesses", data.affectedBusinesses],
        ["Systems", data.affectedSystems]
      ];
      const chain = buildImpactChain(data);
      box.innerHTML = `
        <div class="wb-panel">
          <h2>Impact Analysis : ${escapeHtml(t.name)}</h2>
          <p><span class="wb-pill wb-pill--ok">type=${escapeHtml(t.type)}</span></p>
          <div class="wb-layer-grid">
            ${layers.map(([label, arr]) => `<div class="wb-layer"><strong>${(arr || []).length}</strong><span>${label}</span></div>`).join("")}
          </div>
          <h3>역추적 체인</h3>
          <pre class="wb-chain">${escapeHtml(chain)}</pre>
          <div class="wb-tabs" id="impactTabs">
            <button type="button" class="wb-tab is-active" data-tab="path">Path</button>
            <button type="button" class="wb-tab" data-tab="list">Affected List</button>
            <button type="button" class="wb-tab" data-tab="evidence">Evidence</button>
          </div>
          <div id="impactPane"></div>
        </div>`;
      const pane = document.getElementById("impactPane");
      const show = (tab) => {
        document.querySelectorAll("#impactTabs .wb-tab").forEach((b) => b.classList.toggle("is-active", b.dataset.tab === tab));
        if (tab === "path") {
          pane.innerHTML = `<pre class="wb-chain">${escapeHtml(JSON.stringify(data.paths || [], null, 2))}</pre>`;
        } else if (tab === "list") {
          pane.innerHTML = layers
            .map(([label, arr]) => `<h4>${label}</h4><p class="wb-chain">${escapeHtml((arr || []).join(", ") || "(empty)")}</p>`)
            .join("");
        } else {
          const ev = extractPathEvidence(data.paths);
          pane.innerHTML = `<button type="button" class="wb-btn" id="impEv">Evidence Drawer</button>`;
          document.getElementById("impEv")?.addEventListener("click", () => openEvidence(ev || data.table));
        }
      };
      document.querySelectorAll("#impactTabs .wb-tab").forEach((b) => b.addEventListener("click", () => show(b.dataset.tab)));
      show("path");
    } catch (e) {
      box.innerHTML = `<div class="wb-error">${escapeHtml(e.message)}</div>`;
    }
  }

  function buildImpactChain(data) {
    const parts = [];
    const pushAll = (arr) => {
      (arr || []).forEach((x) => parts.push(x));
    };
    pushAll(data.affectedSystems);
    pushAll(data.affectedBusinesses);
    pushAll(data.affectedFunctions);
    pushAll(data.affectedPrograms);
    pushAll(data.affectedServiceIds);
    pushAll(data.affectedHandlers);
    pushAll(data.affectedFacades);
    pushAll(data.affectedServices);
    pushAll(data.affectedDaos);
    pushAll(data.affectedMappers);
    if (data.table?.name) parts.push(data.table.name);
    return parts.join(" → ");
  }

  function extractPathEvidence(paths) {
    if (!Array.isArray(paths)) return null;
    for (const path of paths) {
      for (const step of path) {
        if (step.provenance) return { step, provenance: step.provenance };
      }
    }
    return null;
  }

  /* ---------- GATE ---------- */
  async function renderGate(params) {
    const target = (params.get("q") || "mgcoa8888S0").trim();
    root.innerHTML = `
      <form class="wb-form-row" id="gateForm">
        <label>검증대상
          <input name="q" value="${escapeHtml(target)}" placeholder="mgcoa8888S0">
        </label>
        <button class="wb-btn" type="submit">검증</button>
      </form>
      <div id="gateResult"><p class="wb-empty">검증 중…</p></div>`;

    document.getElementById("gateForm").addEventListener("submit", (e) => {
      e.preventDefault();
      const fd = new FormData(e.target);
      navigate("gate", { q: String(fd.get("q") || "").trim() });
    });

    const box = document.getElementById("gateResult");
    try {
      const rules = await OntologyApi.validateRules();
      box.innerHTML = `
        <div class="wb-panel">
          <h2>Architecture Gate</h2>
          <p>대상 컨텍스트: <code>${escapeHtml(target)}</code> (전역 규칙 검증 API)</p>
          ${renderRulesTable(rules)}
          <div class="wb-actions" style="margin-top:1rem">
            <button type="button" class="wb-btn wb-btn--ghost" id="gateEv">Evidence</button>
            <button type="button" class="wb-btn" id="gateRetry">다시 검증</button>
          </div>
        </div>`;
      document.getElementById("gateEv")?.addEventListener("click", () => openEvidence(rules));
      document.getElementById("gateRetry")?.addEventListener("click", () => renderGate(params));
    } catch (e) {
      box.innerHTML = `<div class="wb-error">${escapeHtml(e.message)}</div>`;
    }
  }

  function normalizeFindings(rules) {
    if (!rules) return [];
    if (Array.isArray(rules.findings)) return rules.findings;
    if (Array.isArray(rules.results)) return rules.results;
    if (Array.isArray(rules.rules)) return rules.rules;
    if (rules.byRule && typeof rules.byRule === "object") {
      return Object.entries(rules.byRule).map(([id, v]) => ({
        ruleId: id,
        ...(typeof v === "object" ? v : { status: v })
      }));
    }
    return [];
  }

  function renderRulesTable(rules) {
    const findings = normalizeFindings(rules);
    if (!findings.length) {
      return `<pre class="wb-chain">${escapeHtml(JSON.stringify(rules || {}, null, 2))}</pre>`;
    }
    let pass = 0;
    let fail = 0;
    const rows = findings
      .map((f) => {
        const id = f.ruleId || f.id || f.code || f.rule || "RULE";
        const status = String(f.verdict || f.status || f.result || (f.passed === false ? "FAIL" : f.passed ? "PASS" : "")).toUpperCase() || "INFO";
        if (status === "PASS") pass += 1;
        if (status === "FAIL" || status === "FAILED") fail += 1;
        const pill = status === "PASS" ? "wb-pill--ok" : status.includes("FAIL") ? "wb-pill--fail" : "wb-pill--warn";
        const label = f.target ? `${f.message || ""} · ${f.target}` : (f.message || f.description || f.name || "");
        return `<tr>
          <td>${escapeHtml(id)}</td>
          <td>${escapeHtml(label)}</td>
          <td><span class="wb-pill ${pill}">${escapeHtml(status)}</span></td>
        </tr>`;
      })
      .join("");
    const score = pass + fail > 0 ? Math.round((pass / (pass + fail)) * 100) : (rules.score ?? "–");
    const overall = rules.status ? `overall=${rules.status}` : "";
    return `
      <table class="wb-table">
        <thead><tr><th>Rule</th><th>설명</th><th>결과</th></tr></thead>
        <tbody>${rows}</tbody>
      </table>
      <p>Score : <strong>${escapeHtml(score)}</strong> / 100 &nbsp; (PASS ${pass} · FAIL ${fail}) ${escapeHtml(overall)}</p>`;
  }

  /* ---------- QnA ---------- */
  async function renderQnA(params) {
    const preset = (params.get("q") || "").trim();
    root.innerHTML = `
      <section class="wb-panel">
        <h2>06 · Architecture QnA</h2>
        <p class="wb-empty">질문하면 <code>docs/knowledge/exearchidoc</code> 문서를 검색·발췌하여 답합니다. (추출형 QnA, 외부 LLM 없음)</p>
        <form id="qnaForm" class="wb-form-grid">
          <label style="grid-column:1/-1">질문
            <textarea name="question" rows="4" placeholder="예: TimeoutExecutor와 업무 Transaction 경계는? / Filter와 Interceptor를 합치면 안 되는 이유는?">${escapeHtml(preset)}</textarea>
          </label>
          <label>TopK
            <select name="topK">
              <option>3</option>
              <option selected>5</option>
              <option>8</option>
            </select>
          </label>
          <div style="grid-column:1/-1" class="wb-actions">
            <button class="wb-btn primary" type="submit">Ask</button>
            <a class="wb-btn" href="#/knowledge">99 · Knowledge 열기</a>
          </div>
        </form>
        <div class="wb-actions" style="flex-wrap:wrap;margin:0.75rem 0">
          ${[
            "TX 경계는 어디인가",
            "TimeoutExecutor 동작",
            "Filter와 Interceptor 차이",
            "ServiceId 네이밍",
            "페이징 처리 방식",
            "DAO Namespace"
          ]
            .map((s) => `<button type="button" class="wb-pill" data-sample="${escapeHtml(s)}">${escapeHtml(s)}</button>`)
            .join("")}
        </div>
        <div id="qnaResult"><p class="wb-empty">질문을 입력하세요.</p></div>
      </section>`;

    const result = root.querySelector("#qnaResult");
    async function runAsk(question, topK) {
      result.innerHTML = `<p class="wb-empty">검색 중…</p>`;
      try {
        const data = await OntologyApi.qnaAsk(question, topK);
        const refs = (data.references || [])
          .map((r) => `
            <div class="wb-candidate">
              <div class="wb-candidate__head">
                <strong>${escapeHtml(r.rank)}. ${escapeHtml(r.title)}</strong>
                <span class="wb-pill">score ${escapeHtml(r.score)}</span>
              </div>
              <div><code>${escapeHtml(r.fileName)}</code>
                <a class="wb-btn" href="#/knowledge?doc=${encodeURIComponent(r.id)}">문서 보기</a>
              </div>
              <ul>${(r.snippets || []).map((s) => `<li>${escapeHtml(s)}</li>`).join("") || "<li class='wb-empty'>snippet 없음</li>"}</ul>
            </div>`)
          .join("");
        const hints = data.ontologyHints && Object.keys(data.ontologyHints).length
          ? `<h3>Ontology Hints</h3><pre class="wb-pre">${escapeHtml(JSON.stringify(data.ontologyHints, null, 2))}</pre>`
          : "";
        result.innerHTML = `
          <div class="wb-class-preview">
            <div><strong>Status</strong> ${escapeHtml(data.status)} · mode=${escapeHtml(data.mode)}</div>
            <div><strong>Corpus</strong> <code>${escapeHtml(data.corpus)}</code></div>
          </div>
          <h3>Answer</h3>
          <pre class="wb-pre">${escapeHtml(data.answer || "")}</pre>
          <h3>References (${escapeHtml(data.referenceCount || 0)})</h3>
          ${refs || "<p class='wb-empty'>참고 문서 없음</p>"}
          ${hints}`;
      } catch (e) {
        result.innerHTML = `<div class="wb-error">QnA 실패: ${escapeHtml(e.message)}</div>`;
      }
    }

    root.querySelector("#qnaForm").addEventListener("submit", (e) => {
      e.preventDefault();
      const fd = new FormData(e.target);
      const question = String(fd.get("question") || "").trim();
      const topK = Number(fd.get("topK") || 5);
      if (!question) {
        showToast("질문을 입력하세요");
        return;
      }
      navigate("qna", { q: question });
      runAsk(question, topK);
    });
    root.querySelectorAll("[data-sample]").forEach((btn) => {
      btn.addEventListener("click", () => {
        const q = btn.getAttribute("data-sample");
        root.querySelector("textarea[name=question]").value = q;
        runAsk(q, 5);
      });
    });
    if (preset) runAsk(preset, 5);
  }

  /* ---------- KNOWLEDGE ---------- */
  async function renderKnowledge(params) {
    const category = (params.get("category") || "ALL").toUpperCase();
    const q = (params.get("q") || "").trim();
    const docId = (params.get("doc") || "").trim();
    root.innerHTML = `<p class="wb-empty">Architecture Knowledge 로딩 중…</p>`;
    try {
      if (docId) {
        const doc = await OntologyApi.knowledgeDoc(docId);
        const html = (window.WorkbenchMarkdown && WorkbenchMarkdown.render)
          ? WorkbenchMarkdown.render(doc.content || "")
          : `<pre class="wb-pre">${escapeHtml(doc.content || "")}</pre>`;
        root.innerHTML = `
          <section class="wb-panel wb-doc">
            <div class="wb-actions" style="margin-bottom:0.75rem">
              <a class="wb-btn" href="#/knowledge">← Knowledge 목록</a>
              <a class="wb-btn" href="#/qna?q=${encodeURIComponent(doc.title || doc.fileName)}">이 주제로 QnA</a>
              <button type="button" class="wb-btn" id="toggleRawMd">원문(MD) 보기</button>
            </div>
            <header class="wb-doc__head">
              <h2>${escapeHtml(doc.title || doc.fileName)}</h2>
              <p class="wb-empty"><code>${escapeHtml(doc.relativePath || doc.fileName)}</code>
                · category=${escapeHtml(doc.category)} · lines=${escapeHtml(doc.lineCount)}</p>
            </header>
            <nav class="wb-doc__toc" aria-label="목차">
              ${(doc.headings || []).slice(0, 24).map((h) => {
                const id = String(h).toLowerCase().replace(/[^\w가-힣]+/g, "-").replace(/^-|-$/g, "");
                return `<a href="#${id}">${escapeHtml(h)}</a>`;
              }).join("")}
            </nav>
            <article class="wb-md" id="knowledgeArticle">${html}</article>
            <pre class="wb-pre" id="knowledgeRaw" hidden style="max-height:70vh;overflow:auto">${escapeHtml(doc.content || "")}</pre>
          </section>`;
        root.querySelector("#toggleRawMd")?.addEventListener("click", () => {
          const article = root.querySelector("#knowledgeArticle");
          const raw = root.querySelector("#knowledgeRaw");
          const btn = root.querySelector("#toggleRawMd");
          const showRaw = raw.hidden === false;
          if (showRaw) {
            raw.hidden = true;
            article.hidden = false;
            btn.textContent = "원문(MD) 보기";
          } else {
            raw.hidden = false;
            article.hidden = true;
            btn.textContent = "문서형식으로 보기";
          }
        });
        return;
      }

      const data = await OntologyApi.knowledgeCatalog({
        category: category === "ALL" ? "" : category,
        q
      });
      const cats = Object.entries(data.byCategory || {})
        .map(([c, n]) => `<a class="wb-pill ${c === category ? "is-on" : ""}" href="#/knowledge?category=${encodeURIComponent(c)}${q ? `&q=${encodeURIComponent(q)}` : ""}">${escapeHtml(c)} · ${n}</a>`)
        .join(" ");
      const rows = (data.documents || [])
        .map((d) => `<tr>
          <td><span class="wb-pill">${escapeHtml(d.category)}</span></td>
          <td><a href="#/knowledge?doc=${encodeURIComponent(d.id)}"><strong>${escapeHtml(d.title)}</strong></a>
            <div class="wb-muted"><code>${escapeHtml(d.fileName)}</code></div></td>
          <td>${escapeHtml(d.lineCount)}</td>
          <td>${(d.headings || []).slice(0, 3).map((h) => escapeHtml(h)).join(" · ")}</td>
          <td>
            <a class="wb-btn" href="#/knowledge?doc=${encodeURIComponent(d.id)}">Open</a>
            <a class="wb-btn" href="#/qna?q=${encodeURIComponent(d.title)}">Ask</a>
          </td>
        </tr>`)
        .join("");

      root.innerHTML = `
        <section class="wb-panel">
          <h2>99 · Architecture Knowledge</h2>
          <p class="wb-empty">출처 <code>${escapeHtml(data.source)}</code> · loadedFrom=${escapeHtml(data.loadedFrom)}
            · 문서 <strong>${escapeHtml(data.total)}</strong> · 표시 <strong>${escapeHtml(data.count)}</strong></p>
          <div class="wb-actions" style="flex-wrap:wrap;margin-bottom:0.75rem">
            <a class="wb-pill ${category === "ALL" ? "is-on" : ""}" href="#/knowledge">ALL · ${escapeHtml(data.total)}</a>
            ${cats}
          </div>
          <form class="wb-form-row" id="knowledgeFilter">
            <input name="q" value="${escapeHtml(q)}" placeholder="제목/파일명/헤딩 검색">
            <button class="wb-btn" type="submit">Filter</button>
            <a class="wb-btn" href="#/qna">06 · QnA로 질문</a>
          </form>
          <div class="wb-table-wrap">
            <table class="wb-table">
              <thead><tr><th>Cat</th><th>Document</th><th>Lines</th><th>Headings</th><th></th></tr></thead>
              <tbody>${rows || '<tr><td colspan="5">문서 없음</td></tr>'}</tbody>
            </table>
          </div>
          <p class="wb-empty">${escapeHtml(data.note || "")}</p>
        </section>`;

      document.getElementById("knowledgeFilter").addEventListener("submit", (e) => {
        e.preventDefault();
        const fd = new FormData(e.target);
        navigate("knowledge", {
          category: category === "ALL" ? "" : category,
          q: String(fd.get("q") || "").trim()
        });
      });
    } catch (e) {
      root.innerHTML = `<div class="wb-error">Knowledge 로딩 실패: ${escapeHtml(e.message)}</div>`;
    }
  }

  /* ---------- ROUTER ---------- */
  async function render() {
    showToast("");
    closeEvidence();
    const { route, params } = parseHash();
    const embed =
      params.get("embed") === "1" ||
      params.get("embed") === "true" ||
      (window.self !== window.top);
    document.body.classList.toggle("wb-embed", embed);
    const safe = META[route] ? route : "home";
    setActiveNav(safe);
    pageTitle.textContent = META[safe].title;
    pageDesc.textContent = META[safe].desc;
    if (safe === "home") await renderHome();
    else if (safe === "dashboard") await renderDashboard(params);
    else if (safe === "catalog") await renderCatalog(params);
    else if (safe === "search") await renderSearch(params);
    else if (safe === "impact") await renderImpact(params);
    else if (safe === "gate") await renderGate(params);
    else if (safe === "qna") await renderQnA(params);
    else if (safe === "knowledge") await renderKnowledge(params);
    else if (safe === "design") {
      await DesignAssistant.render(root, params, { openEvidence, showToast, navigate });
    }
  }

  window.addEventListener("hashchange", render);
  if (!location.hash) location.hash = "#/home";
  else render();
})();
