(async function () {
  const metaLine = document.getElementById('metaLine');
  const kpiAsset = document.getElementById('kpiAsset');
  const kpiGroup = document.getElementById('kpiGroup');
  const kpiSystem = document.getElementById('kpiSystem');
  const kpiDb = document.getElementById('kpiDb');
  const kpiMw = document.getElementById('kpiMw');
  const kpiEp = document.getElementById('kpiEp');
  const kpiChange = document.getElementById('kpiChange');
  const kpiConfirmed = document.getElementById('kpiConfirmed');
  const previewBody = document.getElementById('previewBody');
  const previewCount = document.getElementById('previewCount');
  const reloadBtn = document.getElementById('reloadBtn');

  async function load() {
    metaLine.textContent = '조회 중…';
    const res = await InfraApi.postService('ifina0100S0', { recentLimit: 10 }, 'INF-010');
    const dto = res.dto || {};
    kpiAsset.textContent = dto.assetCount != null ? dto.assetCount : '-';
    kpiGroup.textContent = dto.groupCount != null ? dto.groupCount : '-';
    kpiSystem.textContent = dto.systemCount != null ? dto.systemCount : '-';
    if (kpiDb) kpiDb.textContent = dto.dbCount != null ? dto.dbCount : '-';
    if (kpiMw) kpiMw.textContent = dto.mwCount != null ? dto.mwCount : '-';
    if (kpiEp) kpiEp.textContent = dto.endpointCount != null ? dto.endpointCount : '-';
    if (kpiChange) kpiChange.textContent = dto.changeLogCount != null ? dto.changeLogCount : '-';
    if (kpiConfirmed) {
      kpiConfirmed.textContent =
        (dto.confirmedCount != null ? dto.confirmedCount : '-') +
        ' / R' + (dto.retiredCount != null ? dto.retiredCount : '-');
    }

    const rows = Array.isArray(dto.recentAssets) ? dto.recentAssets : [];
    previewCount.textContent = `${rows.length}건 · 자산 ${dto.assetCount || 0}`;
    metaLine.textContent =
      `HTTP ${res.httpStatus} · ${res.elapsedMs}ms · ifina0100S0 · 파일럿자산 ${dto.pilotAssetCount || 0}`;

    if (!rows.length) {
      previewBody.innerHTML = '<tr><td colspan="6" class="empty">데이터 없음</td></tr>';
      return;
    }
    previewBody.innerHTML = rows.map((row) => `
      <tr>
        <td class="mono">${InfraApi.escapeHtml(row.assetId)}</td>
        <td>${InfraApi.escapeHtml(row.assetName)}</td>
        <td>${InfraApi.escapeHtml(row.techRoleCd)}</td>
        <td>${InfraApi.escapeHtml(row.envCd)}</td>
        <td class="mono">${InfraApi.escapeHtml(row.systemId)}</td>
        <td>${InfraApi.statusBadge(row.statusCd)}</td>
      </tr>
    `).join('');
  }

  reloadBtn.addEventListener('click', () => load().catch(console.error));
  await load();
})();
