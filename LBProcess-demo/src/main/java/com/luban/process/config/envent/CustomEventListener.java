package com.luban.process.config.envent;

import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiEventType;

@Slf4j
public class CustomEventListener implements ActivitiEventListener {

    @Override
    public void onEvent(ActivitiEvent event) {
        if (ActivitiEventType.CUSTOM == event.getType()) {
            log.info("监听到自定义事件：[{}] [{}]",event.getType(),event.getProcessDefinitionId());
        }
    }

    @Override
    public boolean isFailOnException() {
        return false;
    }
}
