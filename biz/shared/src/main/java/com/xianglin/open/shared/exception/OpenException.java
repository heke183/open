package com.xianglin.open.shared.exception;


/**
 * open系统异常
 */
public class OpenException extends Exception{

    /**
     * 返回码
     */
    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public OpenException(String message) {
        super(message);
    }

    public OpenException(String code,String message) {
        super(message);
        this.code = code;
    }
    
    public OpenException(ResultEnum result) {
        super(result.getMessage());
        this.code = result.getCode();
    }
}
