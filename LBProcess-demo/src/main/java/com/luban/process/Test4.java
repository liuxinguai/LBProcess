package com.luban.process;


import java.util.List;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.task.Task;

public class Test4 {

    public static void main(String[] args) {
        ProcessEngineConfiguration cfg = new StandaloneProcessEngineConfiguration()
            .setJdbcUrl("jdbc:h2:mem:activiti;DB_CLOSE_DELAY=1000")
            .setJdbcUsername("sa")
            .setJdbcPassword("")
            .setJdbcDriver("org.h2.Driver")
            .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        ProcessEngine processEngine = cfg.buildProcessEngine();

        Deployment deployment = processEngine.getRepositoryService().createDeployment()
            .addClasspathResource("my-process-usertask.bpmn20.xml").deploy();
        ProcessDefinition processDefinition = processEngine.getRepositoryService()
            .createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
        processEngine.getRuntimeService().startProcessInstanceById(processDefinition.getId());

        List<Task> tasks = processEngine.getTaskService().createTaskQuery().taskCandidateUser("user1").list();
        System.out.println(tasks.size());
        Task task = tasks.get(0);
        String taskId = task.getId();
        processEngine.getTaskService().setAssignee(taskId,"liuxg");
        tasks = processEngine.getTaskService().createTaskQuery().taskAssignee("liuxg").list();
        System.out.println(tasks.size());
//        processEngine.getTaskService().claim(taskId,"user1");
        tasks = processEngine.getTaskService().createTaskQuery().taskCandidateUser("user2").list();
        System.out.println(tasks.size());
        processEngine.getTaskService().unclaim(taskId);
        tasks = processEngine.getTaskService().createTaskQuery().taskCandidateGroup("group1").list();
        System.out.println(tasks.size());
    }

}
