package com.luban.process.model;

import java.io.Serializable;
import lombok.Data;

@Data
public class Request<T> implements Serializable {

    private String code;

    private String message;

    private T data;

}
