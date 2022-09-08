package com.luban.process;


import java.util.List;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.task.Task;

public class Test3 {

    public static void main(String[] args) {
        ProcessEngineConfiguration cfg = new StandaloneProcessEngineConfiguration()
            .setJdbcUrl("jdbc:h2:mem:activiti;DB_CLOSE_DELAY=1000")
            .setJdbcUsername("sa")
            .setJdbcPassword("")
            .setJdbcDriver("org.h2.Driver")
            .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        ProcessEngine processEngine = cfg.buildProcessEngine();

        Deployment deployment = processEngine.getRepositoryService().createDeployment()
            .addClasspathResource("my-process.bpmn20.xml").deploy();
        ProcessDefinition processDefinition = processEngine.getRepositoryService()
            .createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
        processEngine.getRuntimeService().startProcessInstanceById(processDefinition.getId());

        List<Task> tasks = processEngine.getTaskService().createTaskQuery().list();
        tasks.forEach(task -> {
            System.out.println(task.getId() + "-----" + task.getName());
            processEngine.getTaskService().complete(task.getId());
        });
        processEngine.getTaskService().createTaskQuery().list().forEach(task -> {
            System.out.println(task.getId() + "-----" + task.getName());
            processEngine.getTaskService().complete(task.getId());
        });
    }

}
