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
public class SubCallMain {

    public static void main(String[] args) {
        String processKey = "sub2", resource = "sub-process/sub2.bpmn20.xml", tenantId = "002", processName = "sub2子流程",processInstanceId = "217501";
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
        variables.put("assignee2","李四");
        variables.put("callAssignee1","王五");
        variables.put("call","call");

        ProcessInstance processInstance = null;
        if (StringUtils.isNotBlank(processInstanceId)) {
            processInstance = engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        }

        if (processInstance == null) {
            processInstance = engine.getRuntimeService()
                .startProcessInstanceById(processDefinition.getId(), variables);
        }

        List<Task> tasks = engine.getTaskService().createTaskQuery()
            .processInstanceId(processInstance.getProcessInstanceId()).taskAssignee("张三").list();
        tasks.forEach(task -> {
            log.info("张三处理任务:{}",task);
            engine.getTaskService().complete(task.getId());
        });

        tasks = engine.getTaskService().createTaskQuery()
            .processInstanceId(processInstance.getProcessInstanceId()).taskAssignee("李四").list();
        tasks.forEach(task -> {
            log.info("李四处理任务:{}",task);
            engine.getRuntimeService().getVariables(task.getExecutionId()).forEach((key,value)->{
                log.info("最后流程的参数：{}-{}", key, value);
            });
            engine.getTaskService().complete(task.getId());
        });


    }

}
