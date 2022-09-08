package com.luban.process.config;

import com.luban.process.config.envent.CustomEventListener;
import com.luban.process.config.envent.ProcessEventListener;
import com.luban.process.config.interceptor.DurationCommandInterceptor;
import com.luban.process.config.interceptor.MDCCommandInvoker;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.activiti.engine.impl.form.DateFormType;
import org.activiti.engine.impl.form.StringFormType;
import org.activiti.engine.impl.interceptor.CommandInterceptor;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;

@Slf4j
public class DemoMain {

    public static void main(String[] args) throws ParseException {
        ProcessEngine engine = builder();
        Deployment deploy = deploy("my-process.bpmn20.xml", engine);
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
                Map<String, Object> variables = getVariables(processEngine, scanner, task);
                taskService.complete(task.getId(), variables);
                processInstance = processEngine.getRuntimeService().createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
            }
        }
        scanner.close();
    }

    private static Map<String, Object> getVariables(ProcessEngine processEngine, Scanner scanner, Task task) throws ParseException {
        FormService formService = processEngine.getFormService();
        TaskFormData taskFormData = formService.getTaskFormData(task.getId());
        List<FormProperty> formProperties = taskFormData.getFormProperties();
        Map<String, Object> variables = new HashMap<>();
        for (FormProperty property : formProperties) {
            String line = null;
            if (StringFormType.class.isInstance(property.getType())) {
                log.info("请输入 {}", property.getName());
                line = scanner.nextLine();
                variables.put(property.getId(), line);
            } else if (DateFormType.class.isInstance(property.getType())) {
                DateFormType formType = (DateFormType)property.getType();
                String format = formType.getInformation("datePattern").toString();
                log.info("请输入 {} ? 格式 ( {} )", property.getName(), format);
                line = scanner.nextLine();
                SimpleDateFormat dateFormat = new SimpleDateFormat(format);
                Date date = dateFormat.parse(line);
                variables.put(property.getId(), date);
            } else {
                log.warn("类型暂不支持 {}", property.getType());
            }
            log.info("您输入的内容是 [{}]", line);
        }
        return variables;
    }

    protected static Deployment deploy(String resource,ProcessEngine processEngine) {
        return processEngine.getRepositoryService().createDeployment().addClasspathResource(resource).deploy();
    }

    protected static ProcessInstance start(ProcessEngine processEngine, Deployment deployment) {
        ProcessDefinition result = processEngine.getRepositoryService()
            .createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
       return processEngine.getRuntimeService()
            .startProcessInstanceById(result.getId());
    }

    protected static ProcessEngine builder() {
        StandaloneProcessEngineConfiguration configuration = new StandaloneProcessEngineConfiguration();
        configuration.setCommandInvoker(new MDCCommandInvoker());
        List<CommandInterceptor> list = new ArrayList<>();
        list.add(new DurationCommandInterceptor());
        configuration.setCustomPreCommandInterceptors(list);
        List<ActivitiEventListener> list2 = new ArrayList<>();
        list2.add(new CustomEventListener());
        list2.add(new ProcessEventListener());
        configuration.setEventListeners(list2);
        ProcessEngineConfiguration cfg = configuration
            .setJdbcUrl("jdbc:h2:mem:activiti;DB_CLOSE_DELAY=1000")
            .setJdbcUsername("sa")
            .setJdbcPassword("")
            .setJdbcDriver("org.h2.Driver")
            .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);

        ProcessEngine processEngine = cfg.buildProcessEngine();
        log.info("启动流程引擎，引擎：{}",processEngine.getName());
        return processEngine;
    }


}
