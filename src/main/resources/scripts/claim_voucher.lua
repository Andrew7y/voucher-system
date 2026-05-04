-- KEYS[1] voucher quota key
-- KEYS[2] voucher claimed users set key
-- ARGV[1] user id

-- Check if the user has already claimed the voucher
if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
    return -2 -- Duplicate claim
end

-- Check if there are vouchers available
local quota = redis.call('GET', KEYS[1])
if not quota then
    return -1 -- Not found / Not Started
end

-- Attempt to decrement the voucher quota and save user claim
local q = tonumber(quota)
if q > 0 then
    redis.call('DECR', KEYS[1]) -- Decrement the voucher quota
    redis.call('SADD', KEYS[2], ARGV[1]) -- Add user to claimed set
    return 1 -- Success
else
    return 0 -- Sold Out
end