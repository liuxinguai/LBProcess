package com.luban.process.back;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.activiti.engine.impl.util.CollectionUtil;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;

@Slf4j
public class Demo {

    public static void main(String[] args) {
        String processKey = "back2", resource = "back/back2.bpmn20.xml", tenantId = "002", processName = "回退2.0";
        ProcessEngineConfiguration cfg = new StandaloneProcessEngineConfiguration()
            .setJdbcUrl("jdbc:mysql://192.168.3.210:3306/process_test?useUnicode=true&characterEncoding=UTF-8&useSSL=false")
            .setJdbcUsername("ps_r")
            .setJdbcPassword("ps_r@123")
            .setJdbcDriver("com.mysql.cj.jdbc.Driver")
            .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        ProcessEngine engine = cfg.buildProcessEngine();

        Deployment deployment = engine.getRepositoryService().createDeploymentQuery()
            .deploymentKey(processKey).deploymentTenantId(tenantId).singleResult();
        if (deployment == null) {
            deployment = engine.getRepositoryService().createDeployment()
                .key(processKey).name(processName).tenantId(tenantId).addClasspathResource(resource).deploy();
        }

        ProcessDefinition processDefinition = engine.getRepositoryService()
            .createProcessDefinitionQuery().deploymentId(deployment.getId()).active()
            .singleResult();

        if (processDefinition == null) {
            log.warn("{}-{} 流程未发布",processKey,processName);
            return ;
        }
        Map<String,Object> variables = new HashMap<>();
        variables.put("assinee1","张三");
        variables.put("assinee2","李四");
        variables.put("assinee3","王五");
        engine.getRuntimeService()
            .startProcessInstanceById(processDefinition.getId(), variables);

        List<Task> tasks = engine.getTaskService().createTaskQuery().taskAssignee("张三").list();
        tasks.forEach(task -> {
            log.info("张三处理任务:{}",task.getName());
            engine.getTaskService().complete(task.getId());
        });
        tasks = engine.getTaskService().createTaskQuery().taskAssignee("李四").list();
        tasks.forEach(task -> {
            log.info("李四处理任务:{}",task.getName());
            engine.getTaskService().complete(task.getId());
//            back(task,engine);
        });
//        String rollbackId = "";
//        engine.getHistoryService().createHistoricActivityInstanceQuery()
        tasks = engine.getTaskService().createTaskQuery().taskAssignee("王五").list();
        tasks.forEach(task -> {
            log.info("王五处理任务:{}",task.getName());
            engine.getTaskService().complete(task.getId());
//            back(task,engine);
        });
    }

    protected static void back(Task task, ProcessEngine processEngine) {
        List<HistoricTaskInstance> hisTaskList = processEngine.getHistoryService()
            .createHistoricTaskInstanceQuery()
            .processInstanceId(task.getProcessInstanceId())
            .orderByTaskCreateTime()
            .desc()
            .list();
        List<HistoricActivityInstance> hisActivityList = processEngine.getHistoryService()
            .createHistoricActivityInstanceQuery()
            .processInstanceId(task.getProcessInstanceId()).list();
        if (CollectionUtil.isEmpty(hisTaskList) || hisTaskList.size() < 2) {
            return;
        }

        //  当前任务
        HistoricTaskInstance currentTask = hisTaskList.get(0);
        //  前一个任务
        HistoricTaskInstance lastTask = hisTaskList.get(1);
        //  当前活动
        HistoricActivityInstance currentActivity = hisActivityList.stream().filter(e -> currentTask.getId().equals(e.getTaskId())).collect(Collectors.toList()).get(0);
        //  前一个活动
        HistoricActivityInstance lastActivity = hisActivityList.stream().filter(e -> lastTask.getId().equals(e.getTaskId())).collect(
            Collectors.toList()).get(0);

        BpmnModel bpmnModel = processEngine.getRepositoryService().getBpmnModel(task.getProcessDefinitionId());

        //  获取前一个活动节点
        FlowNode lastFlowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(lastActivity.getActivityId());
        //  获取当前活动节点
        FlowNode currentFlowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(currentActivity.getActivityId());

        //  临时保存当前活动的原始方向
        List<SequenceFlow> originalSequenceFlowList = new ArrayList<>();
        originalSequenceFlowList.addAll(currentFlowNode.getOutgoingFlows());
        //  清理活动方向
        currentFlowNode.getOutgoingFlows().clear();

        //  建立新方向
        SequenceFlow newSequenceFlow = new SequenceFlow();
        newSequenceFlow.setId("n"+UUID.randomUUID().toString());
        newSequenceFlow.setSourceFlowElement(currentFlowNode);
        newSequenceFlow.setTargetFlowElement(lastFlowNode);
        List<SequenceFlow> newSequenceFlowList = new ArrayList<>();
        newSequenceFlowList.add(newSequenceFlow);
        //  当前节点指向新的方向
        currentFlowNode.setOutgoingFlows(newSequenceFlowList);

        //  完成当前任务
        processEngine.getTaskService().complete(task.getId());

        //  重新查询当前任务
        Task nextTask = processEngine.getTaskService().createTaskQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
        if (null != nextTask) {
            processEngine.getTaskService().setAssignee(nextTask.getId(), lastTask.getAssignee());
        }
        //  恢复原始方向
        currentFlowNode.setOutgoingFlows(originalSequenceFlowList);
    }

}
