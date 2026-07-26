package com.nh.nsight.marketing.om.support;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.marketing.om.persistence.dao.OmOperationDao;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OmDashboardSnapshotTx {
    private final OmOperationDao dao;
    private final OmChangeRecorder recorder;

    public OmDashboardSnapshotTx(OmOperationDao dao, OmChangeRecorder recorder) {
        this.dao = dao;
        this.recorder = recorder;
    }

    @Transactional(timeout = 30)
    public SnapshotPurgeResult purgeAll() {
        return new SnapshotPurgeResult(
                dao.deleteAllApStatus(),
                dao.deleteAllDbStatus(),
                dao.deleteAllSessionStatus(),
                dao.deleteAllDeployStatus());
    }

    @Transactional(timeout = 30)
    public void recordResetAudit(TransactionContext context, SnapshotPurgeResult purge, String reason) {
        recorder.recordAdminAudit(context, "DASHBOARD_RESET", "대시보드 스냅샷 DB 초기화",
                reason, "SUCCESS");
        recorder.recordAuthHistory(context, "DASHBOARD", "OM_*_STATUS", "all",
                "deleted:ap=" + purge.deletedAp() + ",db=" + purge.deletedDb()
                        + ",session=" + purge.deletedSession() + ",deploy=" + purge.deletedDeploy(),
                reason);
    }

    public record SnapshotPurgeResult(int deletedAp, int deletedDb, int deletedSession, int deletedDeploy) {
    }
}
