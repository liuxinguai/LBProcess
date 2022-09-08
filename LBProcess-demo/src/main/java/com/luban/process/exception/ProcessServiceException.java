package com.luban.process.exception;

import lombok.Data;

@Data
public class ProcessServiceException extends RuntimeException {

    private Integer code;

    public ProcessServiceException(Integer code) {
        super();
        this.code = code;
    }

    public ProcessServiceException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public ProcessServiceException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public ProcessServiceException(Integer code, Throwable cause) {
        super(cause);
        this.code = code;
    }

    public ProcessServiceException(Integer code, String message, Throwable cause, boolean enableSuppression,
        boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.code = code;
    }
}
