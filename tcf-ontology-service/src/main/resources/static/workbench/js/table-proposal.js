/**
 * STEP 4 · Data/Table Design — New Table Proposal wizard (exearchidoc design 26-08-10-18)
 */
(function (global) {
  const TABLE_TYPES = [
    "MASTER", "DETAIL", "TRANSACTION", "HISTORY", "LOG", "CODE",
    "MAPPING", "TEMPORARY", "SUMMARY", "INTERFACE", "ETC"
  ];
  const DATA_TYPES = ["VARCHAR2", "CHAR", "NUMBER", "DATE", "TIMESTAMP", "CLOB", "BLOB"];
  const ACCESS_TYPES = ["READ", "CREATE", "UPDATE", "DELETE", "READ_WRITE", "MIXED"];
  const YES_NO_UN = ["UNRESOLVED", "YES", "NO"];
  const REL_TYPES = ["ONE_TO_ONE", "ONE_TO_MANY", "MANY_TO_ONE", "MANY_TO_MANY", "REFERENCE", "LOGICAL_JOIN"];

  function esc(v) {
    return String(v ?? "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function opts(list, selected) {
    return list
      .map((v) => `<option value="${esc(v)}" ${String(v) === String(selected) ? "selected" : ""}>${esc(v)}</option>`)
      .join("");
  }

  function emptyProposal(classification, requirement) {
    return {
      mode: "NEW_TABLE_PROPOSAL",
      status: "DRAFT",
      logicalName: "",
      physicalName: "",
      schema: "RDW",
      system: classification?.system || "MG",
      business: classification?.business || "CO",
      function: classification?.function || "A",
      tableType: "MASTER",
      description: requirement?.title || "",
      creationType: "ONLINE",
      retentionValue: 13,
      retentionUnit: "MONTH",
      accessType: "READ",
      hasPersonalData: "UNRESOLVED",
      estimatedRows: "UNRESOLVED",
      columns: [
        {
          logicalName: "",
          physicalName: "",
          dataType: "VARCHAR2",
          length: "20",
          precision: "",
          scale: "",
          primaryKey: true,
          nullable: false,
          defaultValue: "",
          description: "",
          personalData: "UNRESOLVED",
          encryption: "UNRESOLVED",
          masking: "UNRESOLVED",
          role: "PRIMARY_KEY"
        }
      ],
      primaryKey: [],
      indexes: [],
      relations: [],
      filterColumns: [],
      selectColumns: [],
      sort: "",
      paging: "UNRESOLVED",
      pagingKey: "UNRESOLVED"
    };
  }

  function collectProposal(root, draft) {
    const g = (id) => root.querySelector(`#${id}`);
    const proposal = {
      ...draft,
      logicalName: g("ntpLogical")?.value.trim() || draft.logicalName || "",
      physicalName: (g("ntpPhysical")?.value || draft.physicalName || "").trim().toUpperCase(),
      schema: g("ntpSchema")?.value || draft.schema || "RDW",
      system: g("ntpSystem")?.value || draft.system || "MG",
      business: g("ntpBusiness")?.value || draft.business || "CO",
      function: g("ntpFunction")?.value || draft.function || "A",
      tableType: g("ntpType")?.value || draft.tableType || "MASTER",
      description: g("ntpDesc")?.value.trim() || draft.description || "",
      creationType: g("ntpCreation")?.value || draft.creationType || "ONLINE",
      retentionValue: Number(g("ntpRetention")?.value || draft.retentionValue || 0) || null,
      retentionUnit: g("ntpRetentionUnit")?.value || draft.retentionUnit || "MONTH",
      accessType: g("ntpAccess")?.value || draft.accessType || "READ",
      hasPersonalData: g("ntpPersonal")?.value || draft.hasPersonalData || "UNRESOLVED",
      estimatedRows: (g("ntpRows")?.value || draft.estimatedRows || "UNRESOLVED").trim(),
      paging: g("ntpPaging")?.value || draft.paging || "UNRESOLVED",
      pagingKey: (g("ntpPagingKey")?.value || draft.pagingKey || "UNRESOLVED").trim(),
      sort: (g("ntpSort")?.value || draft.sort || "").trim()
    };

    const colBody = root.querySelector("#ntpColBody");
    if (colBody) {
      proposal.columns = [];
      colBody.querySelectorAll("tr").forEach((tr) => {
        const q = (name) => tr.querySelector(`[data-f="${name}"]`);
        const physicalName = (q("physicalName")?.value || "").trim().toUpperCase();
        if (!physicalName && !(q("logicalName")?.value || "").trim()) return;
        proposal.columns.push({
          logicalName: (q("logicalName")?.value || "").trim(),
          physicalName,
          dataType: q("dataType")?.value || "VARCHAR2",
          length: (q("length")?.value || "").trim(),
          precision: (q("precision")?.value || "").trim(),
          scale: (q("scale")?.value || "").trim(),
          primaryKey: !!q("primaryKey")?.checked,
          nullable: !!q("nullable")?.checked,
          defaultValue: (q("defaultValue")?.value || "").trim(),
          description: (q("description")?.value || "").trim(),
          personalData: q("personalData")?.value || "UNRESOLVED",
          encryption: q("encryption")?.value || "UNRESOLVED",
          masking: q("masking")?.value || "UNRESOLVED",
          role: q("role")?.value || ""
        });
      });
    } else {
      proposal.columns = Array.isArray(draft.columns) ? draft.columns.slice() : [];
    }

    proposal.primaryKey = proposal.columns.filter((c) => c.primaryKey).map((c) => c.physicalName);

    const idxBody = root.querySelector("#ntpIdxBody");
    if (idxBody) {
      proposal.indexes = [];
      idxBody.querySelectorAll("tr").forEach((tr) => {
        const q = (name) => tr.querySelector(`[data-f="${name}"]`);
        const indexName = (q("indexName")?.value || "").trim();
        if (!indexName) return;
        proposal.indexes.push({
          indexName,
          indexType: q("indexType")?.value || "NORMAL",
          columns: (q("columns")?.value || "")
            .split(",")
            .map((s) => s.trim().toUpperCase())
            .filter(Boolean),
          unique: !!q("unique")?.checked,
          purpose: (q("purpose")?.value || "").trim(),
          status: "PROPOSED"
        });
      });
    } else {
      proposal.indexes = Array.isArray(draft.indexes) ? draft.indexes.slice() : [];
    }

    const relBody = root.querySelector("#ntpRelBody");
    if (relBody) {
      proposal.relations = [];
      relBody.querySelectorAll("tr").forEach((tr) => {
        const q = (name) => tr.querySelector(`[data-f="${name}"]`);
        const targetTable = (q("targetTable")?.value || "").trim().toUpperCase();
        if (!targetTable) return;
        proposal.relations.push({
          sourceTable: proposal.physicalName,
          sourceColumn: (q("sourceColumn")?.value || "").trim().toUpperCase(),
          relationType: q("relationType")?.value || "LOGICAL_JOIN",
          targetTable,
          targetColumn: (q("targetColumn")?.value || "").trim().toUpperCase(),
          fk: q("fk")?.value || "UNRESOLVED",
          status: "PROPOSED"
        });
      });
    } else {
      proposal.relations = Array.isArray(draft.relations) ? draft.relations.slice() : [];
    }

    if (root.querySelector("[data-usage=filter], [data-usage=select]")) {
      proposal.filterColumns = Array.from(root.querySelectorAll("[data-usage=filter]:checked")).map((x) =>
        x.getAttribute("data-col")
      );
      proposal.selectColumns = Array.from(root.querySelectorAll("[data-usage=select]:checked")).map((x) =>
        x.getAttribute("data-col")
      );
    } else {
      proposal.filterColumns = Array.isArray(draft.filterColumns) ? draft.filterColumns.slice() : [];
      proposal.selectColumns = Array.isArray(draft.selectColumns) ? draft.selectColumns.slice() : [];
    }
    return proposal;
  }

  function columnRowHtml(col, idx) {
    const c = col || {};
    return `<tr data-idx="${idx}">
      <td>${idx + 1}</td>
      <td><input data-f="logicalName" value="${esc(c.logicalName || "")}"></td>
      <td><input data-f="physicalName" value="${esc(c.physicalName || "")}" placeholder="CUST_NO"></td>
      <td><select data-f="dataType">${opts(DATA_TYPES, c.dataType || "VARCHAR2")}</select></td>
      <td><input data-f="length" value="${esc(c.length || "")}" style="width:4rem"></td>
      <td><input data-f="precision" value="${esc(c.precision || "")}" style="width:3rem"></td>
      <td><input data-f="scale" value="${esc(c.scale || "")}" style="width:3rem"></td>
      <td style="text-align:center"><input data-f="primaryKey" type="checkbox" ${c.primaryKey ? "checked" : ""}></td>
      <td style="text-align:center"><input data-f="nullable" type="checkbox" ${c.nullable ? "checked" : ""}></td>
      <td><input data-f="defaultValue" value="${esc(c.defaultValue || "")}"></td>
      <td><select data-f="personalData">${opts(YES_NO_UN, c.personalData || "UNRESOLVED")}</select></td>
      <td><select data-f="encryption">${opts(YES_NO_UN, c.encryption || "UNRESOLVED")}</select></td>
      <td><select data-f="masking">${opts(YES_NO_UN, c.masking || "UNRESOLVED")}</select></td>
      <td><button type="button" class="wb-btn" data-action="del-col" data-idx="${idx}">삭제</button></td>
      <td style="display:none"><input data-f="description" value="${esc(c.description || "")}">
        <input data-f="role" value="${esc(c.role || "")}"></td>
    </tr>`;
  }

  function indexRowHtml(idx, item) {
    const i = item || {};
    return `<tr>
      <td><input data-f="indexName" value="${esc(i.indexName || "")}" placeholder="IDX_..."></td>
      <td><select data-f="indexType">${opts(["NORMAL", "UNIQUE", "BITMAP"], i.indexType || "NORMAL")}</select></td>
      <td><input data-f="columns" value="${esc((i.columns || []).join(", "))}" placeholder="COL1, COL2"></td>
      <td style="text-align:center"><input data-f="unique" type="checkbox" ${i.unique ? "checked" : ""}></td>
      <td><input data-f="purpose" value="${esc(i.purpose || "")}"></td>
      <td><button type="button" class="wb-btn" data-action="del-idx">삭제</button></td>
    </tr>`;
  }

  function relationRowHtml(item, sourceCols) {
    const r = item || {};
    return `<tr>
      <td><select data-f="sourceColumn">${opts(sourceCols.length ? sourceCols : [""], r.sourceColumn || "")}</select></td>
      <td><select data-f="relationType">${opts(REL_TYPES, r.relationType || "LOGICAL_JOIN")}</select></td>
      <td><input data-f="targetTable" value="${esc(r.targetTable || "")}" placeholder="TB_..."></td>
      <td><input data-f="targetColumn" value="${esc(r.targetColumn || "")}"></td>
      <td><select data-f="fk">${opts(YES_NO_UN, r.fk || "UNRESOLVED")}</select></td>
      <td><button type="button" class="wb-btn" data-action="del-rel">삭제</button></td>
    </tr>`;
  }

  async function open(root, ctx) {
    const {
      classification,
      requirement,
      dataDesign,
      showToast,
      onDone,
      onCancel
    } = ctx;
    const draft = dataDesign.draftProposal || emptyProposal(classification, requirement);
    dataDesign.draftProposal = draft;
    let sub = 2; // 4-2 basic ... 4-7 review mapped as 2..7

    function paint() {
      const titles = {
        2: "4-2 New Table Basic",
        3: "4-3 Column Design",
        4: "4-4 Key / Index",
        5: "4-5 Relation",
        6: "4-6 Access / Security / Capacity",
        7: "4-7 Review"
      };

      let body = "";
      if (sub === 2) {
        body = `
          <div class="wb-form-grid">
            <label style="grid-column:1/-1">논리 테이블명 *<input id="ntpLogical" value="${esc(draft.logicalName)}"></label>
            <label style="grid-column:1/-1">물리 테이블명 *<input id="ntpPhysical" value="${esc(draft.physicalName)}" placeholder="TB_MK_CO_A_..."></label>
            <label>Schema<select id="ntpSchema">${opts(["RDW", "APP", "ETC"], draft.schema)}</select></label>
            <label>System<input id="ntpSystem" value="${esc(draft.system)}"></label>
            <label>Business<input id="ntpBusiness" value="${esc(draft.business)}"></label>
            <label>Function<input id="ntpFunction" value="${esc(draft.function)}"></label>
            <label>Table 유형<select id="ntpType">${opts(TABLE_TYPES, draft.tableType)}</select></label>
            <label>생성주기<select id="ntpCreation">${opts(["ONLINE", "BATCH", "ETL", "MANUAL"], draft.creationType)}</select></label>
            <label>보존기간<input id="ntpRetention" type="number" value="${esc(draft.retentionValue || "")}"></label>
            <label>단위<select id="ntpRetentionUnit">${opts(["DAY", "MONTH", "YEAR"], draft.retentionUnit)}</select></label>
            <label style="grid-column:1/-1">설명 *<textarea id="ntpDesc" rows="3">${esc(draft.description)}</textarea></label>
          </div>`;
      } else if (sub === 3) {
        body = `
          <div class="wb-table-wrap">
            <table class="wb-table wb-col-editor">
              <thead><tr>
                <th>#</th><th>논리명</th><th>물리 Column</th><th>Type</th><th>Len</th><th>Prec</th><th>Scale</th>
                <th>PK</th><th>Null</th><th>Default</th><th>개인정보</th><th>Enc</th><th>Mask</th><th></th>
              </tr></thead>
              <tbody id="ntpColBody">${(draft.columns || []).map((c, i) => columnRowHtml(c, i)).join("")}</tbody>
            </table>
          </div>
          <div class="wb-actions" style="margin-top:0.5rem">
            <button type="button" class="wb-btn" data-action="add-col">+ Column 추가</button>
          </div>`;
      } else if (sub === 4) {
        const pk = (draft.columns || []).filter((c) => c.primaryKey).map((c) => c.physicalName);
        body = `
          <div class="wb-class-preview">
            <div><strong>PK Name</strong> <code>PK_${esc(draft.physicalName || "TABLE")}</code></div>
            <div><strong>PK Columns</strong> ${(pk.length ? pk : ["(Column에서 PK 체크)"]).map((c) => `<code>${esc(c)}</code>`).join(" ")}</div>
            <p class="wb-empty">Composite PK는 Column 행의 PK 체크박스로 지정합니다. 문자열 배열로 합치지 않습니다.</p>
          </div>
          <h3>Indexes</h3>
          <div class="wb-table-wrap">
            <table class="wb-table">
              <thead><tr><th>Name</th><th>Type</th><th>Columns</th><th>Unique</th><th>Purpose</th><th></th></tr></thead>
              <tbody id="ntpIdxBody">${(draft.indexes || []).map((i, n) => indexRowHtml(n, i)).join("") || ""}</tbody>
            </table>
          </div>
          <div class="wb-actions"><button type="button" class="wb-btn" data-action="add-idx">+ Index 추가</button></div>`;
      } else if (sub === 5) {
        const cols = (draft.columns || []).map((c) => c.physicalName).filter(Boolean);
        body = `
          <div class="wb-table-wrap">
            <table class="wb-table">
              <thead><tr><th>Source Col</th><th>Relation</th><th>Target Table</th><th>Target Col</th><th>FK</th><th></th></tr></thead>
              <tbody id="ntpRelBody">${(draft.relations || []).map((r) => relationRowHtml(r, cols)).join("")}</tbody>
            </table>
          </div>
          <div class="wb-actions"><button type="button" class="wb-btn" data-action="add-rel">+ Relation 추가</button></div>`;
      } else if (sub === 6) {
        const cols = draft.columns || [];
        body = `
          <div class="wb-form-grid">
            <label>Access Type<select id="ntpAccess">${opts(ACCESS_TYPES, draft.accessType)}</select></label>
            <label>개인정보 포함<select id="ntpPersonal">${opts(YES_NO_UN, draft.hasPersonalData)}</select></label>
            <label>예상 건수<input id="ntpRows" value="${esc(draft.estimatedRows || "UNRESOLVED")}"></label>
            <label>Paging<select id="ntpPaging">${opts(["UNRESOLVED", "YES", "NO"], draft.paging)}</select></label>
            <label>Paging Key<input id="ntpPagingKey" value="${esc(draft.pagingKey || "UNRESOLVED")}"></label>
            <label style="grid-column:1/-1">Sort<input id="ntpSort" value="${esc(draft.sort || "")}" placeholder="REG_DTM DESC"></label>
          </div>
          <h3>Filter / Select Columns</h3>
          <div class="wb-col-grid">
            ${cols
              .map((c) => {
                const n = c.physicalName;
                if (!n) return "";
                const fChk = (draft.filterColumns || []).includes(n) ? "checked" : "";
                const sChk = (draft.selectColumns || []).includes(n) || c.primaryKey ? "checked" : "";
                return `<div>
                  <code>${esc(n)}</code>
                  <label><input type="checkbox" data-usage="filter" data-col="${esc(n)}" ${fChk}> Filter</label>
                  <label><input type="checkbox" data-usage="select" data-col="${esc(n)}" ${sChk}> Select</label>
                </div>`;
              })
              .join("")}
          </div>
          <p class="wb-empty">개인정보 미확인은 NO가 아니라 UNRESOLVED로 유지합니다.</p>`;
      } else {
        const p = draft;
        const pk = (p.columns || []).filter((c) => c.primaryKey).map((c) => c.physicalName);
        body = `
          <div class="wb-class-preview">
            <div><strong>Mode</strong> NEW_TABLE_PROPOSAL · Status <code>PROPOSED</code> (not VERIFIED)</div>
            <div><strong>Logical</strong> ${esc(p.logicalName)}</div>
            <div><strong>Physical</strong> <code>${esc(p.physicalName)}</code> / ${esc(p.schema)}</div>
            <div><strong>Axis</strong> ${esc(p.system)}/${esc(p.business)}/${esc(p.function)} · ${esc(p.tableType)}</div>
            <div><strong>Access</strong> ${esc(p.accessType)} · personalData=${esc(p.hasPersonalData)}</div>
            <div><strong>PK</strong> ${pk.map((x) => `<code>${esc(x)}</code>`).join(" ") || "UNRESOLVED"}</div>
            <div><strong>Columns</strong> ${(p.columns || []).length} · Indexes ${(p.indexes || []).length} · Relations ${(p.relations || []).length}</div>
            <pre class="wb-pre" style="max-height:220px;overflow:auto">${esc(JSON.stringify(p, null, 2))}</pre>
          </div>
          <div id="ntpValidateResult"></div>`;
      }

      root.innerHTML = `
        <section class="wb-panel" id="ntpPanel">
          <h2>New Table Proposal · ${esc(titles[sub])}</h2>
          <p class="wb-empty">신규 Table은 Ontology VERIFIED로 등록하지 않습니다. Proposal(PROPOSED)만 생성합니다.</p>
          <div class="wb-actions" style="margin-bottom:0.75rem">
            ${[2, 3, 4, 5, 6, 7]
              .map((n) => `<button type="button" class="wb-pill ${n === sub ? "is-on" : ""}" data-action="goto" data-sub="${n}">4-${n}</button>`)
              .join("")}
          </div>
          ${body}
          <div class="wb-wizard-nav">
            <button type="button" class="wb-btn" data-action="cancel">취소</button>
            <div class="wb-actions">
              <button type="button" class="wb-btn" data-action="prev" ${sub <= 2 ? "disabled" : ""}>← 이전</button>
              ${sub < 7
                ? `<button type="button" class="wb-btn primary" data-action="next">다음 →</button>`
                : `<button type="button" class="wb-btn" data-action="validate">Validation</button>
                   <button type="button" class="wb-btn primary" data-action="submit">PROPOSED 반영</button>`}
            </div>
          </div>
        </section>`;

      if (sub !== 2) {
        root.insertAdjacentHTML(
          "beforeend",
          `<div hidden>
            <input id="ntpLogical" value="${esc(draft.logicalName)}">
            <input id="ntpPhysical" value="${esc(draft.physicalName)}">
            <input id="ntpSchema" value="${esc(draft.schema)}">
            <input id="ntpSystem" value="${esc(draft.system)}">
            <input id="ntpBusiness" value="${esc(draft.business)}">
            <input id="ntpFunction" value="${esc(draft.function)}">
            <input id="ntpType" value="${esc(draft.tableType)}">
            <textarea id="ntpDesc">${esc(draft.description)}</textarea>
            <input id="ntpCreation" value="${esc(draft.creationType)}">
            <input id="ntpRetention" value="${esc(draft.retentionValue || "")}">
            <input id="ntpRetentionUnit" value="${esc(draft.retentionUnit)}">
          </div>`
        );
      }
      if (sub !== 6) {
        root.insertAdjacentHTML(
          "beforeend",
          `<div hidden>
            <input id="ntpAccess" value="${esc(draft.accessType)}">
            <input id="ntpPersonal" value="${esc(draft.hasPersonalData)}">
            <input id="ntpRows" value="${esc(draft.estimatedRows)}">
            <input id="ntpPaging" value="${esc(draft.paging)}">
            <input id="ntpPagingKey" value="${esc(draft.pagingKey)}">
            <input id="ntpSort" value="${esc(draft.sort)}">
          </div>`
        );
      }

      wire();
    }

    function wire() {
      const panel = root.querySelector("#ntpPanel");
      panel?.addEventListener("click", async (ev) => {
        const btn = ev.target.closest("[data-action]");
        if (!btn) return;
        const action = btn.getAttribute("data-action");
        ev.preventDefault();
        Object.assign(draft, collectProposal(root, draft));
        dataDesign.draftProposal = draft;

        if (action === "cancel") {
          onCancel?.();
          return;
        }
        if (action === "goto") {
          sub = Number(btn.getAttribute("data-sub"));
          paint();
          return;
        }
        if (action === "prev") {
          sub = Math.max(2, sub - 1);
          paint();
          return;
        }
        if (action === "next") {
          if (sub === 2 && (!draft.logicalName || !draft.physicalName || !draft.description)) {
            showToast?.("논리명 / 물리명 / 설명은 필수입니다");
            return;
          }
          if (sub === 3 && !(draft.columns || []).length) {
            showToast?.("Column을 최소 1개 입력하세요");
            return;
          }
          sub = Math.min(7, sub + 1);
          paint();
          return;
        }
        if (action === "add-col") {
          draft.columns = draft.columns || [];
          draft.columns.push({
            logicalName: "",
            physicalName: "",
            dataType: "VARCHAR2",
            length: "20",
            primaryKey: false,
            nullable: true,
            personalData: "UNRESOLVED",
            encryption: "UNRESOLVED",
            masking: "UNRESOLVED"
          });
          paint();
          return;
        }
        if (action === "del-col") {
          const i = Number(btn.getAttribute("data-idx"));
          draft.columns.splice(i, 1);
          paint();
          return;
        }
        if (action === "add-idx") {
          draft.indexes = draft.indexes || [];
          draft.indexes.push({ indexName: "", indexType: "NORMAL", columns: [], unique: false, purpose: "" });
          paint();
          return;
        }
        if (action === "del-idx") {
          btn.closest("tr")?.remove();
          Object.assign(draft, collectProposal(root, draft));
          return;
        }
        if (action === "add-rel") {
          draft.relations = draft.relations || [];
          draft.relations.push({
            sourceColumn: "",
            relationType: "LOGICAL_JOIN",
            targetTable: "",
            targetColumn: "",
            fk: "UNRESOLVED"
          });
          paint();
          return;
        }
        if (action === "del-rel") {
          btn.closest("tr")?.remove();
          Object.assign(draft, collectProposal(root, draft));
          return;
        }
        if (action === "validate" || action === "submit") {
          const proposal = collectProposal(root, draft);
          Object.assign(draft, proposal);
          dataDesign.draftProposal = draft;
          try {
            const res =
              action === "submit"
                ? await OntologyApi.tableProposalCreate(proposal)
                : await OntologyApi.tableProposalValidate(proposal);
            const validation = res.validation || res;
            const findings = (validation.findings || [])
              .map((f) => `<li><code>${esc(f.ruleId)}</code> [${esc(f.verdict)}] ${esc(f.message)}</li>`)
              .join("");
            const box = root.querySelector("#ntpValidateResult");
            if (box) {
              box.innerHTML = `<div class="wb-class-preview">
                <div><strong>Validation</strong> ${esc(validation.status)} · fail=${esc(validation.failCount)} · unresolved=${esc(validation.unresolvedCount)}</div>
                <ul>${findings}</ul>
              </div>`;
            }
            showToast?.(`Table Proposal ${validation.status}`);
            if (action === "submit" && res.accepted) {
              dataDesign.draftProposal = null;
              onDone?.(res.proposal || proposal);
            } else if (action === "submit" && !res.accepted) {
              showToast?.("Validation FAIL — Proposal 미반영");
            }
          } catch (e) {
            showToast?.(`Proposal 실패: ${e.message}`);
          }
        }
      });
    }

    paint();
  }

  global.TableProposalWizard = { open, emptyProposal };
})(window);
