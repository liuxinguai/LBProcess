package com.luban.process.spi;

import cn.hutool.core.util.IdUtil;
import com.luban.process.ProcessEngineAware;
import com.luban.process.ProcessService;
import com.luban.process.enums.ResponseCode;
import com.luban.process.exception.ProcessServiceException;
import com.luban.process.model.ClaimBody;
import com.luban.process.model.CommitBody;
import com.luban.process.model.DispatchBody;
import com.luban.process.model.Request;
import com.luban.process.model.RollbackBody;
import com.luban.process.model.UnClaimBody;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.activiti.bpmn.model.Activity;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.task.Task;
import org.apache.commons.lang3.StringUtils;

public class ProcessServiceImpl implements ProcessService, ProcessEngineAware {

    private ProcessEngine processEngine;

    @Override
    public void rollback(Request<RollbackBody> rollbackRequest) {
        RollbackBody data = rollbackRequest.getData();
        String currentTaskId = data.getCurrentTaskId();
        String rollbackTaskId = data.getRollbackTaskId();
        Task task = check(currentTaskId);
        List<HistoricActivityInstance> historicActivityInstances = processEngine.getHistoryService()
            .createHistoricActivityInstanceQuery().processInstanceId(task.getProcessInstanceId())
            .orderByHistoricActivityInstanceStartTime().desc().list();
        if (historicActivityInstances == null || historicActivityInstances.size() < 2) {
            throw new ProcessServiceException(8,"发起节点不允许回退");
        }
        HistoricActivityInstance lastInstance = historicActivityInstances.stream()
            .filter(
                historicActivityInstance -> historicActivityInstance.getTaskId().equals(rollbackTaskId))
            .findAny().orElseThrow(() -> new ProcessServiceException(8,"要回退的任务不存在!"));
        BpmnModel bpmnModel = processEngine.getRepositoryService()
            .getBpmnModel(task.getProcessDefinitionId());
        FlowNode lastNode = (FlowNode)bpmnModel.getMainProcess()
            .getFlowElement(lastInstance.getActivityId());
        FlowNode currentNode = (FlowNode)bpmnModel.getMainProcess().getFlowElement(task.getTaskDefinitionKey());
        List<SequenceFlow> originalSequenceFlowList = new ArrayList<>(currentNode.getOutgoingFlows());
        currentNode.getOutgoingFlows().clear();
        SequenceFlow newSequenceFlow = new SequenceFlow();
        newSequenceFlow.setId("newSequenceFlow:"+ IdUtil.getSnowflakeNextIdStr());
        newSequenceFlow.setSourceFlowElement(currentNode);
        newSequenceFlow.setTargetFlowElement(lastNode);
        currentNode.setOutgoingFlows(Collections.singletonList(newSequenceFlow));
        if (currentNode instanceof Activity && ((Activity) currentNode).hasMultiInstanceLoopCharacteristics()) {
            List<Task> taskList = processEngine.getTaskService().createTaskQuery()
                .taskDefinitionKey(task.getTaskDefinitionKey()).processInstanceId(task.getProcessInstanceId()).list();
            taskList.forEach(submitTask -> processEngine.getTaskService().complete(submitTask.getId()));
        } else {
            processEngine.getTaskService().complete(task.getId());
        }
        if (!((lastNode instanceof Activity) && ((Activity) lastNode).hasMultiInstanceLoopCharacteristics())) {
            Task nextTask = processEngine.getTaskService().createTaskQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
            processEngine.getTaskService().setAssignee(nextTask.getId(), lastInstance.getAssignee());
        }
        currentNode.setOutgoingFlows(originalSequenceFlowList);
    }

    @Override
    public void commit(Request<CommitBody> commitRequest) {
        Task task = check(commitRequest.getData().getTaskId());
        if (!commitRequest.getData().getCommitUserId().equals(task.getAssignee())) {
            throw new ProcessServiceException(8,"不能提交他人任务");
        }
        processEngine.getTaskService().complete(task.getId(),commitRequest.getData().getFormData());
    }

    @Override
    public void claim(Request<ClaimBody> claimRequest) {
        Task task = check(claimRequest.getData().getTaskId());
        if (StringUtils.isNotBlank(task.getAssignee())) {
            throw new ProcessServiceException(8,"任务已被别人领取");
        }
        processEngine.getTaskService().claim(claimRequest.getData().getTaskId(),claimRequest.getData().getUserId());
    }

    @Override
    public void unClaim(Request<UnClaimBody> unClaimRequest) {
        check(unClaimRequest.getData().getTaskId());
        processEngine.getTaskService().unclaim(unClaimRequest.getData().getTaskId());
    }

    @Override
    public void dispatch(Request<DispatchBody> dispatchRequest) {
        Task task = check(dispatchRequest.getData().getTaskId());
        processEngine.getTaskService().setAssignee(task.getId(), dispatchRequest.getData().getDispatchUserId());
    }

    @Override
    public void setProcessEngine(ProcessEngine processEngine) {
        this.processEngine = processEngine;
    }

    private Task check(String taskId) {
        Task task = processEngine.getTaskService().createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new ProcessServiceException(ResponseCode.NOT_FOUND.getCode(),ResponseCode.NOT_FOUND.getMessage());
        }
        return task;
    }

}
