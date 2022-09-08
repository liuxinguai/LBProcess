package com.luban.process.config.envent;

import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiEventType;

@Slf4j
public class ProcessEventListener implements ActivitiEventListener {

    @Override
    public void onEvent(ActivitiEvent event) {
        if (event.getType() == ActivitiEventType.PROCESS_STARTED) {
            log.info("[{}]流程启动",event.getProcessDefinitionId());
        } else if (event.getType() == ActivitiEventType.PROCESS_COMPLETED) {
            log.info("{}流程完成",event.getProcessDefinitionId());
        }
    }

    @Override
    public boolean isFailOnException() {
        return false;
    }
}
