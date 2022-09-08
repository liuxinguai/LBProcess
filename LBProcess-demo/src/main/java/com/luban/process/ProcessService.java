package com.luban.process;

import com.luban.process.model.ClaimBody;
import com.luban.process.model.CommitBody;
import com.luban.process.model.DispatchBody;
import com.luban.process.model.Request;
import com.luban.process.model.RollbackBody;
import com.luban.process.model.UnClaimBody;

/**
 * 流程实例服务
 * @author xinguai.liu
 */
public interface ProcessService {

    void rollback(Request<RollbackBody> rollbackRequest);

    void commit(Request<CommitBody> commitRequest);

    void claim(Request<ClaimBody> claimRequest);

    void unClaim(Request<UnClaimBody> unClaimRequest);

    void dispatch(Request<DispatchBody> dispatchRequest);

}
