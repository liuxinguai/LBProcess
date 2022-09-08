package com.luban.process;

import org.activiti.engine.ProcessEngine;

/**
 * Interface to be implemented by beans that wish to be aware of their owning {@link ProcessEngine}.
 * @author xinguai.liu
 */
public interface ProcessEngineAware {

    void setProcessEngine(ProcessEngine processEngine);

}
