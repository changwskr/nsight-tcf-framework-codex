/**

 * Architecture Design Wizard (#/design)

 * STEP 1 Requirement → 2 Classification → 3 ServiceId → 4 Data/Table

 * → 5 Application → 6 Runtime/Policy → 7 Gate/Export

 */

(function (global) {

  const state = {

    step: 1,

    sessionId: null,

    scheme: null,

    requirement: null,

    classification: null,

    serviceIdDesign: null,

    programs: null,

    dataDesign: {

      selectedTables: [],

      joins: [],

      referenceServiceId: "",

      tableUnresolved: false,

      tableProposals: [],

      draftProposal: null,

      newTableProposal: null // legacy string; prefer tableProposals[]

    },

    tableCatalog: [],

    tableDetails: {},

    application: null,

    policy: null,

    gate: null,

    exportDoc: null

  };



  function esc(v) {

    return String(v ?? "")

      .replace(/&/g, "&amp;")

      .replace(/</g, "&lt;")

      .replace(/>/g, "&gt;")

      .replace(/"/g, "&quot;");

  }



  function download(filename, text, mime) {

    const blob = new Blob([text], { type: mime || "text/plain;charset=utf-8" });

    const a = document.createElement("a");

    a.href = URL.createObjectURL(blob);

    a.download = filename;

    a.click();

    URL.revokeObjectURL(a.href);

  }



  function operationCode(tx) {

    const t = String(tx || "QUERY").toUpperCase();

    if (t === "CREATE" || t === "C") return "C";

    if (t === "UPDATE" || t === "U") return "U";

    if (t === "DELETE" || t === "D") return "D";

    if (t === "MIXED" || t === "CRUD" || t === "A") return "A";

    if (t === "REPORT" || t === "R") return "R";

    return "S";

  }



  async function classificationOptions() {

    try {

      const scheme = await OntologyApi.classificationScheme();

      const major = scheme.majorGroup || { code: "MG", name: "Market Group Platform" };

      const businesses = Array.isArray(scheme.businessCodes) ? scheme.businessCodes : [];

      return {

        source: scheme.sourceDoc || "ontology/business/classification.yml",

        majorGroup: major,

        businesses: businesses.map((b) => ({

          code: String(b.code || "").toUpperCase(),

          name: b.name || b.code,

          en: b.en || "",

          note: b.note || "",

          functions: Object.entries(b.functions || {}).map(([code, meta]) => {

            const m = meta && typeof meta === "object" ? meta : { name: String(meta || code) };

            return {

              code: String(code).toUpperCase(),

              name: m.name || code,

              desc: m.desc || ""

            };

          })

        }))

      };

    } catch (e) {

      console.warn("classification scheme load failed", e);

      return {

        source: "fallback",

        majorGroup: { code: "MG", name: "Market Group Platform" },

        businesses: [

          {

            code: "CO",

            name: "공통",

            en: "Common",

            functions: [

              { code: "A", name: "공통관리", desc: "공통코드, 환경설정" },

              { code: "B", name: "사용자관리", desc: "사용자/권한" }

            ]

          }

        ]

      };

    }

  }



  function findBusiness(scheme, code) {

    return (scheme.businesses || []).find((b) => b.code === String(code || "").toUpperCase()) || null;

  }



  function findFunction(business, code) {

    if (!business) return null;

    return (business.functions || []).find((f) => f.code === String(code || "").toUpperCase()) || null;

  }



  function packageRootHint(system, businessCode, functionCode) {

    return `nhnis.${String(system || "MG").toLowerCase()}.${String(businessCode || "co").toLowerCase()}.${String(functionCode || "a").toLowerCase()}`;

  }



  function programPrefixHint(system, businessCode, functionCode) {

    return `${String(system || "MG").toLowerCase()}${String(businessCode || "CO").toLowerCase()}${String(functionCode || "A").toLowerCase()}`;

  }



  function businessOptionsHtml(scheme, selected) {

    return (scheme.businesses || [])

      .map((b) => {

        const label = `${b.code} · ${b.name}${b.en ? ` (${b.en})` : ""}`;

        return `<option value="${esc(b.code)}" ${b.code === selected ? "selected" : ""}>${esc(label)}</option>`;

      })

      .join("");

  }



  function functionOptionsHtml(scheme, businessCode, selected) {

    const business = findBusiness(scheme, businessCode) || scheme.businesses?.[0];

    const list = business?.functions?.length ? business.functions : [{ code: "A", name: "A" }];

    const sel = list.some((f) => f.code === selected) ? selected : list[0].code;

    return list

      .map((f) => {

        const label = `${f.code} · ${f.name}${f.desc ? ` — ${f.desc}` : ""}`;

        return `<option value="${esc(f.code)}" ${f.code === sel ? "selected" : ""}>${esc(label)}</option>`;

      })

      .join("");

  }



  function stepbarHtml() {

    const labels = [

      "1 Requirement",

      "2 Classification",

      "3 ServiceId",

      "4 Data/Table",

      "5 Application",

      "6 Runtime/Policy",

      "7 Gate/Export"

    ];

    return `<div class="wb-stepbar">${labels

      .map((l, i) => {

        const n = i + 1;

        const cls = n === state.step ? "wb-step is-on" : n < state.step ? "wb-step is-done" : "wb-step";

        return `<span class="${cls}" data-s="${n}">${esc(l)}</span>`;

      })

      .join("")}</div>`;

  }



  function navHtml(canNext, nextLabel) {

    return `

      <div class="wb-wizard-nav">

        <button type="button" class="wb-btn" id="wizBack" ${state.step <= 1 ? "disabled" : ""}>← Back</button>

        <button type="button" class="wb-btn primary" id="wizNext" ${canNext ? "" : "disabled"}>${esc(nextLabel || "Next →")}</button>

      </div>`;

  }



  function designPayload() {

    return {

      sessionId: state.sessionId,

      requirement: state.requirement || {},

      classification: state.classification || {},

      serviceIdDesign: state.serviceIdDesign || {},

      dataDesign: state.dataDesign || {},

      application: state.application || {},

      policy: state.policy || {},

      gate: state.gate || {}

    };

  }



  async function persistSession(showToast) {

    if (!state.sessionId) {

      const created = await OntologyApi.designSessionCreate(state.requirement || {});

      state.sessionId = created.sessionId;

    }

    try {

      await OntologyApi.designSessionPut(state.sessionId, designPayload());

    } catch (e) {

      showToast?.(`Session sync 실패: ${e.message}`);

    }

  }



  function renderStep1(root, params, helpers) {

    const scheme = state.scheme;

    const preBusiness = (params.get("business") || state.requirement?.businessCode || "CO").toUpperCase();

    const preType = params.get("type") || state.requirement?.transactionType || "QUERY";

    const preFunction = (params.get("function") || state.requirement?.functionCode || "A").toUpperCase();

    const preSystem = (params.get("system") || scheme.majorGroup?.code || "MG").toUpperCase();



    root.innerHTML = `

      ${stepbarHtml()}

      <section class="wb-panel">

        <h2>STEP 1 · Requirement</h2>

        <p class="wb-empty">신규 거래 요구를 입력합니다. Program/ServiceId/Table은 이후 단계에서 설계합니다.</p>

        <form id="wizReqForm" class="wb-form-grid">

          <label style="grid-column:1/-1">Title / Requirement

            <textarea name="title" placeholder="예: 마케팅희망고객 조회 신규 거래">${esc(state.requirement?.title || params.get("title") || "")}</textarea>

          </label>

          <label>대그룹 (System)

            <select name="system"><option value="${esc(preSystem)}" selected>${esc(preSystem)} · ${esc(scheme.majorGroup?.name || "")}</option></select>

          </label>

          <label>업무 (Business)<select name="businessCode" id="reqBusiness">${businessOptionsHtml(scheme, preBusiness)}</select></label>

          <label>기능 (Function)<select name="functionCode" id="reqFunction">${functionOptionsHtml(scheme, preBusiness, preFunction)}</select></label>

          <label>Transaction Type

            <select name="transactionType">

              ${["QUERY", "CREATE", "UPDATE", "DELETE", "MIXED", "REPORT"]

                .map((t) => `<option value="${t}" ${preType === t ? "selected" : ""}>${t} (${operationCode(t)})</option>`)

                .join("")}

            </select>

          </label>

          <label>Channel<select name="channel"><option>WEB</option><option>API</option><option>BATCH</option></select></label>

          <label>Paging<select name="paging"><option ${state.requirement?.paging === "NO" ? "" : "selected"}>YES</option><option ${state.requirement?.paging === "NO" ? "selected" : ""}>NO</option></select></label>

          <label>Timeout<select name="timeoutPolicy"><option>DEFAULT</option><option>STRICT</option></select></label>

          <label>Personal Data<select name="personalData"><option>UNKNOWN</option><option>YES</option><option>NO</option></select></label>

          <label style="grid-column:1/-1">참고 키워드 (Table 검색 힌트)

            <input name="keyword" type="text" value="${esc(state.requirement?.keyword || params.get("tables") || "")}" placeholder="TB_MK_CO 등">

          </label>

        </form>

        ${navHtml(true, "Next · Classification →")}

      </section>`;



    wireCommonNav(root, helpers, async () => {

      const fd = new FormData(root.querySelector("#wizReqForm"));

      const businessCode = String(fd.get("businessCode") || "CO").toUpperCase();

      const functionCode = String(fd.get("functionCode") || "A").toUpperCase();

      const system = String(fd.get("system") || "MG").toUpperCase();

      const business = findBusiness(scheme, businessCode);

      const fn = findFunction(business, functionCode);

      state.requirement = {

        title: String(fd.get("title") || "").trim() || "UNRESOLVED",

        system,

        businessCode,

        functionCode,

        transactionType: String(fd.get("transactionType") || "QUERY"),

        channel: String(fd.get("channel") || "WEB"),

        paging: String(fd.get("paging") || "YES"),

        timeoutPolicy: String(fd.get("timeoutPolicy") || "DEFAULT"),

        personalData: String(fd.get("personalData") || "UNKNOWN"),

        keyword: String(fd.get("keyword") || "").trim()

      };

      state.classification = {

        system,

        business: businessCode,

        businessName: business?.name || businessCode,

        function: functionCode,

        functionName: fn?.name || functionCode,

        packageRoot: packageRootHint(system, businessCode, functionCode),

        programPrefix: programPrefixHint(system, businessCode, functionCode),

        source: scheme.source

      };

      await persistSession(helpers.showToast);

      state.step = 2;

      await paint(root, params, helpers);

    });



    const biz = root.querySelector("#reqBusiness");

    const fnSel = root.querySelector("#reqFunction");

    biz?.addEventListener("change", () => {

      fnSel.innerHTML = functionOptionsHtml(scheme, biz.value, "");

    });

  }



  function renderStep2(root, params, helpers) {

    const c = state.classification || {};

    root.innerHTML = `

      ${stepbarHtml()}

      <section class="wb-panel">

        <h2>STEP 2 · Business Classification</h2>

        <p class="wb-empty">NSIGHT 애플리케이션 코드 분류표 기준. 이 값이 Program/ServiceId 접두를 결정합니다.</p>

        <div class="wb-class-preview">

          <div><strong>System</strong> ${esc(c.system)} · ${esc(state.scheme.majorGroup?.name || "")}</div>

          <div><strong>Business</strong> ${esc(c.business)} · ${esc(c.businessName || "")}</div>

          <div><strong>Function</strong> ${esc(c.function)} · ${esc(c.functionName || "")}</div>

          <div><strong>packageRoot</strong> <code>${esc(c.packageRoot)}</code></div>

          <div><strong>Program 접두</strong> <code>${esc(c.programPrefix)}####</code></div>

          <p class="wb-empty">기준: ${esc(c.source || "")}</p>

        </div>

        ${navHtml(true, "Next · ServiceId Design →")}

      </section>`;

    wireCommonNav(root, helpers, async () => {

      state.step = 3;

      await paint(root, params, helpers);

    }, () => {

      state.step = 1;

      paint(root, params, helpers);

    });

  }



  async function renderStep3(root, params, helpers) {

    const c = state.classification || {};

    const req = state.requirement || {};

    const showToast = helpers.showToast || (() => {});

    root.innerHTML = `

      ${stepbarHtml()}

      <section class="wb-panel" id="step3Panel">

        <h2>STEP 3 · ServiceId Design</h2>

        <p class="wb-empty">신규 Program No / ServiceId는 <strong>제안</strong>만 합니다. Architect가 확정합니다. 자동 확정 금지.</p>

        <div class="wb-form-grid" id="sidForm">

          <label>Program No (4 digits)

            <input id="programNo" maxlength="4" value="${esc(state.serviceIdDesign?.programNo || "")}" placeholder="예: 7000">

          </label>

          <label>Transaction → Operation

            <input value="${esc(req.transactionType || "QUERY")} → ${esc(operationCode(req.transactionType))}" disabled>

          </label>

          <label>Sequence

            <input id="sequence" maxlength="1" value="${esc(state.serviceIdDesign?.sequence || "0")}" placeholder="0">

          </label>

          <label>Reference ServiceId (optional)

            <input id="refServiceId" value="${esc(state.serviceIdDesign?.referenceServiceId || "mgcoa5530S0")}" placeholder="mgcoa5530S0">

          </label>

        </div>

        <div class="wb-actions" style="margin:0.75rem 0">

          <button type="button" class="wb-btn" id="btnLoadPrograms" data-action="load-programs">축별 Program 현황 조회</button>

          <button type="button" class="wb-btn primary" id="btnValidateSid" data-action="validate-sid">중복 검증</button>

        </div>

        <div id="programInventory" class="wb-table-wrap"><p class="wb-empty">「축별 Program 현황 조회」를 누르면 목록이 표시됩니다.</p></div>

        <div id="sidResult"></div>

        ${navHtml(!!state.serviceIdDesign?.available, "Next · Data/Table →")}

      </section>`;



    const inventoryEl = root.querySelector("#programInventory");

    const resultEl = root.querySelector("#sidResult");

    const nextBtn = root.querySelector("#wizNext");

    const loadBtn = root.querySelector("#btnLoadPrograms");

    const validateBtn = root.querySelector("#btnValidateSid");



    async function loadPrograms() {

      if (loadBtn) {

        loadBtn.disabled = true;

        loadBtn.textContent = "조회 중…";

      }

      inventoryEl.innerHTML = `<p class="wb-empty">Program Inventory 조회 중… (${esc(c.system)}/${esc(c.business)}/${esc(c.function)})</p>`;

      try {

        if (!OntologyApi.designPrograms) {

          throw new Error("OntologyApi.designPrograms 미정의 — api.js 캐시를 새로고침하세요");

        }

        const data = await OntologyApi.designPrograms({

          system: c.system || "MG",

          business: c.business || "CO",

          functionCode: c.function || "A",

          function: c.function || "A"

        });

        state.programs = data;

        const rows = (data.programs || [])

          .map((p) => {

            const svcs = (p.services || []).map((s) => s.serviceId).join(", ");

            return `<tr>

              <td><code>${esc(p.programId)}</code></td>

              <td>${esc(p.title || "")}</td>

              <td>${esc(p.table || (Array.isArray(p.tables) ? p.tables.join(",") : ""))}</td>

              <td><code>${esc(svcs)}</code></td>

            </tr>`;

          })

          .join("");

        inventoryEl.innerHTML = `

          <h3>Program Inventory · ${esc(data.system || c.system)}/${esc(data.business || c.business)}/${esc(data.function || c.function)}</h3>

          <p class="wb-empty">used: ${(data.usedProgramNos || []).join(", ") || "(none)"} · proposed(제안): <code>${(data.proposedProgramNos || []).join(", ")}</code></p>

          <table class="wb-table"><thead><tr><th>Program</th><th>Title</th><th>Table</th><th>Services</th></tr></thead>

          <tbody>${rows || '<tr><td colspan="4">프로그램 없음</td></tr>'}</tbody></table>`;

        const programNoInput = root.querySelector("#programNo");

        if (programNoInput && !programNoInput.value && data.proposedProgramNos?.[0]) {

          programNoInput.value = data.proposedProgramNos[0];

        }

        showToast(`Program ${Array.isArray(data.programs) ? data.programs.length : 0}건 조회 완료`);

        inventoryEl.scrollIntoView({ behavior: "smooth", block: "nearest" });

      } catch (e) {

        inventoryEl.innerHTML = `<div class="wb-error">Program 조회 실패: ${esc(e.message)}</div>`;

        showToast(`Program 조회 실패: ${e.message}`);

      } finally {

        if (loadBtn) {

          loadBtn.disabled = false;

          loadBtn.textContent = "축별 Program 현황 조회";

        }

      }

    }



    async function validateSid() {

      if (validateBtn) {

        validateBtn.disabled = true;

        validateBtn.textContent = "검증 중…";

      }

      const programNo = (root.querySelector("#programNo")?.value || "").trim();

      const sequence = (root.querySelector("#sequence")?.value || "").trim() || "0";

      const referenceServiceId = (root.querySelector("#refServiceId")?.value || "").trim();

      try {

        if (!programNo) {

          throw new Error("Program No(4자리)를 입력하세요");

        }

        const res = await OntologyApi.designServiceIdValidate({

          system: c.system || "MG",

          business: c.business || "CO",

          function: c.function || "A",

          programNo,

          transactionType: req.transactionType,

          sequence

        });

        state.serviceIdDesign = {

          ...res,

          programNo,

          sequence,

          referenceServiceId,

          confirmed: false

        };

        state.dataDesign.referenceServiceId = referenceServiceId;

        const findings = (res.findings || [])

          .map((f) => `<li><code>${esc(f.ruleId)}</code> [${esc(f.verdict)}] ${esc(f.message)}</li>`)

          .join("");

        resultEl.innerHTML = `

          <div class="wb-class-preview">

            <div><strong>ProgramId</strong> <code>${esc(res.programId)}</code></div>

            <div><strong>ServiceId</strong> <code>${esc(res.serviceId)}</code> · ${res.available ? "AVAILABLE (제안)" : "REJECTED"}</div>

            <div><strong>packageRoot</strong> <code>${esc(res.packageRoot)}</code></div>

            <ul>${findings}</ul>

            <p class="wb-empty">상태는 PROPOSED입니다. Architect 확정 후에만 개발 착수.</p>

          </div>`;

        if (nextBtn) nextBtn.disabled = !res.available;

        await persistSession(showToast);

        showToast(res.available ? "ServiceId AVAILABLE (제안)" : "ServiceId REJECTED");

      } catch (e) {

        resultEl.innerHTML = `<div class="wb-error">ServiceId 검증 실패: ${esc(e.message)}</div>`;

        showToast(`ServiceId 검증 실패: ${e.message}`);

      } finally {

        if (validateBtn) {

          validateBtn.disabled = false;

          validateBtn.textContent = "중복 검증";

        }

      }

    }



    // Event delegation + direct bind (한 번만 실행되도록 flag)

    let loadingPrograms = false;

    let validatingSid = false;

    const onLoad = (ev) => {

      ev.preventDefault();

      ev.stopPropagation();

      if (loadingPrograms) return;

      loadingPrograms = true;

      Promise.resolve(loadPrograms()).finally(() => {

        loadingPrograms = false;

      });

    };

    const onValidate = (ev) => {

      ev.preventDefault();

      ev.stopPropagation();

      if (validatingSid) return;

      validatingSid = true;

      Promise.resolve(validateSid()).finally(() => {

        validatingSid = false;

      });

    };

    const panel = root.querySelector("#step3Panel");

    if (panel) {

      panel.addEventListener("click", (ev) => {

        const btn = ev.target.closest("[data-action]");

        if (!btn || !panel.contains(btn)) return;

        const action = btn.getAttribute("data-action");

        if (action === "load-programs") onLoad(ev);

        else if (action === "validate-sid") onValidate(ev);

      });

    }



    // 진입 시 자동 조회 (실패해도 버튼은 동작해야 함)

    loadPrograms();



    if (state.serviceIdDesign) {

      resultEl.innerHTML = `<div class="wb-class-preview"><div>현재 제안: <code>${esc(state.serviceIdDesign.serviceId)}</code> (${state.serviceIdDesign.available ? "AVAILABLE" : "REJECTED"})</div></div>`;

    }



    wireCommonNav(root, helpers, async () => {

      if (!state.serviceIdDesign?.available) {

        showToast("사용 가능한 ServiceId를 먼저 검증하세요");

        return;

      }

      state.serviceIdDesign.confirmed = true;

      state.step = 4;

      await paint(root, params, helpers);

    }, () => {

      state.step = 2;

      paint(root, params, helpers);

    });

  }



  function canAdvanceStep4() {
    const dd = state.dataDesign || {};
    return (
      (dd.selectedTables || []).length > 0 ||
      (dd.tableProposals || []).length > 0 ||
      !!dd.tableUnresolved ||
      (!!dd.newTableProposal && String(dd.newTableProposal).trim())
    );
  }

  async function renderStep4(root, params, helpers) {
    const showToast = helpers.showToast || (() => {});
    const c = state.classification || {};
    if (!Array.isArray(state.dataDesign.tableProposals)) state.dataDesign.tableProposals = [];
    if (typeof state.dataDesign.tableUnresolved !== "boolean") state.dataDesign.tableUnresolved = false;

    root.innerHTML = `
      ${stepbarHtml()}
      <section class="wb-panel">
        <h2>STEP 4 · Data / Table Design</h2>
        <p class="wb-empty">Ontology에 있는 Table만 선택합니다. 없으면 [+ 신규 Table 설계]로 NEW_TABLE_PROPOSAL(PROPOSED)을 작성합니다.</p>
        <div class="wb-form-grid">
          <label>Keyword<input id="tableKw" value="${esc(state.requirement?.keyword || state.requirement?.title || "")}"></label>
          <label>Reference ServiceId<input id="tableRef" value="${esc(state.dataDesign.referenceServiceId || state.serviceIdDesign?.referenceServiceId || "")}"></label>
          <div style="grid-column:1/-1">
            <button type="button" class="wb-btn" id="btnSearchTables">Table 검색</button>
            <button type="button" class="wb-btn primary" id="btnNewTable">+ 신규 Table 설계</button>
          </div>
        </div>
        <div id="tableCandidates"></div>
        <h3>Selected Tables</h3>
        <div id="selectedTables"></div>
        <h3>NEW_TABLE_PROPOSAL</h3>
        <div id="tableProposals"></div>
        <label>Join (한 줄에 left.col = right.col)
          <textarea id="joinText" rows="3">${esc((state.dataDesign.joins || []).join("\n"))}</textarea>
        </label>
        ${navHtml(canAdvanceStep4(), "Next · Application →")}
      </section>`;

    const candidatesEl = root.querySelector("#tableCandidates");
    const selectedEl = root.querySelector("#selectedTables");
    const proposalsEl = root.querySelector("#tableProposals");

    function syncNext() {
      const next = root.querySelector("#wizNext");
      if (next) next.disabled = !canAdvanceStep4();
    }

    function openNewTableWizard() {
      if (typeof TableProposalWizard === "undefined" || !TableProposalWizard.open) {
        showToast("table-proposal.js 미로드");
        return;
      }
      TableProposalWizard.open(root, {
        classification: state.classification,
        requirement: state.requirement,
        dataDesign: state.dataDesign,
        showToast,
        onDone: (proposal) => {
          state.dataDesign.tableProposals = state.dataDesign.tableProposals || [];
          state.dataDesign.tableProposals.push(proposal);
          state.dataDesign.tableUnresolved = false;
          state.dataDesign.newTableProposal = null;
          showToast(`PROPOSED 반영: ${proposal.physicalName || proposal.logicalName}`);
          paint(root, params, helpers);
        },
        onCancel: () => {
          paint(root, params, helpers);
        }
      });
    }

    function renderProposals() {
      const list = state.dataDesign.tableProposals || [];
      const unresolved = state.dataDesign.tableUnresolved
        ? `<p class="wb-empty">Table 상태를 <code>UNRESOLVED</code>로 유지 중 — 필요 시 [+ 신규 Table 설계]로 Proposal을 작성하세요.</p>`
        : "";
      if (!list.length) {
        proposalsEl.innerHTML =
          unresolved ||
          `<p class="wb-empty">Proposal 없음. 검색 결과 없을 때 [+ 신규 Table 설계]를 사용하세요.</p>`;
        return;
      }
      proposalsEl.innerHTML =
        unresolved +
        `<table class="wb-table"><thead><tr><th>Physical</th><th>Logical</th><th>Status</th><th>Columns</th><th>PK</th><th></th></tr></thead>
        <tbody>${list
          .map((p, idx) => {
            const pk = Array.isArray(p.primaryKey) ? p.primaryKey.join(", ") : p.primaryKey || "UNRESOLVED";
            return `<tr>
              <td><code>${esc(p.physicalName)}</code></td>
              <td>${esc(p.logicalName)}</td>
              <td><span class="wb-pill is-on">${esc(p.status || "PROPOSED")}</span></td>
              <td>${(p.columns || []).length}</td>
              <td><code>${esc(pk)}</code></td>
              <td><button type="button" class="wb-btn" data-rm-prop="${idx}">Remove</button></td>
            </tr>`;
          })
          .join("")}</tbody></table>`;
      proposalsEl.querySelectorAll("[data-rm-prop]").forEach((btn) => {
        btn.addEventListener("click", () => {
          state.dataDesign.tableProposals.splice(Number(btn.getAttribute("data-rm-prop")), 1);
          renderProposals();
          syncNext();
        });
      });
    }

    function renderSelected() {
      const rows = (state.dataDesign.selectedTables || [])
        .map((t, idx) => {
          const cols = (t.selectColumns || []).join(", ");
          return `<tr>
            <td><code>${esc(t.tableName)}</code></td>
            <td>
              <select data-idx="${idx}" class="accessType">
                ${["READ", "CREATE", "UPDATE", "DELETE", "MIXED"]
                  .map((a) => `<option value="${a}" ${t.accessType === a ? "selected" : ""}>${a}</option>`)
                  .join("")}
              </select>
            </td>
            <td><code>${esc(t.primaryKey || "UNRESOLVED")}</code></td>
            <td>${esc(cols || "(click Detail)")}</td>
            <td>
              <button type="button" class="wb-btn" data-detail="${idx}">Columns</button>
              <button type="button" class="wb-btn" data-rm="${idx}">Remove</button>
            </td>
          </tr>`;
        })
        .join("");
      selectedEl.innerHTML = `
        <table class="wb-table"><thead><tr><th>Table</th><th>Access</th><th>PK</th><th>Columns</th><th></th></tr></thead>
        <tbody>${rows || '<tr><td colspan="5">선택된 Ontology Table 없음</td></tr>'}</tbody></table>
        <div id="columnPicker"></div>`;
      selectedEl.querySelectorAll(".accessType").forEach((sel) => {
        sel.addEventListener("change", () => {
          const i = Number(sel.getAttribute("data-idx"));
          state.dataDesign.selectedTables[i].accessType = sel.value;
          syncNext();
        });
      });
      selectedEl.querySelectorAll("[data-rm]").forEach((btn) => {
        btn.addEventListener("click", () => {
          state.dataDesign.selectedTables.splice(Number(btn.getAttribute("data-rm")), 1);
          renderSelected();
          syncNext();
        });
      });
      selectedEl.querySelectorAll("[data-detail]").forEach((btn) => {
        btn.addEventListener("click", async () => {
          const i = Number(btn.getAttribute("data-detail"));
          const t = state.dataDesign.selectedTables[i];
          try {
            const detail = await OntologyApi.designTableDetail(t.tableName);
            state.tableDetails[t.tableName] = detail;
            t.primaryKey = Array.isArray(detail.table?.pk)
              ? detail.table.pk.join(",")
              : detail.table?.pk || "UNRESOLVED";
            const picker = selectedEl.querySelector("#columnPicker");
            const cols = detail.columns || [];
            picker.innerHTML = `
              <h4>Columns · ${esc(t.tableName)}</h4>
              <div class="wb-col-grid">
                ${cols
                  .map((col) => {
                    const name = col.column || col.name;
                    const checked = (t.selectColumns || []).includes(name) ? "checked" : "";
                    return `<label><input type="checkbox" data-col="${esc(name)}" ${checked}> <code>${esc(name)}</code></label>`;
                  })
                  .join("") || "<p class='wb-empty'>Column 메타 없음 (UNRESOLVED)</p>"}
              </div>
              <button type="button" class="wb-btn primary" id="btnApplyCols">Apply Columns</button>`;
            picker.querySelector("#btnApplyCols")?.addEventListener("click", () => {
              t.selectColumns = Array.from(picker.querySelectorAll("input[data-col]:checked")).map((x) =>
                x.getAttribute("data-col")
              );
              renderSelected();
            });
          } catch (e) {
            showToast(`Table detail 실패: ${e.message}`);
          }
        });
      });
      syncNext();
    }

    function renderNoResult() {
      candidatesEl.innerHTML = `
        <div class="wb-class-preview">
          <p><strong>검색 결과가 없습니다.</strong></p>
          <p class="wb-empty">필요한 Table이 Ontology에 등록되어 있지 않습니다.</p>
          <div class="wb-actions">
            <button type="button" class="wb-btn" id="btnSearchAgain">다시 검색</button>
            <button type="button" class="wb-btn" id="btnKeepUnresolved">UNRESOLVED로 유지</button>
            <button type="button" class="wb-btn primary" id="btnNewTableEmpty">+ 신규 Table 설계</button>
          </div>
        </div>`;
      candidatesEl.querySelector("#btnSearchAgain")?.addEventListener("click", searchTables);
      candidatesEl.querySelector("#btnKeepUnresolved")?.addEventListener("click", () => {
        state.dataDesign.tableUnresolved = true;
        renderProposals();
        syncNext();
        showToast("Table UNRESOLVED로 유지");
      });
      candidatesEl.querySelector("#btnNewTableEmpty")?.addEventListener("click", openNewTableWizard);
    }

    async function searchTables() {
      try {
        const data = await OntologyApi.designTables({
          business: c.business,
          function: c.function,
          keyword: root.querySelector("#tableKw").value.trim(),
          referenceServiceId: root.querySelector("#tableRef").value.trim()
        });
        state.tableCatalog = data.tables || [];
        state.dataDesign.referenceServiceId = root.querySelector("#tableRef").value.trim();
        if (!state.tableCatalog.length) {
          renderNoResult();
          return;
        }
        candidatesEl.innerHTML = `
          <p class="wb-empty">${esc(data.note || "")} · count=${data.count}</p>
          <table class="wb-table"><thead><tr><th>Table</th><th>Source</th><th>PK</th><th></th></tr></thead>
          <tbody>${state.tableCatalog
            .map(
              (t) => `<tr>
              <td><code>${esc(t.tableName)}</code></td>
              <td>${esc(t.source)}</td>
              <td>${esc(Array.isArray(t.pk) ? t.pk.join(",") : t.pk || "UNRESOLVED")}</td>
              <td><button type="button" class="wb-btn" data-add="${esc(t.tableName)}">Select</button></td>
            </tr>`
            )
            .join("")}</tbody></table>
          <div class="wb-actions" style="margin-top:0.5rem">
            <button type="button" class="wb-btn primary" id="btnNewTableFound">+ 신규 Table 설계</button>
          </div>`;
        candidatesEl.querySelectorAll("[data-add]").forEach((btn) => {
          btn.addEventListener("click", () => {
            const name = btn.getAttribute("data-add");
            if (state.dataDesign.selectedTables.some((x) => x.tableName === name)) {
              showToast("이미 선택됨");
              return;
            }
            const hit = state.tableCatalog.find((x) => x.tableName === name);
            state.dataDesign.selectedTables.push({
              tableName: name,
              accessType: operationCode(state.requirement?.transactionType) === "S" ? "READ" : "MIXED",
              primaryKey: Array.isArray(hit?.pk) ? hit.pk.join(",") : hit?.pk || "UNRESOLVED",
              selectColumns: []
            });
            state.dataDesign.tableUnresolved = false;
            renderSelected();
            syncNext();
          });
        });
        candidatesEl.querySelector("#btnNewTableFound")?.addEventListener("click", openNewTableWizard);
      } catch (e) {
        showToast(`Table 검색 실패: ${e.message}`);
      }
    }

    root.querySelector("#btnSearchTables")?.addEventListener("click", searchTables);
    root.querySelector("#btnNewTable")?.addEventListener("click", openNewTableWizard);

    renderSelected();
    renderProposals();
    await searchTables();

    wireCommonNav(root, helpers, async () => {
      state.dataDesign.joins = root
        .querySelector("#joinText")
        .value.split("\n")
        .map((s) => s.trim())
        .filter(Boolean);
      if (!canAdvanceStep4()) {
        showToast("Ontology Table 선택, NEW_TABLE_PROPOSAL, 또는 UNRESOLVED 유지가 필요합니다");
        return;
      }
      await persistSession(showToast);
      state.step = 5;
      await paint(root, params, helpers);
    }, () => {
      state.step = 3;
      paint(root, params, helpers);
    });
  }

  async function renderStep5(root, params, helpers) {

    const showToast = helpers.showToast || (() => {});

    const sid = state.serviceIdDesign || {};

    try {

      state.application = await OntologyApi.designApplication({

        programId: sid.programId,

        serviceId: sid.serviceId,

        referenceServiceId: sid.referenceServiceId || state.dataDesign.referenceServiceId

      });

    } catch (e) {

      showToast(`Application 제안 실패: ${e.message}`);

      state.application = { status: "UNRESOLVED", components: {} };

    }

    const app = state.application || {};

    const comps = app.components || {};

    root.innerHTML = `

      ${stepbarHtml()}

      <section class="wb-panel">

        <h2>STEP 5 · Application Architecture</h2>

        <p class="wb-empty">${esc(app.note || "PDMG 명명 규칙 기반 제안")}</p>

        <div class="wb-class-preview">

          <div><strong>packageRoot</strong> <code>${esc(app.packageRoot)}</code></div>

          <div><strong>Handler</strong> <code>${esc(comps.handler)}</code></div>

          <div><strong>Facade</strong> <code>${esc(comps.facade)}</code></div>

          <div><strong>Service</strong> <code>${esc(comps.service)}</code></div>

          <div><strong>DAO</strong> <code>${esc(comps.dao)}</code></div>

          <div><strong>Mapper</strong> <code>${esc(comps.mapper)}</code></div>

          <div><strong>Rule</strong> <code>${esc(app.rule?.name || "UNRESOLVED")}</code> [${esc(app.rule?.status || "NOT_APPLICABLE")}]</div>

          <pre class="wb-pre">${esc((app.layers || []).join("\n→ "))}</pre>

        </div>

        ${navHtml(true, "Next · Runtime/Policy →")}

      </section>`;

    wireCommonNav(root, helpers, async () => {

      await persistSession(showToast);

      state.step = 6;

      await paint(root, params, helpers);

    }, () => {

      state.step = 4;

      paint(root, params, helpers);

    });

  }



  async function renderStep6(root, params, helpers) {

    const showToast = helpers.showToast || (() => {});

    const req = state.requirement || {};

    try {

      state.policy = await OntologyApi.designPolicy({

        paging: req.paging,

        timeoutPolicy: req.timeoutPolicy,

        personalData: req.personalData,

        pagingKey: "UNRESOLVED"

      });

    } catch (e) {

      showToast(`Policy 제안 실패: ${e.message}`);

      state.policy = { status: "UNRESOLVED" };

    }

    const p = state.policy || {};

    const val = (obj) => {

      if (obj && typeof obj === "object" && "value" in obj) return `${obj.value} [${obj.status || ""}]`;

      return String(obj ?? "UNRESOLVED");

    };

    root.innerHTML = `

      ${stepbarHtml()}

      <section class="wb-panel">

        <h2>STEP 6 · Runtime / Policy</h2>

        <p class="wb-empty">근거 없는 값은 UNRESOLVED로 남깁니다.</p>

        <div class="wb-class-preview">

          <div><strong>Message</strong> req=${esc(val(p.message?.request))} / ok=${esc(val(p.message?.success))} / fail=${esc(val(p.message?.failure))}</div>

          <div><strong>TCF</strong> ${esc(val(p.transaction?.tcf))} · owner ${esc(val(p.transaction?.owner))}</div>

          <div><strong>Timeout</strong> ${esc(val(p.timeout))}</div>

          <div><strong>Paging</strong> enabled=${esc(val(p.paging?.enabled))} key=${esc(val(p.paging?.key))} type=${esc(val(p.paging?.type))}</div>

          <div><strong>Security</strong> personalData=${esc(val(p.security?.personalData))} masking=${esc(val(p.security?.masking))}</div>

          <div><strong>Logging/Audit</strong> ${esc(val(p.logging))} / ${esc(val(p.audit))}</div>

        </div>

        ${navHtml(true, "Next · Gate/Export →")}

      </section>`;

    wireCommonNav(root, helpers, async () => {

      await persistSession(showToast);

      state.step = 7;

      await paint(root, params, helpers);

    }, () => {

      state.step = 5;

      paint(root, params, helpers);

    });

  }



  async function renderStep7(root, params, helpers) {
    const showToast = helpers.showToast || (() => {});
    const navigate = helpers.navigate || ((route) => {
      location.hash = "#/" + route;
    });
    root.innerHTML = `
      ${stepbarHtml()}
      <section class="wb-panel" id="step7Panel">
        <h2>STEP 7 · Architecture Gate / Cursor Export</h2>
        <p class="wb-empty">Gate PASS / PASS_WITH_UNRESOLVED 후 <strong>Done</strong>을 누르면 서버에 COMPLETED로 저장되고 Dashboard Detail에서 조회됩니다. (Ontology VERIFIED 등록 아님)</p>
        <div class="wb-actions" style="margin-bottom:12px">
          <button type="button" class="wb-btn primary" id="btnGate" data-action="run-gate">Run Design Gate</button>
          <button type="button" class="wb-btn" id="btnExportMd" data-action="export-md">Export Markdown</button>
          <button type="button" class="wb-btn" id="btnExportJson" data-action="export-json">Export JSON</button>
        </div>
        <div id="gatePanel"><p class="wb-empty">Gate 결과 대기 중…</p></div>
        <pre class="wb-pre" id="exportPreview" style="max-height:360px;overflow:auto"></pre>
        ${navHtml(false, "Done · 서버 저장")}
      </section>`;

    const gatePanel = root.querySelector("#gatePanel");
    const preview = root.querySelector("#exportPreview");
    const gateBtn = root.querySelector("#btnGate");
    const mdBtn = root.querySelector("#btnExportMd");
    const jsonBtn = root.querySelector("#btnExportJson");
    const wizNext = root.querySelector("#wizNext");
    if (wizNext) {
      wizNext.disabled = true;
      wizNext.textContent = "Done · 서버 저장";
    }

    function syncDoneEnabled() {
      const status = String(state.gate?.status || "").toUpperCase();
      const ok = status === "PASS" || status === "PASS_WITH_UNRESOLVED";
      if (wizNext) wizNext.disabled = !ok;
    }

    async function runGate() {
      if (gateBtn) {
        gateBtn.disabled = true;
        gateBtn.textContent = "Gate 실행 중…";
      }
      gatePanel.innerHTML = `<p class="wb-empty">Design Gate 실행 중…</p>`;
      try {
        const payload = designPayload();
        if (!OntologyApi.designValidate && !OntologyApi.validateDesign) {
          throw new Error("Gate API 미정의 — api.js 캐시를 새로고침하세요");
        }
        let wizGate;
        if (OntologyApi.designValidate) {
          wizGate = await OntologyApi.designValidate(payload);
        } else {
          wizGate = await OntologyApi.validateDesign(payload);
        }
        state.gate = wizGate;
        const findings = (wizGate.findings || [])
          .map((f) => `<li><code>${esc(f.ruleId)}</code> [${esc(f.verdict)}] ${esc(f.target)} — ${esc(f.message)}</li>`)
          .join("");
        gatePanel.innerHTML = `
          <div class="wb-class-preview">
            <div><strong>Gate</strong> ${esc(wizGate.status)} · fail=${esc(wizGate.failCount)} · unresolved=${esc(wizGate.unresolvedCount)}</div>
            <div><strong>scope</strong> ${esc(wizGate.scope || "DESIGN_WIZARD")}</div>
            <ul>${findings || "<li class='wb-empty'>finding 없음</li>"}</ul>
          </div>`;
        await persistSession(showToast);
        syncDoneEnabled();
        showToast(`Gate ${wizGate.status || "DONE"}`);
        gatePanel.scrollIntoView({ behavior: "smooth", block: "nearest" });
        return wizGate;
      } catch (e) {
        gatePanel.innerHTML = `<div class="wb-error">Gate 실패: ${esc(e.message)}</div>`;
        showToast(`Gate 실패: ${e.message}`);
        syncDoneEnabled();
        throw e;
      } finally {
        if (gateBtn) {
          gateBtn.disabled = false;
          gateBtn.textContent = "Run Design Gate";
        }
      }
    }

    async function doExport(format) {
      const btn = format === "json" ? jsonBtn : mdBtn;
      if (btn) {
        btn.disabled = true;
        btn.textContent = "Export 중…";
      }
      try {
        if (!state.gate) {
          await runGate();
        }
        if (!OntologyApi.designExport) {
          throw new Error("Export API 미정의 — api.js 캐시를 새로고침하세요");
        }
        const exp = await OntologyApi.designExport(designPayload(), format);
        state.exportDoc = exp;
        const md = exp.markdown || "";
        if (/\bundefined\b|\[object Object\]|\bNaN\b/.test(md)) {
          throw new Error("Export forbidden token 감지 — 중단");
        }
        preview.textContent = format === "json" ? JSON.stringify(exp.json || designPayload(), null, 2) : md;
        const sid = state.serviceIdDesign?.serviceId || "design";
        if (format === "json") {
          download(`${sid}-context.json`, JSON.stringify(exp.json || designPayload(), null, 2), "application/json");
        } else {
          download(`${sid}-context.md`, md, "text/markdown");
        }
        showToast("Export 완료");
      } catch (e) {
        preview.textContent = `Export 실패: ${e.message}`;
        showToast(`Export 실패: ${e.message}`);
      } finally {
        if (mdBtn) {
          mdBtn.disabled = false;
          mdBtn.textContent = "Export Markdown";
        }
        if (jsonBtn) {
          jsonBtn.disabled = false;
          jsonBtn.textContent = "Export JSON";
        }
      }
    }

    async function completeDesign() {
      const status = String(state.gate?.status || "").toUpperCase();
      if (status !== "PASS" && status !== "PASS_WITH_UNRESOLVED") {
        showToast("Gate PASS / PASS_WITH_UNRESOLVED 후 Done 가능합니다");
        return;
      }
      if (!state.sessionId) {
        await persistSession(showToast);
      }
      if (!OntologyApi.designSessionComplete) {
        throw new Error("Complete API 미정의 — api.js 캐시 새로고침");
      }
      if (wizNext) {
        wizNext.disabled = true;
        wizNext.textContent = "저장 중…";
      }
      try {
        const saved = await OntologyApi.designSessionComplete(state.sessionId, designPayload());
        state.gate = saved.gate || state.gate;
        showToast(`설계 저장 완료 · ${saved.serviceIdDesign?.serviceId || state.serviceIdDesign?.serviceId || saved.sessionId}`);
        navigate("dashboard", { view: "designs", q: saved.serviceIdDesign?.serviceId || state.serviceIdDesign?.serviceId || "" });
      } catch (e) {
        showToast(`Done 저장 실패: ${e.message}`);
        syncDoneEnabled();
        if (wizNext) wizNext.textContent = "Done · 서버 저장";
      }
    }

    let gateRunning = false;
    const panel = root.querySelector("#step7Panel");
    panel?.addEventListener("click", (ev) => {
      const btn = ev.target.closest("[data-action]");
      if (!btn || !panel.contains(btn)) return;
      const action = btn.getAttribute("data-action");
      ev.preventDefault();
      ev.stopPropagation();
      if (action === "run-gate") {
        if (gateRunning) return;
        gateRunning = true;
        Promise.resolve(runGate()).finally(() => {
          gateRunning = false;
        });
      } else if (action === "export-md") {
        doExport("markdown");
      } else if (action === "export-json") {
        doExport("json");
      }
    });

    // 진입 시 자동 Gate (실패해도 버튼은 동작)
    if (state.gate?.status) {
      const findings = (state.gate.findings || [])
        .map((f) => `<li><code>${esc(f.ruleId)}</code> [${esc(f.verdict)}] ${esc(f.target)} — ${esc(f.message)}</li>`)
        .join("");
      gatePanel.innerHTML = `
        <div class="wb-class-preview">
          <div><strong>Gate</strong> ${esc(state.gate.status)} · fail=${esc(state.gate.failCount)} · unresolved=${esc(state.gate.unresolvedCount)}</div>
          <ul>${findings || "<li class='wb-empty'>finding 없음</li>"}</ul>
        </div>`;
      syncDoneEnabled();
    } else {
      gateRunning = true;
      Promise.resolve(runGate()).finally(() => {
        gateRunning = false;
      });
    }

    wireCommonNav(root, helpers, completeDesign, () => {
      state.step = 6;
      paint(root, params, helpers);
    });
  }

  function wireCommonNav(root, helpers, onNext, onBack) {

    root.querySelector("#wizBack")?.addEventListener("click", () => {

      if (onBack) onBack();

    });

    root.querySelector("#wizNext")?.addEventListener("click", async () => {

      if (onNext) await onNext();

    });

  }



  async function paint(root, params, helpers) {

    switch (state.step) {

      case 1:

        renderStep1(root, params, helpers);

        break;

      case 2:

        renderStep2(root, params, helpers);

        break;

      case 3:

        await renderStep3(root, params, helpers);

        break;

      case 4:

        await renderStep4(root, params, helpers);

        break;

      case 5:

        await renderStep5(root, params, helpers);

        break;

      case 6:

        await renderStep6(root, params, helpers);

        break;

      case 7:

        await renderStep7(root, params, helpers);

        break;

      default:

        state.step = 1;

        renderStep1(root, params, helpers);

    }

  }



  async function render(root, params, helpers) {

    state.scheme = await classificationOptions();

    if (!state.sessionId) {

      try {

        const s = await OntologyApi.designSessionCreate({});

        state.sessionId = s.sessionId;

      } catch (e) {

        helpers?.showToast?.(`session 생성 실패(로컬 진행): ${e.message}`);

      }

    }

    await paint(root, params, helpers);

  }



  global.DesignAssistant = { render };

})(window);

