-- 有序集合保存每个预占的独立截止时间，哈希保存同次报名的令牌。
local student = ARGV[1]
local limit = tonumber(ARGV[2])
local ttl = tonumber(ARGV[3])
local now = tonumber(redis.call('TIME')[1])
local expired = redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', now, 'LIMIT', 0, 100)
for _, member in ipairs(expired) do
    redis.call('HDEL', KEYS[2], member)
    redis.call('ZREM', KEYS[1], member)
end
local deadline = tonumber(redis.call('ZSCORE', KEYS[1], student))
if deadline and deadline > now then return 3 end
if limit > 0 and redis.call('ZCOUNT', KEYS[1], '(' .. now, '+inf') >= limit then return 2 end
redis.call('ZADD', KEYS[1], now + ttl, student)
redis.call('HSET', KEYS[2], student, ARGV[4])
redis.call('EXPIRE', KEYS[1], ttl)
redis.call('EXPIRE', KEYS[2], ttl)
return 1
