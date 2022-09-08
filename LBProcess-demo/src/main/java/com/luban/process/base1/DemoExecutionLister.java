package com.luban.process.base1;

import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;

@Slf4j
public class DemoExecutionLister implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) {
        log.info("{}执行",execution.getEventName());
    }
}
