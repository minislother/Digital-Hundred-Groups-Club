"""Reproducible local HTTP baseline; only synthetic, isolated benchmark services.

Uses real JWT verification, controller/service/Redis/MySQL paths. Never imports
dump INSERT rows. Numbers describe this closed-loop local workload, not production.
Run with Python 3: interview_benchmark.py setup|run|stop
Docker Desktop and a built target/shop-0.0.1-SNAPSHOT.jar are required.
"""
import base64, concurrent.futures, csv, hashlib, hmac, http.client, json
import math, os, platform, re, statistics, subprocess, sys, threading, time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'target/interview-benchmark'
DOCKER = r'C:\Program Files\Docker\Docker\resources\bin\docker.exe'
JAVA = r'C:\Users\30983\.jdks\ms-17.0.18\bin\java.exe'
MYSQL = 'codex-baituan-bench-mysql'
REDIS = 'codex-baituan-bench-redis'
SECRET = base64.b64encode(b'isolated-benchmark-only-key-' * 4).decode()
OUT.mkdir(parents=True, exist_ok=True)

def cmd(args, data=None):
    return subprocess.run(args, input=data, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                          check=True).stdout.decode('utf-8', errors='replace').strip()

def sql(statement):
    return cmd([DOCKER, 'exec', '-i', MYSQL, 'mysql', '-uroot',
                '-plocal-benchmark-only', '--default-character-set=utf8mb4',
                '-N', 'club_benchmark'], statement.encode('utf-8'))

def redis(*args):
    return cmd([DOCKER, 'exec', REDIS, 'redis-cli', *map(str, args)])

def token(user='bench-reader', role='STUDENT'):
    def b64(b): return base64.urlsafe_b64encode(b).rstrip(b'=').decode()
    now = int(time.time())
    payload = b64(json.dumps({'alg':'HS512','typ':'JWT'}).encode())+'.'+b64(
        json.dumps({'sub':user, 'role':role, 'iat':now, 'exp':now+7200}).encode())
    return payload+'.'+b64(hmac.new(base64.b64decode(SECRET), payload.encode(), hashlib.sha512).digest())

def setup():
    # Refuse to replace data: the freshly created database must be empty.
    assert sql('SHOW TABLES') == '', 'Benchmark database is not empty; choose fresh isolated containers.'
    source = (ROOT.parent/'club_finalversion.sql').read_text(encoding='utf-8')
    schema = re.findall(r'CREATE TABLE .*?;\s*', source, re.S)
    sql('SET FOREIGN_KEY_CHECKS=0;\n'+'\n'.join(schema))
    sql((ROOT/'src/main/resources/cache-consistency-schema.sql').read_text(encoding='utf-8'))
    for start in range(1, 10001, 500):
        rows = [f"({i},'BenchActivity{i}','BenchClub','Synthetic activity {i}',{i%100},100000,1)"
                for i in range(start,start+500)]
        sql('INSERT INTO activity(id,name,group_name,description,hot,number,is_accepted) VALUES '+','.join(rows))
    launch()

def launch():
    env = os.environ.copy()
    env.update(DB_URL='jdbc:mysql://127.0.0.1:13306/club_benchmark?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true',
               DB_USERNAME='root', DB_PASSWORD='local-benchmark-only', SERVER_PORT='18081',
               REDIS_HOST='127.0.0.1', REDIS_PORT='16379', REDIS_PASSWORD='', REDIS_DATABASE='0',
               JWT_SECRET=SECRET, KAFKA_ENABLED='false', KAFKA_LISTENER_AUTO_STARTUP='false',
               ELASTICSEARCH_ENABLED='false', REQUEST_LOG_ENABLED='false',
               BUSINESS_CACHE_ENABLED='true', LOCAL_CACHE_ENABLED='true', RATE_LIMIT_FAIL_OPEN='false')
    log = open(OUT/'server.log','wb')
    args=[JAVA,'-Xms512m','-Xmx1024m','-jar',str(ROOT/'target/shop-0.0.1-SNAPSHOT.jar'),
                              '--app.activity.async-write-enabled=false',
                              '--spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration']
    if os.getenv('BENCH_POOL_MAX'): args.append('--spring.datasource.hikari.maximum-pool-size='+os.environ['BENCH_POOL_MAX'])
    proc = subprocess.Popen(args, cwd=OUT, env=env,
                              stdout=log, stderr=subprocess.STDOUT, creationflags=0x08000000)
    (OUT/'server.pid').write_text(str(proc.pid))
    facts = {'timestamp':time.strftime('%Y-%m-%dT%H:%M:%S%z'), 'platform':platform.platform(),
             'cpu_logical':os.cpu_count(), 'git_head':cmd(['git','-C',str(ROOT),'rev-parse','HEAD']),
             'git_dirty':cmd(['git','-C',str(ROOT),'status','--porcelain']),
             'jar_sha256':hashlib.sha256((ROOT/'target/shop-0.0.1-SNAPSHOT.jar').read_bytes()).hexdigest(),
             'java':subprocess.run([JAVA,'-version'],capture_output=True,text=True).stderr,
             'mysql':sql('SELECT VERSION()'), 'redis':redis('INFO','server').splitlines()[:5],
             'activities':10000, 'dataset':'synthetic; no real user rows; JWT subjects are synthetic',
             'jvm':'Xms512m Xmx1024m', 'cache':'Caffeine L1 + Redis L2 enabled; warmed reads',
             'mysql_pool':os.getenv('BENCH_POOL_MAX','10'), 'logs':'detailed controller request logging disabled; mapper DEBUG retained',
             'kafka':False,'elasticsearch':False,'async_admission':False,
             'build_override':'mvn -DskipTests package; ES auto-configuration excluded at launch (unresolved ES dependency mismatch)',
             'limiter':'enabled, 200 requests / 10s / method / forwarded IP',
             'load':'local HTTP/1.1 keep-alive, closed loop, zero think time; full response body read',
             'forwarded_ip':'each request gets synthetic client IP; single-IP guard tested separately'}
    (OUT/'environment.json').write_text(json.dumps(facts,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps({'server_pid':proc.pid, 'output':str(OUT)},ensure_ascii=False),flush=True)

SEQ = 0
LOCK = threading.Lock()
def next_id():
    global SEQ
    with LOCK: SEQ += 1; return SEQ

def request(conn, kind, fixed_ip=None, fixed_user=None, activity=9001):
    ident = next_id()
    user = fixed_user or (f'bench-{ident}' if kind=='join' else 'bench-reader')
    role = 'MANAGER' if kind=='sql_detail' else 'STUDENT'
    headers = {'Authorization':'Bearer '+token(user,role), 'Content-Type':'application/x-www-form-urlencoded',
               'X-Forwarded-For':fixed_ip or f'10.{(ident//65536)%256}.{(ident//256)%256}.{ident%256}'}
    endpoint,body = {
        'list':('/activity/all?pageNum=1&pageSize=20',''),
        'detail':('/activity/studentDetail','groupName=BenchClub&activityName=BenchActivity5000'),
        'sql_detail':('/activity/managerDetail','groupName=BenchClub&activityName=BenchActivity5000'),
        'join':('/individualActivity/joinActivity',f'activityId={activity}'),
        'search':('/activity/search?keyword=Bench',''),
    }[kind]
    start=time.perf_counter()
    try:
        conn.request('POST' if body else 'GET',endpoint,body,headers)
        resp=conn.getresponse(); raw=resp.read(); latency=(time.perf_counter()-start)*1000
        doc=json.loads(raw); code=doc.get('code'); ok=resp.status==200 and code==20000
        if ok and kind in ('detail','sql_detail'): ok=(doc.get('data',{}).get('activity') or {}).get('id')==5000
        if ok and kind=='list': ok=len(doc.get('data',{}).get('items',[]))==20
        return latency,ok,resp.status,str(code),doc.get('message',''),len(raw)
    except Exception as exc:
        conn.close()
        return (time.perf_counter()-start)*1000,False,0,type(exc).__name__,str(exc)[:150],0

def sample(kind, concurrency, duration, repeat):
    barrier=threading.Barrier(concurrency+1); rows=[]
    def worker(worker_id):
        conn=http.client.HTTPConnection('127.0.0.1',18081,timeout=15)
        barrier.wait(); local=[]
        while time.perf_counter()<deadline[0]:
            result=request(conn,kind)
            local.append((kind,concurrency,repeat,worker_id,*result))
        conn.close(); return local
    deadline=[0]
    with concurrent.futures.ThreadPoolExecutor(max_workers=concurrency) as pool:
        futures=[pool.submit(worker,i) for i in range(concurrency)]
        begin=time.perf_counter(); deadline[0]=begin+duration; barrier.wait()
        for f in futures: rows.extend(f.result())
        elapsed=time.perf_counter()-begin
    good=[r[4] for r in rows if r[5]]; good.sort()
    def pct(p): return good[max(0,math.ceil(len(good)*p)-1)] if good else None
    from collections import Counter
    stats={'kind':kind,'concurrency':concurrency,'repeat':repeat,'seconds':round(elapsed,4),
           'requests':len(rows),'success':len(good),'errors':len(rows)-len(good),
           'success_qps':round(len(good)/elapsed,2),'all_qps':round(len(rows)/elapsed,2),
           'mean_ms':round(statistics.mean(good),3) if good else None,
           'p50_ms':pct(.50),'p95_ms':pct(.95),'p99_ms':pct(.99),
           'error_counts':dict(Counter(f'{r[6]}:{r[7]}:{r[8]}' for r in rows if not r[5]))}
    with open(OUT/'requests.csv','a',newline='',encoding='utf-8') as f: csv.writer(f).writerows(rows)
    print(json.dumps(stats,ensure_ascii=False),flush=True)
    return stats

def run():
    (OUT/'initial-counts.txt').write_text(sql('SELECT activity_id,count(*) FROM individual_activity GROUP BY activity_id'),encoding='utf-8')
    conn=http.client.HTTPConnection('127.0.0.1',18081,timeout=15)
    probes={k:request(conn,k) for k in ('list','detail','sql_detail','search')}
    (OUT/'probes.json').write_text(json.dumps(probes,ensure_ascii=False,indent=2),encoding='utf-8')
    print('probes '+json.dumps(probes,ensure_ascii=False),flush=True)
    assert all(probes[k][1] for k in ('list','detail','sql_detail')), 'Preflight failed'
    # 250 fast requests from one IP expose the configured guard independently.
    guard=[request(conn,'detail',fixed_ip='198.51.100.1') for _ in range(250)]
    (OUT/'single-ip.json').write_text(json.dumps(guard,ensure_ascii=False),encoding='utf-8')
    with open(OUT/'requests.csv','w',newline='',encoding='utf-8') as f:
        csv.writer(f).writerow(['kind','concurrency','repeat','worker','latency_ms','success','http_status','business_code','message','bytes'])
    results=[]
    for kind in ('list','detail','sql_detail','join'):
        # Write warmup uses distinct identities and remains included in DB reconciliation.
        for _ in range(20): request(conn,kind)
        for concurrency in (1,8,32):
            for repeat in (1,2,3):
                results.append(sample(kind,concurrency,5,repeat))
                (OUT/'summary.json').write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf-8')
    conn.close()
    # Correctness under competition: 64 callers, capacity 10, then replay one user.
    sql('UPDATE activity SET number=10 WHERE id=9002')
    def race(i):
        c=http.client.HTTPConnection('127.0.0.1',18081,timeout=15)
        r=request(c,'join',fixed_user=f'race-{i}',activity=9002);c.close();return r
    with concurrent.futures.ThreadPoolExecutor(max_workers=32) as pool: race_results=list(pool.map(race,range(64)))
    check={'results':race_results,'replay':race(0),
           'database_counts':sql('SELECT activity_id,count(*),count(distinct user_id) FROM individual_activity GROUP BY activity_id'),
           'pending_invalidation':sql('SELECT count(*) FROM cache_invalidation_task'),
           'outbox':sql('SELECT count(*) FROM activity_admission_outbox'),
           'redis_pending_9001':redis('ZCARD','activity:apply:pending-expiry:{9001}'),
           'redis_pending_9002':redis('ZCARD','activity:apply:pending-expiry:{9002}')}
    (OUT/'correctness.json').write_text(json.dumps(check,ensure_ascii=False,indent=2),encoding='utf-8')
    print('correctness '+json.dumps({k:v for k,v in check.items() if k!='results'},ensure_ascii=False),flush=True)

def diagnose():
    """After restarting with BENCH_POOL_MAX=64, keep this evidence separate from baseline."""
    global SEQ
    SEQ=2000000
    connection=http.client.HTTPConnection('127.0.0.1',18081,timeout=15)
    for _ in range(20): request(connection,'join')
    connection.close()
    before=sql('SELECT count(*) FROM individual_activity WHERE activity_id=9001')
    results=[sample('join',32,5,i) for i in (1,2,3)]
    after=sql('SELECT count(*),count(distinct user_id) FROM individual_activity WHERE activity_id=9001')
    # A separate empty activity prevents previous timeout experiments contaminating correctness.
    assert sql('SELECT count(*) FROM individual_activity WHERE activity_id=9003')=='0'
    sql('UPDATE activity SET number=10 WHERE id=9003')
    def race(i):
        c=http.client.HTTPConnection('127.0.0.1',18081,timeout=15)
        r=request(c,'join',fixed_user=f'clean-race-{i}',activity=9003);c.close();return r
    with concurrent.futures.ThreadPoolExecutor(max_workers=32) as pool:
        races=list(pool.map(race,range(64)))
    winner=next((i for i,r in enumerate(races) if r[1]),None)
    replay=race(winner) if winner is not None else None
    evidence={'pool_max':64,'warmup':20,'before':before,'after':after,'runs':results,
              'race':races,'replay':replay,
              'race_db':sql('SELECT count(*),count(distinct user_id) FROM individual_activity WHERE activity_id=9003'),
              'race_redis_pending':redis('ZCARD','activity:apply:pending-expiry:{9003}')}
    (OUT/'pool64-warm-diagnostic.json').write_text(json.dumps(evidence,ensure_ascii=False,indent=2),encoding='utf8')
    print('DIAGNOSTIC_DONE '+json.dumps({k:v for k,v in evidence.items() if k not in ('race','runs')},ensure_ascii=False),flush=True)

if __name__=='__main__':
    if sys.argv[1]=='setup': setup()
    elif sys.argv[1]=='launch': launch()
    elif sys.argv[1]=='run': run()
    elif sys.argv[1]=='diagnose': diagnose()
    elif sys.argv[1]=='stop':
        cmd(['taskkill','/PID',(OUT/'server.pid').read_text(),'/T','/F'])
        cmd([DOCKER,'stop',MYSQL,REDIS])
