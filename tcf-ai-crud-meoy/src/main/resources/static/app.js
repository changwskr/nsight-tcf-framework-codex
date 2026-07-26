(() => {
  const state = {
    steps: [],
    sessions: [],
    sessionId: null,
    detail: null,
    stepDetail: null,
    qIndex: 0,
    draftAnswer: null,
    filter: "",
    domainSummary: null,
    domainSelectedBc: null,
  };

  const $ = (id) => document.getElementById(id);
  const els = {
    sessionList: $("sessionList"),
    sessionCount: $("sessionCount"),
    sessionSearch: $("sessionSearch"),
    pageTitle: $("pageTitle"),
    gateBadge: $("gateBadge"),
    exportBtn: $("exportBtn"),
    deleteSessionBtn: $("deleteSessionBtn"),
    emptyView: $("emptyView"),
    masterView: $("masterView"),
    ledgerView: $("ledgerView"),
    domainView: $("domainView"),
    stepSessionView: $("stepSessionView"),
    sourceView: $("sourceView"),
    wizardView: $("wizardView"),
    sourceMeta: $("sourceMeta"),
    sourceFiles: $("sourceFiles"),
    sourceCount: $("sourceCount"),
    sourceFileTitle: $("sourceFileTitle"),
    sourceFileMeta: $("sourceFileMeta"),
    sourceContent: $("sourceContent"),
    masterMarkdown: $("masterMarkdown"),
    ledgerRows: $("ledgerRows"),
    stepSessionQuery: $("stepSessionQuery"),
    stepSessionStepId: $("stepSessionStepId"),
    stepSessionStatus: $("stepSessionStatus"),
    stepSessionBc: $("stepSessionBc"),
    stepSessionRows: $("stepSessionRows"),
    stepSessionDetailTitle: $("stepSessionDetailTitle"),
    stepSessionDetail: $("stepSessionDetail"),
    domainSourceNote: $("domainSourceNote"),
    domainMetricModules: $("domainMetricModules"),
    domainMetricDomains: $("domainMetricDomains"),
    domainMetricSids: $("domainMetricSids"),
    domainQuery: $("domainQuery"),
    domainGroup: $("domainGroup"),
    domainStatus: $("domainStatus"),
    domainBusiness: $("domainBusiness"),
    domainModules: $("domainModules"),
    domainModuleCount: $("domainModuleCount"),
    domainDetailTitle: $("domainDetailTitle"),
    domainDetailMeta: $("domainDetailMeta"),
    domainDetailBody: $("domainDetailBody"),
    domainRows: $("domainRows"),
    stepRail: $("stepRail"),
    stepTitle: $("stepTitle"),
    stepOutcome: $("stepOutcome"),
    stepBadge: $("stepBadge"),
    qProgress: $("qProgress"),
    qText: $("qText"),
    qRecommended: $("qRecommended"),
    qInput: $("qInput"),
    gateChecks: $("gateChecks"),
    answeredList: $("answeredList"),
    stepMarkdown: $("stepMarkdown"),
    wizardMsg: $("wizardMsg"),
    prevQBtn: $("prevQBtn"),
    nextQBtn: $("nextQBtn"),
    saveAnswerBtn: $("saveAnswerBtn"),
    completeStepBtn: $("completeStepBtn"),
    nextStepBtn: $("nextStepBtn"),
  };

  async function api(path, options = {}) {
    const res = await fetch(path, {
      headers: { "Content-Type": "application/json", ...(options.headers || {}) },
      ...options,
    });
    if (!res.ok) {
      let msg = res.statusText;
      try {
        const body = await res.json();
        msg = body.error || msg;
      } catch (_) {}
      throw new Error(msg);
    }
    if (res.status === 204) return null;
    const ct = res.headers.get("content-type") || "";
    if (ct.includes("application/zip") || ct.includes("octet-stream")) {
      return res.blob();
    }
    return res.json();
  }

  function showView(name) {
    [els.emptyView, els.masterView, els.ledgerView, els.domainView, els.stepSessionView, els.sourceView, els.wizardView]
      .forEach((el) => el && el.classList.remove("active"));
    if (name === "empty") els.emptyView.classList.add("active");
    if (name === "master") els.masterView.classList.add("active");
    if (name === "ledger") els.ledgerView.classList.add("active");
    if (name === "domain") els.domainView.classList.add("active");
    if (name === "stepSession") els.stepSessionView.classList.add("active");
    if (name === "source") els.sourceView.classList.add("active");
    if (name === "wizard") els.wizardView.classList.add("active");
  }

  function showMsg(text, isError) {
    els.wizardMsg.textContent = text;
    els.wizardMsg.classList.remove("hidden");
    els.wizardMsg.style.background = isError ? "#fdecec" : "";
    els.wizardMsg.style.color = isError ? "#7a1c13" : "";
  }

  function clearMsg() {
    els.wizardMsg.classList.add("hidden");
  }

  function gateClass(status) {
    if (status === "PASS" || status === "CONDITIONAL") return "ok";
    if (status === "HOLD" || status === "STOP") return "lock";
    return "muted";
  }

  function isUnlocked() {
    return state.detail && (state.detail.unlocked === true
      || ["PASS", "CONDITIONAL"].includes(state.detail.session?.gateStatus));
  }

  function stepDone(stepId) {
    return (state.detail?.results || []).some((r) => r.stepId === stepId && r.status === "DONE");
  }

  function renderSessions() {
    const q = state.filter.trim().toLowerCase();
    const list = state.sessions.filter((s) => !q || s.name.toLowerCase().includes(q));
    els.sessionCount.textContent = String(state.sessions.length);
    if (!list.length) {
      els.sessionList.innerHTML = '<div class="model-empty">세션 없음</div>';
      return;
    }
    els.sessionList.innerHTML = list.map((s) => `
      <button class="model-item ${s.id === state.sessionId ? "active" : ""}" data-id="${s.id}">
        <strong>${s.sampleFlag ? "📦 " : ""}${escapeHtml(s.name)}</strong>
        <span>${escapeHtml(s.currentStepId || "")} · Gate ${escapeHtml(s.gateStatus || "NONE")}${s.businessCode ? " · " + escapeHtml(s.businessCode) : ""}</span>
      </button>
    `).join("");
    els.sessionList.querySelectorAll(".model-item").forEach((btn) => {
      btn.addEventListener("click", () => openSession(btn.dataset.id));
    });
  }

  function renderStepRail() {
    els.stepRail.innerHTML = state.steps.map((step) => {
      const locked = step.requiresGate && !isUnlocked();
      const done = stepDone(step.id);
      const active = state.detail?.session?.currentStepId === step.id;
      return `<button class="step-chip ${active ? "active" : ""} ${done ? "done" : ""} ${locked ? "locked" : ""}"
        data-step="${step.id}" ${locked ? "disabled" : ""}>${step.id}</button>`;
    }).join("");
    els.stepRail.querySelectorAll(".step-chip").forEach((btn) => {
      btn.addEventListener("click", async () => {
        try {
          await api(`/api/sessions/${state.sessionId}/move/${btn.dataset.step}`, { method: "POST", body: "{}" });
          await openSession(state.sessionId);
        } catch (e) {
          showMsg(e.message, true);
        }
      });
    });
  }

  function currentStepDef() {
    const id = state.detail?.session?.currentStepId;
    return state.steps.find((s) => s.id === id) || state.steps[0];
  }

  function answerFor(stepId, questionId) {
    return (state.detail?.answers || []).find((a) => a.stepId === stepId && a.questionId === questionId);
  }

  function parseAnswer(raw) {
    if (raw == null) return "";
    try {
      return JSON.parse(raw);
    } catch (_) {
      return raw;
    }
  }

  function visibleQuestions(step) {
    const questions = step.questions || [];
    return questions.filter((q) => {
      if (!q.showWhen || !q.showWhen.questionId) return true;
      const dep = answerFor(step.id, q.showWhen.questionId);
      if (!dep) return false;
      const val = String(parseAnswer(dep.answerJson) ?? "").trim();
      const expected = String(q.showWhen.equals ?? "").trim();
      return val === expected
        || val.startsWith(expected + " ")
        || val.startsWith(expected + " —")
        || val.startsWith(expected + "—");
    });
  }

  function renderQuestion() {
    const step = currentStepDef();
    if (!step || !state.stepDetail) return;
    const questions = visibleQuestions(step);
    if (!questions.length) return;
    if (state.qIndex >= questions.length) state.qIndex = questions.length - 1;
    if (state.qIndex < 0) state.qIndex = 0;
    const q = questions[state.qIndex];
    const existing = answerFor(step.id, q.id);
    const existingVal = existing ? parseAnswer(existing.answerJson) : (q.recommended || "");

    els.stepTitle.textContent = `${step.id} — ${step.title}`;
    els.stepOutcome.textContent = step.outcome || "";
    els.stepBadge.textContent = step.id;
    els.qProgress.textContent = `질문 ${state.qIndex + 1}/${questions.length}`;
    els.qText.textContent = q.text;
    els.qRecommended.textContent = q.recommended ? `권장: ${q.recommended}` : "";
    els.gateChecks.innerHTML = (step.gateChecks || []).map((c) => `<li>${escapeHtml(c)}</li>`).join("");
    els.stepMarkdown.textContent = state.stepDetail.markdown || "";

    if (q.type === "single") {
      els.qInput.innerHTML = (q.options || []).map((o) => `
        <label class="option ${o.value === q.recommended ? "recommended" : ""}">
          <input type="radio" name="ans" value="${escapeAttr(o.value)}" ${String(existingVal) === String(o.value) ? "checked" : ""}>
          <span><strong>${escapeHtml(o.value)}</strong> — ${escapeHtml(o.label)}</span>
        </label>
      `).join("");
      els.qInput.querySelectorAll("input").forEach((inp) => {
        inp.addEventListener("change", () => { state.draftAnswer = inp.value; });
      });
      state.draftAnswer = String(existingVal || "");
    } else {
      els.qInput.innerHTML = `<textarea rows="4" placeholder="${escapeAttr(q.placeholder || "")}">${escapeHtml(String(existingVal || ""))}</textarea>`;
      const ta = els.qInput.querySelector("textarea");
      state.draftAnswer = ta.value;
      ta.addEventListener("input", () => { state.draftAnswer = ta.value; });
    }

    const answered = questions.map((qq) => {
      const a = answerFor(step.id, qq.id);
      return `<li><strong>${escapeHtml(qq.text)}</strong>${a ? escapeHtml(String(parseAnswer(a.answerJson))) : "<em>미답</em>"}</li>`;
    }).join("");
    els.answeredList.innerHTML = answered || "<li>아직 없음</li>";

    els.prevQBtn.disabled = state.qIndex <= 0;
    els.nextQBtn.disabled = state.qIndex >= questions.length - 1;
    const allAnswered = questions.every((qq) => !!answerFor(step.id, qq.id));
    els.completeStepBtn.disabled = !allAnswered;
    const nextId = step.nextId;
    const nextLocked = nextId && state.steps.find((s) => s.id === nextId)?.requiresGate && !isUnlocked();
    els.nextStepBtn.disabled = !stepDone(step.id) || !nextId || !!nextLocked;
  }

  async function loadSteps() {
    const data = await api("/api/steps");
    state.steps = data.steps || [];
  }

  async function loadSessions() {
    const data = await api("/api/sessions");
    state.sessions = data.sessions || [];
    renderSessions();
  }

  async function openSession(id) {
    state.sessionId = id;
    state.detail = await api(`/api/sessions/${id}`);
    const session = state.detail.session;
    els.pageTitle.textContent = session.name;
    els.gateBadge.textContent = `Gate: ${session.gateStatus || "NONE"}`;
    els.gateBadge.className = `badge ${gateClass(session.gateStatus)}`;
    els.exportBtn.disabled = false;
    els.deleteSessionBtn.disabled = false;
    const cloneBtn = $("cloneTemplateBtn");
    if (cloneBtn) cloneBtn.disabled = false;
    const srcBtn = $("viewSourcesBtn");
    if (srcBtn) srcBtn.disabled = false;
    renderSessions();
    renderStepRail();
    const stepId = session.currentStepId || "C-MASTER";
    state.stepDetail = await api(`/api/steps/${encodeURIComponent(stepId)}`);
    state.qIndex = 0;
    clearMsg();
    showView("wizard");
    renderQuestion();
  }

  async function saveAnswer() {
    const step = currentStepDef();
    const q = step.questions[state.qIndex];
    let answer = state.draftAnswer;
    if (q.type === "single") {
      const checked = els.qInput.querySelector("input[name=ans]:checked");
      answer = checked ? checked.value : answer;
    } else {
      const ta = els.qInput.querySelector("textarea");
      answer = ta ? ta.value : answer;
    }
    if (answer == null || String(answer).trim() === "") {
      showMsg("답을 입력하세요.", true);
      return;
    }
    try {
      await api(`/api/sessions/${state.sessionId}/answers`, {
        method: "POST",
        body: JSON.stringify({ stepId: step.id, questionId: q.id, answer }),
      });
      state.detail = await api(`/api/sessions/${state.sessionId}`);
      els.gateBadge.textContent = `Gate: ${state.detail.session.gateStatus}`;
      els.gateBadge.className = `badge ${gateClass(state.detail.session.gateStatus)}`;
      showMsg("답변을 확정했습니다.", false);
      renderQuestion();
      renderStepRail();
      await loadSessions();
    } catch (e) {
      showMsg(e.message, true);
    }
  }

  async function completeStep() {
    const step = currentStepDef();
    try {
      const result = await api(`/api/sessions/${state.sessionId}/steps/${step.id}/complete`, {
        method: "POST",
        body: "{}",
      });
      showMsg(`단계 ${step.id} 완료. 다음: ${result.nextId || "(끝)"}`, false);
      await openSession(state.sessionId);
      await loadSessions();
      els.nextStepBtn.disabled = !result.nextId;
    } catch (e) {
      showMsg(e.message, true);
    }
  }

  async function goNextStep() {
    const step = currentStepDef();
    if (!step.nextId) return;
    try {
      await api(`/api/sessions/${state.sessionId}/move/${step.nextId}`, { method: "POST", body: "{}" });
      await openSession(state.sessionId);
    } catch (e) {
      showMsg(e.message, true);
    }
  }

  async function showDomainLedger(keepSelection) {
    if (!state.domainSummary) {
      state.domainSummary = await api("/api/domains/summary");
      fillSelect(els.domainGroup, state.domainSummary.groups || []);
      fillSelect(els.domainStatus, state.domainSummary.statuses || []);
    }
    els.domainSourceNote.textContent = state.domainSummary.sourceNote || "";
    els.domainMetricModules.textContent = `모듈 ${state.domainSummary.moduleCount || 0}`;
    els.domainMetricDomains.textContent = `도메인 ${state.domainSummary.domainCount || 0}`;
    els.domainMetricSids.textContent = `ServiceId ${state.domainSummary.serviceIdCount || 0}`;

    const params = new URLSearchParams();
    const q = els.domainQuery.value.trim();
    const group = els.domainGroup.value;
    const status = els.domainStatus.value;
    const bc = els.domainBusiness.value;
    if (q) params.set("q", q);
    if (group) params.set("group", group);
    if (status) params.set("status", status);
    if (bc) params.set("businessCode", bc);

    const data = await api("/api/domains?" + params.toString());
    const modules = data.modules || [];
    fillSelect(els.domainBusiness, modules.map((m) => m.businessCode), true);

    els.domainModuleCount.textContent = String(modules.length);
    els.domainModules.innerHTML = modules.map((m) => `
      <button class="domain-module-card ${state.domainSelectedBc === m.businessCode ? "active" : ""}"
        data-bc="${escapeAttr(m.businessCode)}">
        <strong>${escapeHtml(m.businessCode)} · ${escapeHtml(m.moduleName || "")}</strong>
        <span>${escapeHtml(m.group || "")} · ${m.domainCount || 0}도메인 · ${m.serviceIdCount || 0} SID · ${escapeHtml(m.status || "")}</span>
      </button>
    `).join("") || '<div class="muted">결과 없음</div>';
    els.domainModules.querySelectorAll(".domain-module-card").forEach((btn) => {
      btn.addEventListener("click", () => openDomainModule(btn.dataset.bc));
    });

    const rows = data.rows || [];
    els.domainRows.innerHTML = rows.length
      ? rows.map((r) => `
        <tr>
          <td><span class="bc-chip">${escapeHtml(r.businessCode || "")}</span></td>
          <td>${escapeHtml(r.domainCode || "-")}<div class="sub">${escapeHtml(r.domainName || "")}</div></td>
          <td class="mono">${escapeHtml(r.serviceId || "-")}</td>
          <td>${escapeHtml(r.operation || "-")}</td>
          <td>${escapeHtml(r.handler || "-")}</td>
          <td>${r.localPort ?? ""}</td>
          <td class="status-${escapeAttr(r.status || "")}">${escapeHtml(r.status || "")}</td>
        </tr>`).join("")
      : '<tr><td colspan="7">조회 결과 없음</td></tr>';

    if (keepSelection && state.domainSelectedBc) {
      await openDomainModule(state.domainSelectedBc, true);
    } else if (!state.domainSelectedBc && modules[0]) {
      await openDomainModule(modules[0].businessCode, true);
    }

    showView("domain");
    els.pageTitle.textContent = "업무도메인 원장";
  }

  async function openDomainModule(businessCode, silent) {
    state.domainSelectedBc = businessCode;
    const mod = await api(`/api/domains/${encodeURIComponent(businessCode)}`);
    els.domainModules.querySelectorAll(".domain-module-card").forEach((btn) => {
      btn.classList.toggle("active", btn.dataset.bc === businessCode);
    });
    els.domainDetailTitle.textContent = `${mod.businessCode} — ${mod.moduleName || ""}`;
    els.domainDetailMeta.textContent =
      `그룹 ${mod.group || "-"} · 포트 ${mod.localPort ?? "-"} · Gradle ${mod.gradleModule || "(없음)"} · ${mod.status}`;
    if (!mod.domains || !mod.domains.length) {
      els.domainDetailBody.innerHTML = '<p class="muted">등록된 도메인/ServiceId가 없습니다. (CATALOG_ONLY 또는 미구현)</p>';
    } else {
      els.domainDetailBody.innerHTML = mod.domains.map((d) => `
        <div class="domain-domain-block">
          <h5>${escapeHtml(d.domainCode)} (${escapeHtml(d.domainName || "")}) · ${escapeHtml(d.handler || "")}</h5>
          <ul>${(d.serviceIds || []).map((s) =>
            `<li><code>${escapeHtml(s.serviceId)}</code> · ${escapeHtml(s.operation || "")}</li>`
          ).join("")}</ul>
        </div>`).join("");
    }
    if (!silent) {
      els.domainBusiness.value = businessCode;
    }
  }

  function fillSelect(selectEl, values, preserveCurrent) {
    const current = preserveCurrent ? selectEl.value : "";
    selectEl.innerHTML = "";
    selectEl.appendChild(new Option("전체", ""));
    values.forEach((v) => {
      if (!v) return;
      selectEl.appendChild(new Option(v, v));
    });
    if (preserveCurrent && current) selectEl.value = current;
  }

  async function showStepSessions() {
    if (els.stepSessionStepId.options.length <= 1 && state.steps.length) {
      state.steps.forEach((s) => {
        els.stepSessionStepId.appendChild(new Option(`${s.id} ${s.title}`, s.id));
      });
    }
    const params = new URLSearchParams();
    const q = els.stepSessionQuery.value.trim();
    const stepId = els.stepSessionStepId.value;
    const status = els.stepSessionStatus.value;
    const bc = els.stepSessionBc.value.trim();
    if (q) params.set("q", q);
    if (stepId) params.set("stepId", stepId);
    if (status) params.set("status", status);
    if (bc) params.set("businessCode", bc);
    const data = await api("/api/step-sessions?" + params.toString());
    const rows = data.rows || [];
    els.stepSessionRows.innerHTML = rows.length
      ? rows.map((r) => `
        <tr data-id="${r.id}">
          <td>${r.id}</td>
          <td>${escapeHtml(r.sessionName || r.sessionId || "")}</td>
          <td>${escapeHtml(r.stepId || "")}</td>
          <td>${escapeHtml(r.stepTitle || "")}</td>
          <td>${escapeHtml(r.businessCode || "-")}</td>
          <td>${escapeHtml(r.domainCode || "-")}</td>
          <td>${escapeHtml(r.status || "")}</td>
          <td>${escapeHtml((r.updatedAt || "").toString().slice(0, 19))}</td>
          <td><button class="ghost small" data-open="${r.id}">상세</button>
              <button class="ghost small" data-sess="${escapeAttr(r.sessionId || "")}">세션열기</button>
              <button class="ghost small" data-clone="${escapeAttr(r.sessionId || "")}">템플릿복제</button></td>
        </tr>`).join("")
      : '<tr><td colspan="9">조회 결과 없음</td></tr>';
    els.stepSessionRows.querySelectorAll("button[data-open]").forEach((btn) => {
      btn.addEventListener("click", async () => {
        const detail = await api(`/api/step-sessions/${btn.dataset.open}`);
        els.stepSessionDetailTitle.textContent = `${detail.stepId} · ${detail.sessionName || ""}`;
        els.stepSessionDetail.textContent = JSON.stringify({
          id: detail.id,
          sessionId: detail.sessionId,
          status: detail.status,
          businessCode: detail.businessCode,
          domainCode: detail.domainCode,
          answersJson: (() => { try { return JSON.parse(detail.answersJson || "[]"); } catch { return detail.answersJson; } })(),
          summaryMd: detail.summaryMd,
          confirmedAt: detail.confirmedAt,
        }, null, 2);
      });
    });
    els.stepSessionRows.querySelectorAll("button[data-sess]").forEach((btn) => {
      btn.addEventListener("click", () => openSession(btn.dataset.sess));
    });
    els.stepSessionRows.querySelectorAll("button[data-clone]").forEach((btn) => {
      btn.addEventListener("click", async () => {
        const name = prompt("복제 세션 이름", "CRUD 작업용 (템플릿 복제)");
        if (name === null) return;
        try {
          const created = await api(`/api/sessions/${btn.dataset.clone}/clone-as-template`, {
            method: "POST",
            body: JSON.stringify({ name }),
          });
          await loadSessions();
          await openSession(created.id);
        } catch (e) {
          alert(e.message);
        }
      });
    });
    showView("stepSession");
    els.pageTitle.textContent = "단계 세션 조회";
  }

  async function showSources() {
    if (!state.sessionId) {
      alert("세션을 먼저 선택하세요.");
      return;
    }
    const data = await api(`/api/sessions/${state.sessionId}/sources`);
    const files = data.files || [];
    els.sourceMeta.textContent = data.note
      || `업무코드 ${data.businessCode || "-"} · 도메인 ${data.domainCode || "-"} · 대상 ${data.module || "-"}${data.baseModule ? " · 기준참조 " + data.baseModule : ""} · ${files.length}개 파일`;
    els.sourceCount.textContent = String(files.length);
    els.sourceFileTitle.textContent = "파일을 선택하세요";
    els.sourceFileMeta.textContent = "";
    els.sourceContent.textContent = "";

    const byCategory = new Map();
    files.forEach((f) => {
      if (!byCategory.has(f.category)) byCategory.set(f.category, []);
      byCategory.get(f.category).push(f);
    });
    els.sourceFiles.innerHTML = files.length
      ? [...byCategory.entries()].map(([cat, list]) => `
          <div class="source-group">
            <div class="source-group-title">${escapeHtml(cat)} <span class="count-inline">${list.length}</span></div>
            ${list.map((f) => `
              <button class="source-file-item" data-path="${escapeAttr(f.path)}">
                <span class="source-file-name">${escapeHtml(f.name)}</span>
                <span class="source-file-path">${escapeHtml(f.path)}</span>
              </button>`).join("")}
          </div>`).join("")
      : '<div class="muted" style="padding:8px">표시할 소스가 없습니다.</div>';

    els.sourceFiles.querySelectorAll("button[data-path]").forEach((btn) => {
      btn.addEventListener("click", async () => {
        els.sourceFiles.querySelectorAll("button[data-path]").forEach((b) => b.classList.remove("selected"));
        btn.classList.add("selected");
        try {
          const file = await api(`/api/sources/content?path=${encodeURIComponent(btn.dataset.path)}`);
          els.sourceFileTitle.textContent = file.path;
          els.sourceFileMeta.textContent = `${file.size.toLocaleString()} bytes${file.truncated ? " · 512KB까지만 표시" : ""}`;
          els.sourceContent.textContent = file.content;
        } catch (e) {
          alert(e.message);
        }
      });
    });
    showView("source");
    els.pageTitle.textContent = "관련 소스";
  }

  async function showMaster() {
    const data = await api("/api/steps/C-MASTER");
    els.masterMarkdown.textContent = data.markdown || "";
    showView("master");
    els.pageTitle.textContent = "C-MASTER 원칙";
  }

  async function showLedger() {
    if (!state.sessionId) {
      alert("세션을 먼저 선택하세요.");
      return;
    }
    const data = await api(`/api/sessions/${state.sessionId}/ledger`);
    const rows = data.ledger || [];
    els.ledgerRows.innerHTML = rows.length
      ? rows.map((r) => `<tr><td>${escapeHtml(r.entryKey)}</td><td>${escapeHtml(r.value)}</td><td>${escapeHtml(r.sourceStepId || "")}</td></tr>`).join("")
      : '<tr><td colspan="3">원장 항목 없음</td></tr>';
    showView("ledger");
    els.pageTitle.textContent = "현황(원장)";
  }

  async function exportZip() {
    if (!state.sessionId) return;
    const blob = await api(`/api/sessions/${state.sessionId}/export.zip`);
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `crud-meoy-${state.sessionId}.zip`;
    a.click();
    URL.revokeObjectURL(url);
  }

  function escapeHtml(s) {
    return String(s)
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;");
  }

  function escapeAttr(s) {
    return escapeHtml(s).replaceAll("'", "&#39;");
  }

  $("newSessionBtn").addEventListener("click", async () => {
    const name = prompt("세션 이름", `CRUD-${new Date().toISOString().slice(0, 10)}`);
    if (name === null) return;
    const created = await api("/api/sessions", { method: "POST", body: JSON.stringify({ name }) });
    await loadSessions();
    await openSession(created.id);
  });
  $("domainLedgerBtn").addEventListener("click", () => showDomainLedger(false).catch((e) => alert(e.message)));
  $("stepSessionBtn").addEventListener("click", () => showStepSessions().catch((e) => alert(e.message)));
  $("stepSessionSearchBtn").addEventListener("click", () => showStepSessions().catch((e) => alert(e.message)));
  $("stepSessionResetBtn").addEventListener("click", () => {
    els.stepSessionQuery.value = "";
    els.stepSessionStepId.value = "";
    els.stepSessionStatus.value = "";
    els.stepSessionBc.value = "";
    showStepSessions().catch((e) => alert(e.message));
  });
  $("createSampleBtn").addEventListener("click", async () => {
    try {
      const r = await api("/api/samples/ln-customer-contact", { method: "POST", body: "{}" });
      await loadSessions();
      alert("샘플 세션 생성: " + r.sessionId);
      await showStepSessions();
    } catch (e) {
      alert(e.message);
    }
  });
  $("domainSearchBtn").addEventListener("click", () => showDomainLedger(true).catch((e) => alert(e.message)));
  $("domainResetBtn").addEventListener("click", () => {
    els.domainQuery.value = "";
    els.domainGroup.value = "";
    els.domainStatus.value = "";
    els.domainBusiness.value = "";
    state.domainSelectedBc = null;
    showDomainLedger(false).catch((e) => alert(e.message));
  });
  els.domainQuery.addEventListener("keydown", (e) => {
    if (e.key === "Enter") showDomainLedger(true).catch((err) => alert(err.message));
  });
  $("masterBtn").addEventListener("click", () => showMaster().catch((e) => alert(e.message)));
  $("ledgerBtn").addEventListener("click", () => showLedger().catch((e) => alert(e.message)));
  $("backFromMasterBtn").addEventListener("click", () => {
    if (state.sessionId) openSession(state.sessionId); else showView("empty");
  });
  $("backFromLedgerBtn").addEventListener("click", () => {
    if (state.sessionId) openSession(state.sessionId); else showView("empty");
  });
  $("viewSourcesBtn").addEventListener("click", () => showSources().catch((e) => alert(e.message)));
  $("backFromSourceBtn").addEventListener("click", () => {
    if (state.sessionId) openSession(state.sessionId); else showView("empty");
  });
  $("exportBtn").addEventListener("click", () => exportZip().catch((e) => alert(e.message)));
  $("deleteSessionBtn").addEventListener("click", async () => {
    if (!state.sessionId || !confirm("세션을 삭제할까요?")) return;
    await api(`/api/sessions/${state.sessionId}`, { method: "DELETE" });
    state.sessionId = null;
    state.detail = null;
    els.exportBtn.disabled = true;
    els.deleteSessionBtn.disabled = true;
    const cloneBtn = $("cloneTemplateBtn");
    if (cloneBtn) cloneBtn.disabled = true;
    const srcBtn = $("viewSourcesBtn");
    if (srcBtn) srcBtn.disabled = true;
    els.pageTitle.textContent = "세션을 선택하세요";
    els.gateBadge.textContent = "Gate: NONE";
    els.gateBadge.className = "badge muted";
    showView("empty");
    await loadSessions();
  });
  $("cloneTemplateBtn").addEventListener("click", async () => {
    if (!state.sessionId) return;
    const srcName = state.detail?.session?.name || "세션";
    const name = prompt("복제 세션 이름", srcName.replace(/^\[샘플\]\s*/, "") + " (작업용)");
    if (name === null) return;
    try {
      const created = await api(`/api/sessions/${state.sessionId}/clone-as-template`, {
        method: "POST",
        body: JSON.stringify({ name }),
      });
      await loadSessions();
      await openSession(created.id);
      showMsg("템플릿으로 복제했습니다. 이 세션에서 계속 진행하세요.", false);
    } catch (e) {
      alert(e.message);
    }
  });
  els.sessionSearch.addEventListener("input", () => {
    state.filter = els.sessionSearch.value;
    renderSessions();
  });
  els.prevQBtn.addEventListener("click", () => { state.qIndex -= 1; renderQuestion(); });
  els.nextQBtn.addEventListener("click", () => { state.qIndex += 1; renderQuestion(); });
  els.saveAnswerBtn.addEventListener("click", () => saveAnswer());
  els.completeStepBtn.addEventListener("click", () => completeStep());
  els.nextStepBtn.addEventListener("click", () => goNextStep());

  (async () => {
    try {
      await loadSteps();
      await loadSessions();
      showView("empty");
    } catch (e) {
      alert("초기화 실패: " + e.message);
    }
  })();
})();
