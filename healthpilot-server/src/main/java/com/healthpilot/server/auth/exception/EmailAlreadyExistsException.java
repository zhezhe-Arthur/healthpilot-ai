package com.healthpilot.server.auth.exception;

public class EmailAlreadyExistsException extends RuntimeException  {
    public EmailAlreadyExistsException() {
        super("该邮箱已被注册");
    }
}
