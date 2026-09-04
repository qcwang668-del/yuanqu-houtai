#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
力企企业库 · 融资信息导入脚本

数据源：投融资.csv（GB2312 编码）
关联口径：**按企业名称（entity_name）匹配 liqi_enterprise.enterprise_name**
    注意：CSV 的 entity_id 是外部平台的纯数字 ID，与本库 liqi_enterprise.entity_id
    （统一社会信用代码）不是同一套编码体系，实测零命中，故改用企业名称匹配。
    CSV 的 entity_id 仅作溯源冗余存储。

用法：
    python enterprise-financing-import.py \
        --csv "投融资.csv" --host 127.0.0.1 --port 3306 \
        --user root --pass "" --db ruoyi-vue-pro
"""

import argparse
import csv
import io
from datetime import datetime
from decimal import Decimal, InvalidOperation

import pymysql

# 视为「金额未公开」的文本，统一存 NULL 数值
UNDISCLOSED = {"未披露", "未透露", "未公布", "不明确", "保密", "未披露金额"}


def parse_date(val):
    """dd/MM/yyyy HH:mm:ss → datetime；无法解析返回 None"""
    s = (val or "").strip()
    if not s:
        return None
    for fmt in ("%d/%m/%Y %H:%M:%S", "%d/%m/%Y"):
        try:
            return datetime.strptime(s, fmt)
        except ValueError:
            continue
    return None


def parse_amt_num(val):
    """纯数字金额 → Decimal；中文描述/未披露 → None（原文仍存 rz_amt）"""
    s = (val or "").strip().replace(",", "")
    if not s or s in UNDISCLOSED:
        return None
    try:
        return Decimal(s)
    except InvalidOperation:
        return None


def main():
    ap = argparse.ArgumentParser(description="导入融资 CSV 到企业库")
    ap.add_argument("--csv", required=True, help="投融资.csv 路径")
    ap.add_argument("--host", default="127.0.0.1")
    ap.add_argument("--port", type=int, default=3306)
    ap.add_argument("--user", required=True)
    ap.add_argument("--pass", dest="password", default="")
    ap.add_argument("--db", default="ruoyi-vue-pro")
    ap.add_argument("--encoding", default="gb2312", help="CSV 编码，默认 gb2312")
    ap.add_argument("--truncate", action="store_true", help="导入前清空表（重跑用）")
    args = ap.parse_args()

    # 1) 读 CSV（GB2312）
    print(f"[1/4] 读取 CSV：{args.csv}（encoding={args.encoding}）")
    with io.open(args.csv, "r", encoding=args.encoding, errors="replace", newline="") as f:
        rows = list(csv.DictReader(f))
    print(f"      共 {len(rows)} 条记录")

    conn = pymysql.connect(
        host=args.host, port=args.port, user=args.user, password=args.password,
        db=args.db, charset="utf8mb4", cursorclass=pymysql.cursors.DictCursor,
    )
    try:
        cur = conn.cursor()

        # 2) 企业名称 → (id, tenant_id) 映射；tenant_id 必须随企业，否则多租户过滤查不到
        print("[2/4] 加载企业库名称映射")
        cur.execute("SELECT id, tenant_id, enterprise_name FROM liqi_enterprise")
        name_map = {}
        for r in cur.fetchall():
            nm = (r["enterprise_name"] or "").strip()
            if nm:
                name_map[nm] = (r["id"], r["tenant_id"])
        print(f"      已加载 {len(name_map)} 家企业")

        if args.truncate:
            cur.execute("TRUNCATE TABLE liqi_enterprise_financing")
            print("      已清空 liqi_enterprise_financing")

        # 3) 组装并批量写入
        print("[3/4] 写入融资记录")
        sql = (
            "INSERT INTO liqi_enterprise_financing "
            "(enterprise_id, entity_id, source_id, rz_time, rz_amt, rz_amt_num, "
            " rz_round, investor_info, tenant_id) "
            "VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s)"
        )
        batch, inserted, skipped = [], 0, 0
        for r in rows:
            nm = (r.get("entity_name") or "").strip()
            hit = name_map.get(nm)
            if not hit:
                skipped += 1
                continue
            ent_id, tenant_id = hit

            src = (r.get("id") or "").strip()
            amt = (r.get("rz_amt") or "").strip() or None
            inv = (r.get("investor_info") or "").strip()
            batch.append((
                ent_id,
                (r.get("entity_id") or "").strip() or None,
                int(src) if src.isdigit() else None,
                parse_date(r.get("rz_time")),
                amt,
                parse_amt_num(amt),
                (r.get("rz_round") or "").strip() or None,
                inv if inv and inv.lower() != "null" else None,
                tenant_id,
            ))
            inserted += 1
            if len(batch) >= 200:
                cur.executemany(sql, batch)
                batch = []
        if batch:
            cur.executemany(sql, batch)
        conn.commit()

        # 4) 校验
        print("[4/4] 校验")
        cur.execute("SELECT COUNT(*) c FROM liqi_enterprise_financing")
        total = cur.fetchone()["c"]
        cur.execute("SELECT COUNT(DISTINCT enterprise_id) c FROM liqi_enterprise_financing")
        ents = cur.fetchone()["c"]
        print(f"\n完成：写入 {inserted} 条，跳过（企业未匹配）{skipped} 条")
        print(f"      表内合计 {total} 条，覆盖 {ents} 家企业")
    finally:
        conn.close()


if __name__ == "__main__":
    main()