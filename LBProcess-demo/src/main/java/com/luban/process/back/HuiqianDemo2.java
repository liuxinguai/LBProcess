package com.luban.process.back;

import cn.hutool.core.util.IdUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.activiti.bpmn.model.Activity;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;

@Slf4j
public class HuiqianDemo2 {

    public static void main(String[] args) {
        String processKey = "huiqian4", resource = "back/huiqian4.bpmn", tenantId = "002", processName = "会签4.0";
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
        variables.put("assignee1","张三");
        variables.put("assignee2List", Arrays.asList("李四","王五"));
        variables.put("assignee3", "赵六");

        ProcessInstance instance = engine.getRuntimeService()
            .startProcessInstanceById(processDefinition.getId(), variables);

        List<Task> tasks = engine.getTaskService().createTaskQuery().processInstanceId(instance.getProcessInstanceId()).taskAssignee("张三").list();
        tasks.forEach(task -> {
            log.info("张三处理任务:{}",task.getName());
            engine.getTaskService().complete(task.getId());
        });

        System.out.println("会签------------------------------------");
        //TODO 处理会签
        tasks = engine.getTaskService().createTaskQuery().processInstanceId(instance.getProcessInstanceId()).taskAssignee("李四").list();
        tasks.forEach(task -> {
            log.info("李四处理任务:{}",task.getName());
            engine.getTaskService().complete(task.getId());
        });
        AtomicReference<String> lastTaskId = new AtomicReference<>();
        tasks = engine.getTaskService().createTaskQuery().processInstanceId(instance.getProcessInstanceId()).taskAssignee("王五").list();
        tasks.forEach(task -> {
            log.info("王五处理任务:{}",task.getName());
            engine.getTaskService().complete(task.getId());
            lastTaskId.set(task.getId());
        });

        //TODO 归档
        tasks = engine.getTaskService().createTaskQuery().processInstanceId(instance.getProcessInstanceId()).taskAssignee("赵六").list();
        tasks.forEach(task -> {
            log.info("赵六处理任务:{}",task.getName());
            back(task,lastTaskId.get(),engine);
//            engine.getTaskService().complete(task.getId());
        });
        System.out.println("结束-------------------------");

        //TODO 处理会签
        tasks = engine.getTaskService().createTaskQuery().processInstanceId(instance.getProcessInstanceId()).taskAssignee("李四").list();
        tasks.forEach(task -> {
            log.info("李四处理任务:{}",task.getName());
            engine.getTaskService().complete(task.getId());
        });
        tasks = engine.getTaskService().createTaskQuery().processInstanceId(instance.getProcessInstanceId()).taskAssignee("王五").list();
        tasks.forEach(task -> {
            log.info("王五处理任务:{}",task.getName());
            engine.getTaskService().complete(task.getId());
        });
        //TODO 归档
        tasks = engine.getTaskService().createTaskQuery().processInstanceId(instance.getProcessInstanceId()).taskAssignee("赵六").list();
        tasks.forEach(task -> {
            log.info("赵六处理任务:{}",task.getName());
//            back(task,lastTaskId.get(),engine);
            engine.getTaskService().complete(task.getId());
        });
    }

    public static void back(Task task, String lastTaskId, ProcessEngine engine) {
        List<HistoricActivityInstance> historicActivityInstances = engine.getHistoryService()
            .createHistoricActivityInstanceQuery().processInstanceId(task.getProcessInstanceId())
            .orderByHistoricActivityInstanceStartTime().desc().list();
        if (historicActivityInstances == null || historicActivityInstances.size() < 2) {
            return;
        }
        HistoricActivityInstance lastInstance = historicActivityInstances.stream()
            .filter(
                historicActivityInstance -> historicActivityInstance.getTaskId().equals(lastTaskId))
            .findFirst().orElseThrow(() -> new RuntimeException("要回退的任务不存在!"));
        BpmnModel bpmnModel = engine.getRepositoryService()
            .getBpmnModel(task.getProcessDefinitionId());
        FlowNode lastNode = (FlowNode)bpmnModel.getMainProcess()
            .getFlowElement(lastInstance.getActivityId());
        FlowNode currentNode = (FlowNode)bpmnModel.getMainProcess().getFlowElement(task.getTaskDefinitionKey());
        List<SequenceFlow> originalSequenceFlowList = new ArrayList<>();
        originalSequenceFlowList.addAll(currentNode.getOutgoingFlows());
        currentNode.getOutgoingFlows().clear();
        SequenceFlow newSequenceFlow = new SequenceFlow();
        newSequenceFlow.setId("newSequenceFlow:"+ IdUtil.getSnowflakeNextIdStr());
        newSequenceFlow.setSourceFlowElement(currentNode);
        newSequenceFlow.setTargetFlowElement(lastNode);
        List<SequenceFlow> newSequenceFlowList = new ArrayList<>();
        newSequenceFlowList.add(newSequenceFlow);
        currentNode.setOutgoingFlows(newSequenceFlowList);
        if (currentNode instanceof Activity && ((Activity) currentNode).hasMultiInstanceLoopCharacteristics()) {
            List<Task> taskList = engine.getTaskService().createTaskQuery()
                .taskDefinitionKey(task.getTaskDefinitionKey()).processInstanceId(task.getId())
                .list();
            taskList.forEach(submitTask -> engine.getTaskService().complete(submitTask.getId()));
        } else {
            engine.getTaskService().complete(task.getId());
        }
        if (!((lastNode instanceof Activity) && ((Activity) lastNode).hasMultiInstanceLoopCharacteristics())) {
            Task nextTask = engine.getTaskService().createTaskQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
            engine.getTaskService().setAssignee(nextTask.getId(), lastInstance.getAssignee());
        }
        currentNode.setOutgoingFlows(originalSequenceFlowList);
    }

}
