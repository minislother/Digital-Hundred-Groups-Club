"""Real MySQL integration checks for the registration migration and concurrent writes.

Only creates new codex_registration_* databases in an explicitly named test container.
Never reads business rows or deletes databases. Requires Docker and MySQL 8.x.
"""
import argparse, concurrent.futures, json, os, re, shutil, subprocess, threading, uuid
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--container',required=True)
    args=parser.parse_args()
    if not args.container.startswith('codex-'): raise SystemExit('Use an isolated codex-* test container')
    docker=shutil.which('docker') or r'C:\Program Files\Docker\Docker\resources\bin\docker.exe'
    password=os.environ['MYSQL_TEST_PASSWORD']
    prefix='codex_registration_'+uuid.uuid4().hex[:12]
    def sql(statement,database=None,success=True):
        command=[docker,'exec','-i','-e','MYSQL_PWD='+password,args.container,
                 'mysql','-uroot','--default-character-set=utf8mb4','--batch','--skip-column-names']
        if database: command.append(database)
        run=subprocess.run(command,input=statement.encode('utf8'),capture_output=True)
        if success and run.returncode: raise AssertionError(run.stderr.decode('utf8',errors='replace'))
        return run
    def value(statement,database): return sql(statement,database).stdout.decode('utf8').strip()
    migration=(ROOT/'src/main/resources/db/migration/activity-registration-unique.sql').read_text(encoding='utf8')
    dump=(ROOT.parent/'club_finalversion.sql').read_text(encoding='utf8')
    fresh=re.search(r'CREATE TABLE `individual_activity` \(.*?;',dump,re.S).group()
    old="""CREATE TABLE individual_activity (
        id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, activity_id INT NULL,
        user_id VARCHAR(100) COLLATE utf8mb4_0900_ai_ci NULL, status INT NULL
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"""
    results=[]
    def database(suffix,schema):
        name=prefix+'_'+suffix;sql('CREATE DATABASE '+name+' CHARACTER SET utf8mb4');sql(schema,name);return name
    clean=database('clean',old)
    sql("INSERT INTO individual_activity(activity_id,user_id,status) VALUES (1,'existing',-1)",clean)
    sql(migration,clean);sql(migration,clean)
    assert value('SELECT count(*) FROM individual_activity',clean)=='1'
    assert value("SELECT count(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='individual_activity' AND column_name IN ('activity_id','user_id') AND is_nullable='NO'",clean)=='2'
    results.append('legacy clean schema migrated twice; row preserved; both keys NOT NULL')
    # Same identity remains unique across approval states; re-approval updates the existing row.
    rejected=sql("INSERT INTO individual_activity(activity_id,user_id,status) VALUES(1,'existing',0)",clean,False)
    assert rejected.returncode and b'1062' in rejected.stderr
    sql("UPDATE individual_activity SET status=1 WHERE activity_id=1 AND user_id='existing'",clean)
    for fields in ["NULL,'missing',0","2,NULL,0"]:
        assert sql('INSERT INTO individual_activity(activity_id,user_id,status) VALUES('+fields+')',clean,False).returncode
    results.append('status=-1 cannot be reinserted; update supported; NULL keys rejected')
    # Concurrent independent database connections, deliberately bypassing Redis and application checks.
    barrier=threading.Barrier(16)
    def insert(_):
        barrier.wait()
        result=sql("INSERT INTO individual_activity(activity_id,user_id,status) VALUES(2,'race',0)",clean,False)
        return 0 if result.returncode==0 else (1062 if b'1062' in result.stderr else -1)
    with concurrent.futures.ThreadPoolExecutor(max_workers=16) as pool: codes=list(pool.map(insert,range(16)))
    assert codes.count(0)==1 and codes.count(1062)==15,codes
    assert value("SELECT count(*) FROM individual_activity WHERE activity_id=2 AND user_id='race'",clean)=='1'
    sql("INSERT INTO individual_activity(activity_id,user_id,status) VALUES(3,'race',0),(2,'another',0)",clean)
    results.append('16 concurrent writes: 1 committed, 15 duplicate errors; other pairs allowed')
    for suffix,seed,reason in [('duplicates',"(1,'same',0),(1,'same',1)",'duplicate'),
                               ('null',"(NULL,'null-row',0)",'null/blank'),
                               ('blank',"(1,'  ',0)",'null/blank')]:
        db=database(suffix,old)
        sql('INSERT INTO individual_activity(activity_id,user_id,status) VALUES '+seed,db)
        before=value('SELECT count(*) FROM individual_activity',db)
        refusal=sql(migration,db,False)
        assert refusal.returncode and reason.encode() in refusal.stderr
        assert before==value('SELECT count(*) FROM individual_activity',db)
        assert value("SELECT count(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='individual_activity' AND index_name='uk_individual_activity_activity_user'",db)=='0'
        results.append(suffix+': migration refused without deleting rows or adding constraint')
    wrong=database('wrongindex',old)
    sql('CREATE INDEX uk_individual_activity_activity_user ON individual_activity(user_id)',wrong)
    refusal=sql(migration,wrong,False)
    assert refusal.returncode and b'unexpected index' in refusal.stderr
    results.append('wrong same-name index refused')
    new=database('fresh',fresh)
    sql("INSERT INTO individual_activity(activity_id,user_id,status) VALUES(1,'fresh',0)",new)
    duplicate=sql("INSERT INTO individual_activity(activity_id,user_id,status) VALUES(1,'fresh',1)",new,False)
    assert duplicate.returncode and b'1062' in duplicate.stderr
    sql(migration,new)
    results.append('fresh dump schema enforces uniqueness and supports migration replay')
    output={'database_prefix':prefix,'mysql':value('SELECT VERSION()',clean),'checks':results,
            'concurrent_writers':16,'committed':1,'duplicate_rejected':15}
    target=ROOT/'target/registration-unique-integration.json'
    target.write_text(json.dumps(output,ensure_ascii=False,indent=2),encoding='utf8')
    print(json.dumps(output,ensure_ascii=False,indent=2))

if __name__=='__main__': main()
