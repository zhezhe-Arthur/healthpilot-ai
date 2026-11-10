
package com.healthpilot.server.auth.exception;

public class InvalidRefreshTokenException
        extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("刷新凭证无效或已过期");
    }
}
