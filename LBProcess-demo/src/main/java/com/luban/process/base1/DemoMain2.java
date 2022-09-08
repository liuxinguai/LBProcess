package com.luban.process.base1;

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
import org.activiti.engine.TaskService;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.activiti.engine.impl.form.DateFormType;
import org.activiti.engine.impl.form.StringFormType;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;

@Slf4j
public class DemoMain2 {

    public static void main(String[] args) throws ParseException {
        ProcessEngine engine = builder();
        Deployment deploy = deploy("base/base5.bpmn20.xml", engine,"base5");
        ProcessInstance instance = start(engine, deploy);
        hanle(engine,instance);
    }

    protected static void hanle(ProcessEngine processEngine, ProcessInstance processInstance)
        throws ParseException {
        Scanner scanner = new Scanner(System.in);
        while (processInstance != null && !processInstance.isEnded()) {
            TaskService taskService = processEngine.getTaskService();
            List<Task> list = taskService.createTaskQuery().list();
            log.info("待处理任务数量 [{}]", list.size());
            for (Task task : list) {
                log.info("待处理任务 [{}]", task.getName());
                if ("liuxg".equals(task.getAssignee())) {
                    taskService.complete(task.getId());
                } else {
                    log.info("请输入要增加的候选人");
                    String username = scanner.nextLine();
                    if (username.equals("liuxg")) {
                        taskService.setAssignee(task.getId(),username);
                        taskService.complete(task.getId());
                    } else {
                        taskService.addCandidateUser(task.getId(),username);
//                        taskService.claim(task.getId(),username);
                        List<Task> tasks = taskService.createTaskQuery().taskCandidateUser(username)
                            .list();
                        log.info("{}获取到的任务数：{}",username,tasks.size());
                    }
                }
                processInstance = processEngine.getRuntimeService().createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
            }
        }
        scanner.close();
    }

    protected static Deployment deploy(String resource,ProcessEngine processEngine, String processKey) {
        Deployment deployment = processEngine.getRepositoryService().createDeploymentQuery()
            .deploymentKey(processKey).singleResult();
        if (deployment == null) {
            DeploymentBuilder builder = processEngine.getRepositoryService().createDeployment()
                .deploymentProperty("key", processKey);
            builder.tenantId("liuxg");
            builder.key(processKey);
            return builder.addClasspathResource(resource).deploy();
        }
        return deployment;
    }

    protected static ProcessInstance start(ProcessEngine processEngine, Deployment deployment) {
        ProcessDefinition result = processEngine.getRepositoryService()
            .createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
       return processEngine.getRuntimeService()
            .startProcessInstanceById(result.getId());
    }

    protected static ProcessEngine builder() {
        StandaloneProcessEngineConfiguration configuration = new StandaloneProcessEngineConfiguration();
        Map<Object,Object> beans = new HashMap<>();
//        beans.put("helloService",new HelloService());
        beans.put("helloDelegateExpression",new HelloDelegateExpression());
        configuration.setBeans(beans);
//        configuration.setCommandInvoker(new MDCCommandInvoker());
//        List<CommandInterceptor> list = new ArrayList<>();
//        list.add(new DurationCommandInterceptor());
//        configuration.setCustomPreCommandInterceptors(list);
//        List<ActivitiEventListener> list2 = new ArrayList<>();
//        list2.add(new CustomEventListener());
//        list2.add(new ProcessEventListener());
//        configuration.setEventListeners(list2);
        ProcessEngineConfiguration cfg = configuration
            .setJdbcUrl("jdbc:mysql://192.168.3.210:3306/process_test?useUnicode=true&characterEncoding=UTF-8&useSSL=false")
            .setJdbcUsername("ps_r")
            .setJdbcPassword("ps_r@123")
            .setJdbcDriver("com.mysql.cj.jdbc.Driver")
            .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);

        ProcessEngine processEngine = cfg.buildProcessEngine();
        log.info("启动流程引擎，引擎：{}",processEngine.getName());
        return processEngine;
    }


}
