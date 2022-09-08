package com.luban.process.gateway;

import com.luban.process.base1.HelloDelegateExpression;
import com.luban.process.base1.IntegerFormType;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
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
import org.activiti.engine.impl.form.BooleanFormType;
import org.activiti.engine.impl.form.DateFormType;
import org.activiti.engine.impl.form.StringFormType;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;

@Slf4j
public class Demo {

    public static void main(String[] args) throws ParseException {
        ProcessEngine engine = builder();
        Deployment deploy = deploy("gateway/gateway1.bpmn20.xml", engine,"gateway1");
        ProcessInstance instance = start(engine, deploy);
        hanle(engine,instance);
    }

    protected static void hanle(ProcessEngine processEngine, ProcessInstance processInstance)
        throws ParseException {
        Scanner scanner = new Scanner(System.in);
        List<Task> tasks = processEngine.getTaskService().createTaskQuery().processDefinitionId(processInstance.getProcessDefinitionId())
            .taskAssignee("liuxg").list();
        log.info("liuxg待处理任务数量 [{}]", tasks.size());
        for (Task task : tasks) {
            log.info("liuxg待处理任务 [{}]", task.getName());
            Map<String, Object> variables = getVariables(processEngine, scanner, task);
            if (variables == null || variables.isEmpty()) {
                variables = task.getProcessVariables();
                variables.forEach((key,value)->{
                    log.info("输入的内容：[{}-{}]",key,value);
                });
            }
            processEngine.getTaskService().complete(task.getId(), variables);
        }
        scanner.close();
        tasks = processEngine.getTaskService().createTaskQuery()
            .processDefinitionId(processInstance.getProcessDefinitionId())
            .taskAssignee("manager").list();
        log.info("人事待处理任务数量 [{}]", tasks.size());
        for (Task task : tasks) {
            log.info("人事待处理任务 [{}]", task.getName());
            Map<String, Object> variables = processEngine.getRuntimeService().getVariables(task.getExecutionId());
            if (variables != null && !variables.isEmpty()) {
                log.info("人事获取到的表单数据：");
                variables.forEach((key,value)-> log.info("输入的内容：[{}-{}]",key,value));
            }
            processEngine.getTaskService().complete(task.getId());
        }
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
            } else if (IntegerFormType.class.isInstance(property.getType())) {
                log.info("请输入 {}", property.getName());
                line = scanner.nextLine();
                variables.put(property.getId(), line);
            } else if (BooleanFormType.class.isInstance(property.getType())) {
                log.info("请输入 {}", property.getName());
                line = scanner.nextLine();
                variables.put(property.getId(), line);
            } else {
                log.warn("类型暂不支持 {}", property.getType());
            }
        }
        return variables;
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
        configuration.setCustomFormTypes(Arrays.asList(new IntegerFormType()));
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
