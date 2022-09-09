package com.luban.process.sub;

import cn.hutool.core.util.IdUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.activiti.bpmn.model.Activity;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.EndEvent;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.bpmn.model.SubProcess;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class SubMain {

    public static void main(String[] args) {
        String processKey = "sub1", resource = "sub-process/sub1.bpmn20.xml", tenantId = "002", processName = "sub1子流程",processInstanceId = "";
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
        variables.put("parentAssignee1","张三");
        variables.put("parentAssignee2","李四");
        variables.put("subAssignee1","王五");
        variables.put("subAssignee2","赵六");


        ProcessInstance processInstance = null;
        if (StringUtils.isNotBlank(processInstanceId)) {
            processInstance = engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        }

        if (processInstance == null) {
            processInstance = engine.getRuntimeService()
                .startProcessInstanceById(processDefinition.getId(), variables);
        }

        AtomicReference<String> lastId = new AtomicReference<>();
        List<Task> tasks = engine.getTaskService().createTaskQuery()
            .processInstanceId(processInstance.getProcessInstanceId()).taskAssignee("张三").list();
        tasks.forEach(task -> {
            log.info("张三处理任务:{}",task);
            engine.getTaskService().complete(task.getId());
            lastId.set(task.getId());
        });

        tasks = engine.getTaskService().createTaskQuery()
            .processInstanceId(processInstance.getProcessInstanceId()).taskAssignee("王五").list();
        tasks.forEach(task -> {
            log.info("王五处理任务:{}",task);
            back(task,lastId.get(),engine);
//            engine.getTaskService().complete(task.getId());
        });

        tasks = engine.getTaskService().createTaskQuery()
            .processInstanceId(processInstance.getProcessInstanceId()).taskAssignee("赵六").list();
        tasks.forEach(task -> {
            log.info("赵六处理任务:{}",task);
//            back(task,lastId.get(),engine);
//            engine.getTaskService().complete(task.getId());
        });

        tasks = engine.getTaskService().createTaskQuery()
            .processInstanceId(processInstance.getProcessInstanceId()).taskAssignee("张三").list();
        tasks.forEach(task -> {
            log.info("张三再次处理任务:{}",task);
            engine.getTaskService().complete(task.getId());
            lastId.set(task.getId());
        });

        tasks = engine.getTaskService().createTaskQuery()
            .processInstanceId(processInstance.getProcessInstanceId()).taskAssignee("王五").list();
        tasks.forEach(task -> {
            log.info("王五再次处理任务:{}",task);
            engine.getTaskService().complete(task.getId());
            lastId.set(task.getId());
        });

        tasks = engine.getTaskService().createTaskQuery()
            .processInstanceId(processInstance.getProcessInstanceId()).taskAssignee("赵六").list();
        tasks.forEach(task -> {
            log.info("赵六再次处理任务:{}",task);
            engine.getTaskService().complete(task.getId());
        });

        tasks = engine.getTaskService().createTaskQuery()
            .processInstanceId(processInstance.getProcessInstanceId()).taskAssignee("李四").list();
        tasks.forEach(task -> {
            log.info("李四再次处理任务:{}",task);
            engine.getTaskService().complete(task.getId());
        });
    }

    private static void back(Task task, String lastId, ProcessEngine engine) {
        List<HistoricActivityInstance> historicActivityInstances = engine.getHistoryService()
            .createHistoricActivityInstanceQuery()
            .processInstanceId(task.getProcessInstanceId())
            .orderByHistoricActivityInstanceStartTime().desc().list();
        if (historicActivityInstances == null || historicActivityInstances.size() < 2) {
            return;
        }
        HistoricActivityInstance lastInstance = historicActivityInstances.stream()
            .filter(historicActivityInstance -> lastId.equals(historicActivityInstance.getTaskId()))
            .findFirst().orElseThrow(() -> new RuntimeException("流程节点不存在!"));
        BpmnModel bpmnModel = engine.getRepositoryService()
            .getBpmnModel(task.getProcessDefinitionId());
        FlowElement lastNode = bpmnModel.getMainProcess()
            .getFlowElement(lastInstance.getActivityId());
        //（当前节点）11（上一个节点） 两个都是子节点
        //
        byte flag = 0;
        if (lastNode == null && (lastNode = bpmnModel.getMainProcess().getFlowElement(lastInstance.getActivityId(),true)) != null) {
            log.info("要回退的节点为子流程节点:{}-{}",lastNode.getId(),lastNode.getName());
            flag |= 1;
        }
        FlowNode currentNode = (FlowNode)bpmnModel.getMainProcess().getFlowElement(task.getTaskDefinitionKey());
        if (currentNode == null && (currentNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(task.getTaskDefinitionKey(),true)) != null) {
            log.info("回退节点为子节点:{}-{}",currentNode.getId(),currentNode.getName());
            flag |= 2;
        }
        if (flag == 3 || flag == 0) { //全为子流程
            List<SequenceFlow> originalSequenceFlowList = new ArrayList<>(currentNode.getOutgoingFlows());
            currentNode.getOutgoingFlows().clear();
            SequenceFlow newSequenceFlow = new SequenceFlow();
            newSequenceFlow.setId("newSequenceFlowId:"+ IdUtil.getSnowflakeNextIdStr());
            newSequenceFlow.setSourceFlowElement(currentNode);
            newSequenceFlow.setTargetFlowElement(lastNode);
            currentNode.setOutgoingFlows(Collections.singletonList(newSequenceFlow));
            if (currentNode instanceof Activity && ((Activity) currentNode).hasMultiInstanceLoopCharacteristics()) {
                List<Task> taskList = engine.getTaskService().createTaskQuery()
                    .taskDefinitionKey(task.getTaskDefinitionKey()).processInstanceId(task.getProcessInstanceId()).list();
                taskList.forEach(submitTask -> {
                    engine.getTaskService().complete(submitTask.getId());
                });
            } else {
                engine.getTaskService().complete(task.getId());
            }
            if (!((lastNode instanceof Activity) && ((Activity) lastNode).hasMultiInstanceLoopCharacteristics())) {
                Task nextTask = engine.getTaskService().createTaskQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
                engine.getTaskService().setAssignee(nextTask.getId(), lastInstance.getAssignee());
            }
            currentNode.setOutgoingFlows(originalSequenceFlowList);
        } else if (flag == 1) {
            //TODO 将主节点的流转方向指向子节点的入口节点
            SubProcess subProcess = lastNode.getSubProcess();
            List<SequenceFlow> originalSequenceFlowList = new ArrayList<>(currentNode.getOutgoingFlows());
            currentNode.getOutgoingFlows().clear();
            SequenceFlow newSequenceFlow = new SequenceFlow();
            newSequenceFlow.setId("newSequenceFlowId:"+ IdUtil.getSnowflakeNextIdStr());
            newSequenceFlow.setSourceFlowElement(currentNode);
            newSequenceFlow.setTargetFlowElement(subProcess);
            currentNode.setOutgoingFlows(Collections.singletonList(newSequenceFlow));

            //TODO 将子节点的开始节点执行要跳转的节点
            FlowNode currentSubNode = (FlowNode) subProcess.getFlowElements().stream()
                .filter(flowElement -> flowElement instanceof StartEvent).findFirst().get();
            List<SequenceFlow> originalSubSequenceFlowList = new ArrayList<>(currentSubNode.getOutgoingFlows());
            currentSubNode.getOutgoingFlows().clear();
            SequenceFlow newSubSequenceFlow = new SequenceFlow();
            newSubSequenceFlow.setId("newSequenceFlowId:"+ IdUtil.getSnowflakeNextIdStr());
            newSubSequenceFlow.setSourceFlowElement(currentSubNode);
            newSubSequenceFlow.setTargetFlowElement(lastNode);
            currentSubNode.setOutgoingFlows(Collections.singletonList(newSubSequenceFlow));

            //TODO 提交任务
            if (currentNode instanceof Activity && ((Activity) currentNode).hasMultiInstanceLoopCharacteristics()) {
                List<Task> taskList = engine.getTaskService().createTaskQuery()
                    .taskDefinitionKey(task.getTaskDefinitionKey()).processInstanceId(task.getProcessInstanceId()).list();
                taskList.forEach(submitTask -> {
                    engine.getTaskService().complete(submitTask.getId());
                });
            } else {
                engine.getTaskService().complete(task.getId());
            }
            //TODO 设置流转到的当前节点的任务人
            if (!((lastNode instanceof Activity) && ((Activity) lastNode).hasMultiInstanceLoopCharacteristics())) {
                Task nextTask = engine.getTaskService().createTaskQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
                engine.getTaskService().setAssignee(nextTask.getId(), lastInstance.getAssignee());
            }
            //TODO 将子节点的顺序还原
            currentSubNode.setOutgoingFlows(originalSubSequenceFlowList);
            //TODO 将主节点的顺序还原
            currentNode.setOutgoingFlows(originalSequenceFlowList);
        } else if (flag == 2) {
            //TODO 将子流程结束节点指向主节点
            SubProcess subProcess = currentNode.getSubProcess();
            List<SequenceFlow> originalSequenceFlowList = new ArrayList<>(subProcess.getOutgoingFlows());
            subProcess.getOutgoingFlows().clear();
            SequenceFlow newSequenceFlow = new SequenceFlow();
            newSequenceFlow.setId("newSequenceFlowId:"+ IdUtil.getSnowflakeNextIdStr());
            newSequenceFlow.setSourceFlowElement(subProcess);
            newSequenceFlow.setTargetFlowElement(lastNode);
            subProcess.setOutgoingFlows(Collections.singletonList(newSequenceFlow));
            //TODO 将当前子节点指向结束节点
            FlowNode subEndNode = (FlowNode)subProcess.getFlowElements().stream()
                .filter(flowElement -> EndEvent.class.isInstance(flowElement)).findFirst().get();
            List<SequenceFlow> originalSubSequenceFlowList = new ArrayList<>(currentNode.getOutgoingFlows());
            currentNode.getOutgoingFlows().clear();
            SequenceFlow newSubSequenceFlow = new SequenceFlow();
            newSubSequenceFlow.setId("newSequenceFlowId:"+ IdUtil.getSnowflakeNextIdStr());
            newSubSequenceFlow.setSourceFlowElement(currentNode);
            newSubSequenceFlow.setTargetFlowElement(subEndNode);
            currentNode.setOutgoingFlows(Collections.singletonList(newSubSequenceFlow));

            //TODO 提交任务
            if (currentNode instanceof Activity && ((Activity) currentNode).hasMultiInstanceLoopCharacteristics()) {
                List<Task> taskList = engine.getTaskService().createTaskQuery()
                    .taskDefinitionKey(task.getTaskDefinitionKey()).processInstanceId(task.getProcessInstanceId()).list();
                taskList.forEach(submitTask -> {
                    engine.getTaskService().complete(submitTask.getId());
                });
            } else {
                engine.getTaskService().complete(task.getId());
            }
            //TODO 设置流转到的当前节点的任务人
            if (!((lastNode instanceof Activity) && ((Activity) lastNode).hasMultiInstanceLoopCharacteristics())) {
                Task nextTask = engine.getTaskService().createTaskQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
                engine.getTaskService().setAssignee(nextTask.getId(), lastInstance.getAssignee());
            }

            //TODO 将当前子节点的流向恢复正常
            currentNode.setOutgoingFlows(originalSubSequenceFlowList);
            //TODO 将子节点流程结束流向恢复正常
            subProcess.setOutgoingFlows(originalSequenceFlowList);

        }

//        List<SequenceFlow> originalSequenceFlowList = new ArrayList<>(currentNode.getOutgoingFlows());
//        currentNode.getOutgoingFlows().clear();
//        SequenceFlow newSequenceFlow = new SequenceFlow();
//        newSequenceFlow.setId("newSequenceFlowId:"+ IdUtil.getSnowflakeNextIdStr());
//        newSequenceFlow.setSourceFlowElement(currentNode);
//        newSequenceFlow.setTargetFlowElement(lastNode);
//        currentNode.setOutgoingFlows(Collections.singletonList(newSequenceFlow));
//        if (currentNode instanceof Activity && ((Activity) currentNode).hasMultiInstanceLoopCharacteristics()) {
//            List<Task> taskList = engine.getTaskService().createTaskQuery()
//                .taskDefinitionKey(task.getTaskDefinitionKey()).processInstanceId(task.getProcessInstanceId()).list();
//            taskList.forEach(submitTask -> {
//                engine.getTaskService().complete(submitTask.getId());
//            });
//        } else {
//            engine.getTaskService().complete(task.getId());
//        }
//        if (!((lastNode instanceof Activity) && ((Activity) lastNode).hasMultiInstanceLoopCharacteristics())) {
//            Task nextTask = engine.getTaskService().createTaskQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
//            engine.getTaskService().setAssignee(nextTask.getId(), lastInstance.getAssignee());
//        }
//        currentNode.setOutgoingFlows(originalSequenceFlowList);
    }

}
