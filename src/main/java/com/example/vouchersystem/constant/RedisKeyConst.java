package com.example.vouchersystem.constant;

public class RedisKeyConst {
    public static final String VOUCHER_QUOTA_PREFIX = "voucher:quota:";

    private RedisKeyConst(){}

    public static String getQuotaKey(Long ruleId){
        return VOUCHER_QUOTA_PREFIX + ruleId;
    }
}
