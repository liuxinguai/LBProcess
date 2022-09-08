package com.luban.process;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.TaskService;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.activiti.engine.impl.form.DateFormType;
import org.activiti.engine.impl.form.LongFormType;
import org.activiti.engine.impl.form.StringFormType;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;

public class Test2 {

    private ProcessEngine processEngine;

    private ProcessInstance processInstance;

    public static void main(String[] args) {
        Test2 test2 = new Test2();
        test2.init();
        test2.deploy("second_approve.bpmn20.xml");
        System.out.println(test2.processInstance.isEnded());
        while (!test2.handle()) {}

        HistoricTaskInstanceQuery instanceQuery = test2.processEngine.getHistoryService()
            .createHistoricTaskInstanceQuery()
            .processInstanceId(test2.processInstance.getProcessInstanceId()).processFinished();
        instanceQuery.list().forEach(taskInstance -> {
            System.out.println(taskInstance.getName());
        });
    }

    protected void init() {
        ProcessEngineConfiguration cfg = new StandaloneProcessEngineConfiguration()
            .setJdbcUrl("jdbc:h2:mem:activiti;DB_CLOSE_DELAY=1000")
            .setJdbcUsername("sa")
            .setJdbcPassword("")
            .setJdbcDriver("org.h2.Driver")
            .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        processEngine = cfg.buildProcessEngine();
    }

    protected void deploy(String classpathResource) {
        Deployment deployment = processEngine.getRepositoryService().createDeployment()
            .addClasspathResource(classpathResource).deploy();
        ProcessDefinition processDefinition = processEngine.getRepositoryService()
            .createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
        this.processInstance = processEngine.getRuntimeService()
            .startProcessInstanceById(processDefinition.getId());
    }

    protected boolean handle() {
        TaskQuery query = processEngine.getTaskService().createTaskQuery()
            .processDefinitionId(processInstance.getProcessDefinitionId());
        List<Task> tasks = query.list();
        boolean result = tasks.isEmpty();
        tasks.forEach(task -> {
            if (!task.getId().equals("endEvent")) {
                TaskFormData formData = processEngine.getFormService().getTaskFormData(task.getId());
                if (formData != null && formData.getFormProperties() != null && !formData.getFormProperties().isEmpty()) {
                    Map<String,Object> variables = new HashMap<>();
                    Scanner scanner = new Scanner(System.in);
                    formData.getFormProperties().forEach(formProperty -> {
                        if (StringFormType.class.isInstance(formProperty.getType())) {
                            System.out.println(formProperty.getName() + "?");
                            String value = scanner.nextLine();
                            variables.put(formProperty.getId(), value);
                        } else if (LongFormType.class.isInstance(formProperty.getType())) {
                            System.out.println(formProperty.getName() + "?");
                            Long value = Long.valueOf(scanner.nextLine());
                            variables.put(formProperty.getId(), value);
                        } else if (DateFormType.class.isInstance(formProperty.getType())) {
                            DateFormType type = (DateFormType)formProperty.getType();
                            System.out.println(formProperty.getName() + "? (Must be a date " + type.getInformation("datePattern").toString() + ")");
                            DateFormat dateFormat = new SimpleDateFormat(type.getInformation("datePattern").toString());
                            Date value = null;
                            try {
                                value = dateFormat.parse(scanner.nextLine());
                            } catch (ParseException e) {
                                throw new RuntimeException(e);
                            }
                            variables.put(formProperty.getId(), value);
                        } else {
                            System.out.println("<form type not supported>");
                        }
                    });
                    processEngine.getTaskService().complete(task.getId(),variables);
                    System.out.println(task.getName()+"任务完成....");
                }
            }
        });
        return result;
    }


}
