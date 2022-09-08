package com.luban.process.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseCode {

    NOT_FOUND(404,"not found");

    private final Integer code;

    private final String message;

}
