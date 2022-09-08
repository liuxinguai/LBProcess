package com.luban.process.back;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.activiti.engine.IdentityService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;

public class ActivitiDeploy {

    public static void main(String[] args) {
        //获取流程引擎
        ProcessEngine engine = ProcessEngines.getDefaultProcessEngine();
        //获取流程存储服务组件
        RepositoryService repositoryService = engine.getRepositoryService();
        //部署流程描述文件
        DeploymentBuilder builder = repositoryService.createDeployment();
        builder.addClasspathResource("bpmn/applyHoliday.bpmn").deploy();
        //获取运行时服务组件
        RuntimeService runtimeService = engine.getRuntimeService();
        TaskService taskService = engine.getTaskService();
        //启动流程引擎
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("applyHoliday2");
        //获取身份服务组件
        IdentityService identityService = engine.getIdentityService();
        System.out.println("启动成功，流程id为：" + processInstance.getId());
        //创建模拟用户
        createUser(identityService);
        //启动任务
        startTask(taskService, processInstance.getId());
    }
    /**
     * 设置会签人员并启动任务
     * @param taskService
     * @param proId
     */
    public static void startTask(TaskService taskService, String proId){
        Task task = taskService.createTaskQuery().processInstanceId(proId).singleResult();
        System.out.println("当前任务编码：" + task.getId() + "，当前任务名称：" + task.getName());
        //设置会签人员
        Map<String, Object> var = new HashMap<String, Object>();
        List<String> signList = new ArrayList<String>();
        signList.add("zhangSan");
        signList.add("liSi");
        var.put("signList", signList);
        taskService.complete(task.getId(), var);
    }
    /**
     * 新增员工，模拟流程
     * @param identityService
     */
    public static void createUser(IdentityService identityService){
//        Emp.createUser(identityService, "zhangSan", "张", "三");
//        Emp.createUser(identityService, "liSi", "李", "四");
//        Emp.createUser(identityService, "wangWu", "王", "五");
        System.out.println("新建员工成功");
    }

}
