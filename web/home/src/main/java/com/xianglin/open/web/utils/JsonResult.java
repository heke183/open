package com.xianglin.open.web.utils;

import com.xianglin.open.shared.exception.ResultEnum;
import sun.management.snmp.jvminstr.JvmThreadInstanceEntryImpl;

/** json格式返回信息
 * @param <T>
 */
public class JsonResult <T>{

    /**
     * 返回码
     */
    private String code;

    /**
     * 提示信息
     */
    private String message;

    /**
     * 内容
     */
    private T content;

    public JsonResult() {
        this.code = ResultEnum.SUCCESS.getCode();
        this.message = ResultEnum.SUCCESS.getMessage();
    }

    public void setResult(ResultEnum result){
        this.code = result.getCode();
        this.message = result.getMessage();
    }

    public void setResult(String code,String message){
        this.code = code;
        this.message = message;
    }

    /**判断返回是否成功
     * @return
     */
   public boolean isSuccess(){
        return ResultEnum.SUCCESS.getCode().equals(this.code);
   }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getContent() {
        return content;
    }

    public void setContent(T content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "JsonResponse{" +
                "code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", content=" + content +
                '}';
    }
}
