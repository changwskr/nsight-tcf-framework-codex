/* Shared API client — BootRun (/) and Tomcat context (/tcf-ontology-service) both work. */
(function (global) {
  const DEFAULT_TIMEOUT_MS = 12000;

  function resolveContextPath() {
    const marker = "/workbench/";
    const path = window.location.pathname || "";
    const idx = path.indexOf(marker);
    if (idx >= 0) {
      return path.substring(0, idx);
    }
    // fallback: /tcf-ontology-service/workbench/index.html without trailing marker match
    const m = path.match(/^(.*?)\/workbench(?:\/|$)/);
    return m ? m[1] : "";
  }

  const APP_CONTEXT = resolveContextPath();

  function apiUrl(path) {
    if (!path) return APP_CONTEXT || "/";
    return `${APP_CONTEXT}${path.startsWith("/") ? path : "/" + path}`;
  }

  async function apiGet(path, options = {}) {
    const timeoutMs = options.timeoutMs ?? DEFAULT_TIMEOUT_MS;
    const url = apiUrl(path);
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);
    try {
      const res = await fetch(url, {
        method: "GET",
        headers: { Accept: "application/json" },
        signal: controller.signal
      });
      const text = await res.text();
      let body = null;
      try {
        body = text ? JSON.parse(text) : null;
      } catch {
        body = { raw: text };
      }
      if (!res.ok) {
        const msg = (body && (body.error || body.message)) || `HTTP ${res.status}`;
        const err = new Error(msg);
        err.status = res.status;
        err.body = body;
        err.url = url;
        throw err;
      }
      return body;
    } catch (e) {
      if (e.name === "AbortError") {
        const err = new Error(`요청 시간 초과 (${timeoutMs}ms): ${url}`);
        err.code = "TIMEOUT";
        throw err;
      }
      throw e;
    } finally {
      clearTimeout(timer);
    }
  }

  async function apiPost(path, body) {
    const url = apiUrl(path);
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), DEFAULT_TIMEOUT_MS);
    try {
      const res = await fetch(url, {
        method: "POST",
        headers: { Accept: "application/json", "Content-Type": "application/json" },
        body: JSON.stringify(body || {}),
        signal: controller.signal
      });
      const text = await res.text();
      const parsed = text ? JSON.parse(text) : null;
      if (!res.ok) throw new Error((parsed && parsed.error) || `HTTP ${res.status}`);
      return parsed;
    } catch (e) {
      if (e.name === "AbortError") throw new Error(`요청 시간 초과: ${url}`);
      throw e;
    } finally {
      clearTimeout(timer);
    }
  }

  async function apiPut(path, body) {
    const url = apiUrl(path);
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), DEFAULT_TIMEOUT_MS);
    try {
      const res = await fetch(url, {
        method: "PUT",
        headers: { Accept: "application/json", "Content-Type": "application/json" },
        body: JSON.stringify(body || {}),
        signal: controller.signal
      });
      const text = await res.text();
      const parsed = text ? JSON.parse(text) : null;
      if (!res.ok) throw new Error((parsed && (parsed.error || parsed.message)) || `HTTP ${res.status}`);
      return parsed;
    } catch (e) {
      if (e.name === "AbortError") throw new Error(`요청 시간 초과: ${url}`);
      throw e;
    } finally {
      clearTimeout(timer);
    }
  }

  const OntologyApi = {
    contextPath: APP_CONTEXT,
    catalog: () => apiGet("/api/ontology/catalog"),
    dashboardSummary: () => apiGet("/api/ontology/dashboard"),
    dashboardDetail: (view, params = {}) => {
      const q = new URLSearchParams();
      Object.entries(params).forEach(([k, v]) => {
        if (v !== undefined && v !== null && String(v) !== "") q.set(k, String(v));
      });
      const qs = q.toString();
      return apiGet(`/api/ontology/dashboard/${encodeURIComponent(view)}${qs ? `?${qs}` : ""}`);
    },
    concepts: (params = {}) => {
      const q = new URLSearchParams();
      Object.entries(params).forEach(([k, v]) => {
        if (v !== undefined && v !== null && String(v) !== "") q.set(k, String(v));
      });
      const qs = q.toString();
      return apiGet(`/api/ontology/v1/concepts${qs ? `?${qs}` : ""}`);
    },
    consistency: () => apiGet("/api/ontology/consistency"),
    validateRules: () => apiGet("/api/ontology/validate/rules"),
    serviceStructure: (serviceId) =>
      apiGet(`/api/ontology/query/service/${encodeURIComponent(serviceId)}/structure`),
    serviceTables: (serviceId) =>
      apiGet(`/api/ontology/query/service/${encodeURIComponent(serviceId)}/tables`),
    programServices: (programId) =>
      apiGet(`/api/ontology/query/program/${encodeURIComponent(programId)}/services`),
    handlerServices: (handler) =>
      apiGet(`/api/ontology/query/handler/${encodeURIComponent(handler)}/services`),
    tableServices: (table) =>
      apiGet(`/api/ontology/query/table/${encodeURIComponent(table)}/services`),
    impactTable: (table) =>
      apiGet(`/api/ontology/impact/table/${encodeURIComponent(table)}`),
    concept: (id) => apiGet(`/api/ontology/v1/concept/${encodeURIComponent(id)}`),
    runtimeTxChain: () => apiGet("/api/ontology/runtime/tx-chain"),
    meta: () => apiGet("/api/ontology/v1/meta"),
    recommend: (params = {}) => {
      const q = new URLSearchParams();
      Object.entries(params).forEach(([k, v]) => {
        if (v !== undefined && v !== null && String(v) !== "") q.set(k, String(v));
      });
      return apiGet(`/api/ontology/recommend?${q.toString()}`);
    },
    designRecommend: (body) => apiPost("/api/ontology/design/recommend", body),
    designSessionCreate: (body) => apiPost("/api/ontology/design/session", body),
    designSessionGet: (sessionId) =>
      apiGet(`/api/ontology/design/session/${encodeURIComponent(sessionId)}`),
    designSessionPut: (sessionId, body) =>
      apiPut(`/api/ontology/design/session/${encodeURIComponent(sessionId)}`, body),
    designSessions: () => apiGet("/api/ontology/design/sessions"),
    designSessionComplete: (sessionId, body) =>
      apiPost(`/api/ontology/design/session/${encodeURIComponent(sessionId)}/complete`, body || {}),
    designServiceIdValidate: (body) => apiPost("/api/ontology/design/service-id/validate", body),
    designPrograms: (params = {}) => {
      const q = new URLSearchParams();
      Object.entries(params).forEach(([k, v]) => {
        if (v !== undefined && v !== null && String(v) !== "") q.set(k, String(v));
      });
      return apiGet(`/api/ontology/design/programs?${q.toString()}`);
    },
    designTables: (params = {}) => {
      const q = new URLSearchParams();
      Object.entries(params).forEach(([k, v]) => {
        if (v !== undefined && v !== null && String(v) !== "") q.set(k, String(v));
      });
      return apiGet(`/api/ontology/design/tables?${q.toString()}`);
    },
    designTableDetail: (tableName) =>
      apiGet(`/api/ontology/design/table/${encodeURIComponent(tableName)}`),
    designTableColumns: (tableName) =>
      apiGet(`/api/ontology/design/table/${encodeURIComponent(tableName)}/columns`),
    designApplication: (body) => apiPost("/api/ontology/design/application", body),
    designPolicy: (body) => apiPost("/api/ontology/design/policy", body),
    designValidate: (body) => apiPost("/api/ontology/design/validate", body),
    tableProposalValidate: (body) => apiPost("/api/ontology/design/table-proposal/validate", body),
    tableProposalCreate: (body) => apiPost("/api/ontology/design/table-proposal", body),
    tableProposalGet: (id) => apiGet(`/api/ontology/design/table-proposal/${encodeURIComponent(id)}`),
    tableProposalUpdate: (id, body) =>
      apiPut(`/api/ontology/design/table-proposal/${encodeURIComponent(id)}`, body),
    designExport: (body, format = "markdown") =>
      apiPost(`/api/ontology/design/export?format=${encodeURIComponent(format)}`, body),
    designExportSession: (sessionId, format = "markdown") =>
      apiGet(`/api/ontology/design/export/${encodeURIComponent(sessionId)}?format=${encodeURIComponent(format)}`),
    knowledgeCatalog: (params = {}) => {
      const q = new URLSearchParams();
      Object.entries(params).forEach(([k, v]) => {
        if (v !== undefined && v !== null && String(v) !== "") q.set(k, String(v));
      });
      const qs = q.toString();
      return apiGet(`/api/ontology/knowledge${qs ? `?${qs}` : ""}`);
    },
    knowledgeDoc: (id) => apiGet(`/api/ontology/knowledge/doc/${encodeURIComponent(id)}`),
    knowledgeSearch: (q, limit = 8) =>
      apiGet(`/api/ontology/knowledge/search?q=${encodeURIComponent(q)}&limit=${encodeURIComponent(limit)}`),
    qnaAsk: (question, topK = 5) => apiPost("/api/ontology/qna/ask", { question, topK }),
    classificationScheme: () => apiGet("/api/ontology/bundle/business/classification.yml"),
    validateService: (serviceId) =>
      apiGet(`/api/ontology/validate/service/${encodeURIComponent(serviceId)}`),
    validateDesignBaseline: (body) => apiPost("/api/ontology/validate/design-baseline", body),
    validateDesign: (body) => apiPost("/api/ontology/validate/design", body),
    promptJson: (id) => apiGet(`/api/ontology/prompt/${encodeURIComponent(id)}`),
    promptMarkdown: async (id) => {
      const path = apiUrl(`/api/ontology/prompt/${encodeURIComponent(id)}.md`);
      const controller = new AbortController();
      const timer = setTimeout(() => controller.abort(), DEFAULT_TIMEOUT_MS);
      try {
        const res = await fetch(path, { signal: controller.signal, headers: { Accept: "text/markdown" } });
        const text = await res.text();
        if (!res.ok) throw new Error(text || `HTTP ${res.status}`);
        return text;
      } catch (e) {
        if (e.name === "AbortError") throw new Error(`요청 시간 초과: ${path}`);
        throw e;
      } finally {
        clearTimeout(timer);
      }
    }
  };

  global.OntologyApi = OntologyApi;
  global.apiGet = apiGet;
  global.apiUrl = apiUrl;
  global.APP_CONTEXT = APP_CONTEXT;
})(window);
