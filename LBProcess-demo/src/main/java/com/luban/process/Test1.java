package com.luban.process;

import org.activiti.engine.FormService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.TaskService;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.task.TaskQuery;

public class Test1 {

    public static void main(String[] args) {
        ProcessEngineConfiguration engineConfiguration = new StandaloneProcessEngineConfiguration()
            .setJdbcUrl("jdbc:h2:mem:activiti;DB_CLOSE_DELAY=1000")
            .setJdbcUsername("sa")
            .setJdbcPassword("")
            .setJdbcDriver("org.h2.Driver")
            .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        ProcessEngine processEngine = engineConfiguration.buildProcessEngine();
        Deployment deployment = processEngine.getRepositoryService()
            .createDeployment().addClasspathResource("test1.bpmn20.xml")
            .deploy();
        ProcessDefinition processDefinition = processEngine.getRepositoryService()
            .createProcessDefinitionQuery()
            .deploymentId(deployment.getId())
            .singleResult();
        processEngine.getRuntimeService().startProcessInstanceById(processDefinition.getId());
        TaskService taskService = processEngine.getTaskService();
        TaskQuery taskQuery = taskService.createTaskQuery().taskCandidateUser("liuxg");
        taskQuery.list().forEach(task -> {
            System.out.println(task.getName());
//            TaskFormData formData = formService.getTaskFormData(task.getId());
//            formData.getFormProperties().forEach(formProperty -> {
//                System.out.println(formProperty);
//            });
            taskService.complete(task.getId());
        });
        HistoricTaskInstance result = processEngine.getHistoryService()
            .createHistoricTaskInstanceQuery()
            .processInstanceId(processDefinition.getId())
            .singleResult();
        System.out.println(result.getName());
    }

}
