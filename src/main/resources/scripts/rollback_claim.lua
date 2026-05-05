-- KEYS[1] voucher quota
-- KEYS[2] list users claimed voucher
-- ARGV[1] current userId

if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
    redis.call('SREM', KEYS[2], ARGV[1])
    redis.call('INCR', KEYS[1])
    return 1
else
    return 0
end