package com.luban.process.config.interceptor;

import java.time.Duration;
import java.time.LocalTime;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.impl.interceptor.AbstractCommandInterceptor;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandConfig;
@Slf4j
public class DurationCommandInterceptor extends AbstractCommandInterceptor {

    @Override
    public <T> T execute(CommandConfig config, Command<T> command) {
        LocalTime start = LocalTime.now();
        try {
            return this.getNext().execute(config, command);
        } finally {
            LocalTime end = LocalTime.now();
            long l = Duration.between(start, end).toMillis();
            log.info("{} 执行时长 {} 毫秒", command.getClass().getSimpleName(), l);
        }
    }

}
