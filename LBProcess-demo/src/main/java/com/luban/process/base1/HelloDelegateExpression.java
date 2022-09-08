package com.luban.process.base1;

import lombok.Data;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.delegate.JavaDelegate;

@Data
public class HelloDelegateExpression implements JavaDelegate {

    private Expression test1;

    private Expression test2;


    @Override
    public void execute(DelegateExecution execution) {
        Number value1 = (Number)test1.getValue(execution);
        Number value2 = (Number)test2.getValue(execution);
        int result = value1.intValue() + value2.intValue();
        System.out.println(test1.getExpressionText() + "------->"+value1);
        System.out.println(test2.getExpressionText() + "------->"+value2);
        System.out.println(test1.getExpressionText() + "+" + test2.getExpressionText() + "------->"+result);
    }
}
