package com.luban.process.config;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.FormService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.impl.form.DateFormType;
import org.activiti.engine.impl.form.StringFormType;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;

@Slf4j
public class ConfigMain {
    
    
    public static void main(String[] args) throws ParseException {

        log.info("启动程序");
        // 创建流程引擎
        ProcessEngine processEngine = getProcessEngine();

        // 部署流程定义文件
        ProcessDefinition processDefinition = getProcessDefinition(processEngine);

        // 启动运行流程
        ProcessInstance processInstance = getProcessInstance(processEngine, processDefinition);

        // 处理流程任务
        processTask(processEngine, processInstance);

        log.info("结束程序");

    }

    private static void processTask(ProcessEngine processEngine, ProcessInstance processInstance) throws ParseException {
        Scanner scanner = new Scanner(System.in);
        while (processInstance != null && !processInstance.isEnded()) {
            TaskService taskService = processEngine.getTaskService();
            List<Task> list = taskService.createTaskQuery().list();
            log.info("待处理任务数量 [{}]", list.size());
            for (Task task : list) {

                log.info("待处理任务 [{}]", task.getName());
                Map<String, Object> variables = getMap(processEngine, scanner, task);
                taskService.complete(task.getId(), variables);
                processInstance = processEngine.getRuntimeService()
                    .createProcessInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .singleResult();
            }
        }
        scanner.close();
    }

    private static Map<String, Object> getMap(ProcessEngine processEngine, Scanner scanner, Task task) throws ParseException {
        FormService formService = processEngine.getFormService();
        TaskFormData taskFormData = formService.getTaskFormData(task.getId());
        List<FormProperty> formProperties = taskFormData.getFormProperties();
        Map<String, Object> variables = new HashMap<>();
        for (FormProperty property : formProperties) {
            String line = null;
            if (StringFormType.class.isInstance(property.getType())) {
                log.info("请输入 {} ？", property.getName());
                line = scanner.nextLine();
                variables.put(property.getId(), line);
            } else if (DateFormType.class.isInstance(property.getType())) {
                log.info("请输入 {} ？ 格式 （yyyy-MM-dd）", property.getName());
                line = scanner.nextLine();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                Date date = dateFormat.parse(line);
                variables.put(property.getId(), date);
            } else {
                log.info("类型暂不支持 {}", property.getType());
            }
            log.info("您输入的内容是 [{}]", line);

        }
        return variables;
    }

    private static ProcessInstance getProcessInstance(ProcessEngine processEngine, ProcessDefinition processDefinition) {
        RuntimeService runtimeService = processEngine.getRuntimeService();
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
        log.info("启动流程 [{}]", processInstance.getProcessDefinitionKey());
        return processInstance;
    }

    private static ProcessDefinition getProcessDefinition(ProcessEngine processEngine) {
        RepositoryService repositoryService = processEngine.getRepositoryService();
        DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();
        deploymentBuilder.addClasspathResource("second_approve.bpmn20.xml");
        Deployment deployment = deploymentBuilder.deploy();
        String deploymentId = deployment.getId();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
            .deploymentId(deploymentId).singleResult();
        log.info("流程定义文件 [{}]，流程ID [{}]", processDefinition.getName(), processDefinition.getId());
        return processDefinition;
    }

    private static ProcessEngine getProcessEngine() {
        ProcessEngineConfiguration cfg = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration();
        ProcessEngine processEngine = cfg.buildProcessEngine();
        String name = processEngine.getName();
        String version = ProcessEngine.VERSION;
        log.info("流程引擎名称{}，版本{}", name, version);
        return processEngine;
    }
}
