local students_key = KEYS[1]
local count_key = KEYS[2]
local student_id = ARGV[1]
local limit = tonumber(ARGV[2])
local db_count = tonumber(ARGV[3])
local ttl = tonumber(ARGV[4])

if redis.call('EXISTS', count_key) == 0 then
  redis.call('SET', count_key, db_count)
end

if redis.call('SISMEMBER', students_key, student_id) == 1 then
  return 1
end

local current = tonumber(redis.call('GET', count_key) or db_count)
if limit > 0 and current >= limit then
  return 2
end

redis.call('SADD', students_key, student_id)
redis.call('INCR', count_key)
redis.call('EXPIRE', students_key, ttl)
redis.call('EXPIRE', count_key, ttl)
return 0
