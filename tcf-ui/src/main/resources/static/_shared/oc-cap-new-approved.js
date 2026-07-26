(function () {
    const pendingEl = document.getElementById('pendingList');
    const approvedEl = document.getElementById('approvedList');
    const historyBody = document.getElementById('historyBody');
    const msgEl = document.getElementById('capNewMsg');

    const params = new URLSearchParams(location.search);
    const focusId = params.get('id');

    function showMsg(text, ok) {
        if (!msgEl) return;
        msgEl.textContent = text;
        msgEl.className = 'oc-capnew-msg ' + (ok ? 'oc-capnew-msg--ok' : 'oc-capnew-msg--error');
        msgEl.hidden = !text;
    }

    function escapeHtml(v) {
        return String(v ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

    function renderPending(items) {
        const pending = items.filter(i => i.status === 'COMPLETED');
        if (!pending.length) {
            pendingEl.innerHTML = '<p>확정 대기 시나리오가 없습니다.</p>';
            return;
        }
        pendingEl.innerHTML = pending.map(item => `
            <article class="oc-capnew-card" data-id="${escapeHtml(item.scenarioId)}">
                <h3>${escapeHtml(item.scenarioName)} <span class="oc-capnew-status oc-capnew-status--completed">COMPLETED</span></h3>
                <p>${escapeHtml(item.projectName || '')} · ${escapeHtml(item.targetEnv || '')} · ${escapeHtml(item.versionNo || '')}</p>
                <div class="oc-capnew-form-grid" style="margin-top:0.75rem;">
                    <label>확정자 *<input class="f_approver" type="text" placeholder="홍길동"></label>
                    <label>검토자<input class="f_reviewer" type="text" placeholder="운영(PROD) 시 필수"></label>
                    <label style="grid-column:1/-1">확정 메모<textarea class="f_note" rows="2" placeholder="검토 의견"></textarea></label>
                    <label><input class="f_override" type="checkbox"> 위험(CRITICAL) 판정 사유 확정</label>
                </div>
                <p style="margin-top:0.75rem;">
                    <button type="button" class="tcf-btn btn-approve">최종 확정</button>
                    <button type="button" class="tcf-btn tcf-btn--ghost" data-capnew-env-handoff data-scenario-id="${escapeHtml(item.scenarioId)}">ENV 점검</button>
                    <button type="button" class="tcf-btn tcf-btn--ghost" data-capnew-export="scenario" data-scenario-id="${escapeHtml(item.scenarioId)}">Excel</button>
                    <a class="tcf-btn tcf-btn--ghost" href="/oc/cap-new/wizard.html?id=${encodeURIComponent(item.scenarioId)}">상세 보기</a>
                </p>
            </article>
        `).join('');

        pendingEl.querySelectorAll('.btn-approve').forEach(btn => {
            btn.addEventListener('click', async () => {
                const card = btn.closest('.oc-capnew-card');
                const id = card.dataset.id;
                try {
                    await window.ocCapNewApi.approve(id, {
                        approver: card.querySelector('.f_approver').value,
                        reviewer: card.querySelector('.f_reviewer').value,
                        approvalNote: card.querySelector('.f_note').value,
                        criticalOverride: card.querySelector('.f_override').checked
                    });
                    showMsg('시나리오가 확정되었습니다.', true);
                    await reload();
                } catch (e) {
                    showMsg(e.message, false);
                }
            });
        });

        if (focusId) {
            const focus = pendingEl.querySelector(`[data-id="${focusId}"]`);
            if (focus) focus.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
    }

    function renderApproved(items) {
        const approved = items.filter(i => i.status === 'APPROVED');
        if (!approved.length) {
            approvedEl.innerHTML = '<p>승인된 기준안이 없습니다.</p>';
            return;
        }
        approvedEl.innerHTML = approved.map(item => `
            <article class="oc-capnew-card">
                <h3>${escapeHtml(item.scenarioName)} <span class="oc-capnew-status oc-capnew-status--approved">APPROVED</span></h3>
                <p>${escapeHtml(item.projectName || '')} · ${escapeHtml(item.targetEnv || '')} · ${escapeHtml(item.versionNo || '')}</p>
                <p>갱신 ${escapeHtml(item.updatedAt || '-')}</p>
                <p style="margin-top:0.5rem;">
                    <button type="button" class="tcf-btn tcf-btn--ghost btn-clone" data-id="${escapeHtml(item.scenarioId)}">새 버전 복제</button>
                    <button type="button" class="tcf-btn tcf-btn--ghost btn-revoke" data-id="${escapeHtml(item.scenarioId)}">확정 취소</button>
                    <button type="button" class="tcf-btn tcf-btn--ghost" data-capnew-env-handoff data-scenario-id="${escapeHtml(item.scenarioId)}">ENV 점검</button>
                    <button type="button" class="tcf-btn tcf-btn--ghost" data-capnew-export="scenario" data-scenario-id="${escapeHtml(item.scenarioId)}">Excel</button>
                    <a class="tcf-btn tcf-btn--ghost" href="/oc/cap-new/wizard.html?id=${encodeURIComponent(item.scenarioId)}">상세 보기</a>
                </p>
            </article>
        `).join('');

        approvedEl.querySelectorAll('.btn-clone').forEach(btn => {
            btn.addEventListener('click', async () => {
                try {
                    const cloned = await window.ocCapNewApi.cloneVersion(btn.dataset.id);
                    showMsg('새 버전이 생성되었습니다: ' + cloned.scenarioId, true);
                    location.href = '/oc/cap-new/wizard.html?id=' + encodeURIComponent(cloned.scenarioId);
                } catch (e) {
                    showMsg(e.message, false);
                }
            });
        });

        approvedEl.querySelectorAll('.btn-revoke').forEach(btn => {
            btn.addEventListener('click', async () => {
                const note = prompt('확정 취소 사유를 입력하세요.');
                if (note === null) return;
                try {
                    await window.ocCapNewApi.revoke(btn.dataset.id, {
                        revoker: 'reviewer',
                        revokeNote: note
                    });
                    showMsg('확정이 취소되었습니다.', true);
                    await reload();
                } catch (e) {
                    showMsg(e.message, false);
                }
            });
        });
    }

    function renderHistory(rows) {
        if (!rows || !rows.length) {
            historyBody.innerHTML = '<tr><td colspan="8">확정 이력이 없습니다.</td></tr>';
            return;
        }
        historyBody.innerHTML = rows.map(r => `
            <tr>
                <td>${escapeHtml(r.createdAt)}</td>
                <td>${escapeHtml(r.action)}</td>
                <td><a href="/oc/cap-new/wizard.html?id=${encodeURIComponent(r.scenarioId)}">${escapeHtml(r.scenarioName)}</a></td>
                <td>${escapeHtml(r.versionNo)}</td>
                <td>${escapeHtml(r.overallJudgment)}</td>
                <td>${escapeHtml(r.approver)}</td>
                <td>${escapeHtml(r.reviewer)}</td>
                <td>${escapeHtml(r.approvalNote)}</td>
            </tr>
        `).join('');
    }

    async function reload() {
        const [scenarios, history] = await Promise.all([
            window.ocCapNewApi.listScenarios(),
            window.ocCapNewApi.listApprovals()
        ]);
        renderPending(scenarios);
        renderApproved(scenarios);
        renderHistory(history);
    }

    async function init() {
        try {
            await reload();
        } catch (e) {
            showMsg(e.message, false);
        }
    }

    init();
})();
