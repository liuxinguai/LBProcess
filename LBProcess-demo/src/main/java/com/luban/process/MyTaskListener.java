package com.luban.process;

import java.util.Arrays;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

public class MyTaskListener implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {

        String eventName = delegateTask.getEventName();
        if (StringUtils.endsWith("create", eventName)) {
            System.out.println("config by listener");

            delegateTask.addCandidateUsers(Arrays.asList("user1", "user2"));
            delegateTask.addCandidateGroup("group1");
            delegateTask.setVariable("key1", "value1");

            delegateTask.setDueDate(DateTime.now().plusDays(3).toDate());
        } else if (StringUtils.endsWith("complete", eventName)) {
            System.out.println("task complete");
        }
    }
}