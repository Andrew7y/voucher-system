package com.example.vouchersystem.constant;

public class RedisKeyConst {
    public static final String VOUCHER_QUOTA_PREFIX = "voucher:quota:";
    public static final String VOUCHER_CLAIMED_USERS_PREFIX = "voucher:claimed:";
    public static final String VOUCHER_INFO_PREFIX = "voucher:info:";

    private RedisKeyConst(){}

    public static String getQuotaKey(Long ruleId){
        return VOUCHER_QUOTA_PREFIX + ruleId;
    }

    public static String getClaimedUsersKey(Long ruleId){
        return VOUCHER_CLAIMED_USERS_PREFIX + ruleId;
    }

    public static String getInfoKey(Long ruleId){
        return VOUCHER_INFO_PREFIX + ruleId;
    }
}
