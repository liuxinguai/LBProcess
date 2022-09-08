package com.luban.process.base1;

import org.activiti.engine.form.AbstractFormType;

public class IntegerFormType extends AbstractFormType {

    @Override
    public Object convertFormValueToModelValue(String propertyValue) {
        if (propertyValue == null || "".equals(propertyValue)) {
            return null;
        }
        return new Integer(propertyValue);
    }

    @Override
    public String convertModelValueToFormValue(Object modelValue) {
        if (modelValue == null) {
            return null;
        }
        return modelValue.toString();
    }

    @Override
    public String getName() {
        return "integer";
    }
}
