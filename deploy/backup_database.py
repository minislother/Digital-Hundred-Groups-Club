"""备份 Compose MySQL；可在独立临时数据库验证恢复，不覆盖业务库。"""
import argparse
from pathlib import Path
import re
import subprocess
import uuid

ROOT = Path(__file__).resolve().parents[1]


def command(script, **kwargs):
    return subprocess.run(["docker", "compose", "-f", str(ROOT / "docker-compose.yml"),
                           "exec", "-T", "mysql", "sh", "-c", script],
                          cwd=ROOT, check=True, **kwargs)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output", type=Path, help="仓库外或 outputs/ 下的备份文件路径")
    parser.add_argument("--verify", action="store_true", help="在随机临时库恢复并核对表数量后删除临时库")
    args = parser.parse_args()
    if args.output.exists():
        raise SystemExit("目标文件已存在，请换一个文件名，避免覆盖已有备份")
    args.output.parent.mkdir(parents=True, exist_ok=True)
    # 使用远端环境传递凭据，密码不会出现在宿主命令行或脚本输出中。
    with args.output.open("xb") as output:
        command('MYSQL_PWD="$MYSQL_PASSWORD" exec mysqldump --user="$MYSQL_USER" '
                '--single-transaction --routines --triggers --no-tablespaces "$MYSQL_DATABASE"', stdout=output)
    if args.output.stat().st_size == 0:
        raise SystemExit("备份为空，不能继续恢复验证")
    if args.verify:
        database = "baituan_verify_" + uuid.uuid4().hex
        assert re.fullmatch(r"baituan_verify_[0-9a-f]{32}", database)
        prefix = 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --user=root '
        command(prefix + '-e "CREATE DATABASE ' + database + ' CHARACTER SET utf8mb4"')
        try:
            with args.output.open("rb") as source:
                command(prefix + database, stdin=source)
            count = command(prefix + '-N -s -e "SELECT COUNT(*) FROM information_schema.tables '
                            "WHERE table_schema='" + database + "'\"", capture_output=True, text=True)
            if int(count.stdout.strip()) <= 0:
                raise RuntimeError("恢复后的临时库没有数据表")
        finally:
            # 仅删除本次创建、固定前缀和随机ID组成的临时库，绝不接受业务库名作为删除目标。
            command(prefix + '-e "DROP DATABASE ' + database + '"')
    print("备份完成" + ("，临时库恢复验证通过" if args.verify else "；尚未执行恢复验证"))


if __name__ == "__main__":
    main()
