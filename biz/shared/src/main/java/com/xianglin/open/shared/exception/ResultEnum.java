package com.xianglin.open.shared.exception;

/**
 * 返回结果信息
 */
public enum ResultEnum {

    SUCCESS("000000", "操作成功"),


    ERROR_400001("400001","接口编号错误"),
    ERROR_400002("400002","超过交易时间"),
    ERROR_400003("400003","交易号已存在"),
    ERROR_400004("400004","渠道号为空或错误"),
    ERROR_400005("400005","签名校验失败"),
    ERROR_400006("400006","生成授权码失败"),
    ERROR_400007("400007","授权码已过期"),
    ERROR_400008("400008","opneId错误"),
    ERROR_400009("400009","刷新令牌错误"),
    ERROR_400010("400010","刷新令牌已过期"),
    ERROR_400011("400011","授权码已过期"),
    ERROR_400012("400012","渠道id错误"),
    ERROR_400013("400013","访问令牌已过期"),
    ERROR_500000("500000","服务错误"),

    ;
    /**
     * 结果码
     */
    private final String code;

    /**
     * 用户友好提示
     */
    private final String message;

    ResultEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
