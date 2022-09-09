package com.luban.process.sub;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class SubCallMain2 {

    public static void main(String[] args) {
        String processKey = "call", resource = "sub-process/call.bpmn20.xml", tenantId = "002", processName = "call子流程",processInstanceId = "217512";
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

//        Map<String,Object> variables = new HashMap<>();
//        variables.put("assignee1","张三");
//        variables.put("assignee2","李四");
//        variables.put("callAssignee1","王五");
//        variables.put("call","call");

        ProcessInstance processInstance = null;
        if (StringUtils.isNotBlank(processInstanceId)) {
            processInstance = engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        }

        if (processInstance == null) {
            log.info("重新发布流程");
            processInstance = engine.getRuntimeService()
                .startProcessInstanceById(processDefinition.getId());
        }

        List<Task> tasks = engine.getTaskService().createTaskQuery()
            .processInstanceId(processInstance.getProcessInstanceId()).taskAssignee("王五").list();
        tasks.forEach(task -> {
            log.info("王五处理子流程任务:{}",task);
            Map<String,Object> variable = new HashMap<>();
            variable.put("testcallSub","liuxg");
            engine.getTaskService().complete(task.getId(),variable,false);
        });
//
//        tasks = engine.getTaskService().createTaskQuery()
//            .processInstanceId(processInstance.getProcessInstanceId()).taskAssignee("李四").list();
//        tasks.forEach(task -> {
//            log.info("王五处理任务:{}",task);
//            engine.getTaskService().complete(task.getId());
//        });

    }

}
