#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""Merge 2026 Henan history admission guide rows into static prediction JSON."""

from __future__ import annotations

import csv
import json
import re
from collections import defaultdict
from pathlib import Path
from statistics import median

ROOT = Path(__file__).resolve().parents[1]
SOURCE_TSV = ROOT / "tools" / "henan_history_zsy_2026_import.tsv"
STATIC_JSON_PATHS = [
    ROOT / "assets" / "prediction-lines.json",
    ROOT / "docs" / "assets" / "prediction-lines.json",
]

HENAN = "\u6cb3\u5357"
HISTORY = "\u5386\u53f2"
UNDERGRAD = "\u672c\u79d1"
JUNIOR = "\u4e13\u79d1"
UNKNOWN = "\u672a\u8bc6\u522b"
PLAN_SOURCE_LABEL = "2026\u62db\u751f\u4e4b\u53cb"


def clean(value):
    return str(value or "").strip()


def major_parts(value):
    parts = []
    for raw in re.split(r"[\u3001,，;；]", clean(value)):
        text = raw.strip()
        text = re.sub(r"^\d{2}", "", text)
        text = re.sub(r"\(\d+\u4eba\)$", "", text)
        text = re.sub(r"\uff08\d+\u4eba\uff09$", "", text)
        text = text.strip("()（）;； ")
        if len(text) >= 2 and text not in parts:
            parts.append(text)
    return parts


def group_full(group, elective):
    group = clean(group)
    elective = clean(elective)
    if not group:
        return ""
    return f"{group}\uff08{elective}\uff09" if elective else f"{group}\u7ec4"


def row_key_by_code(row):
    return (clean(row.get("subjectType")), clean(row.get("schoolCode")), clean(row.get("majorGroup")))


def row_key_by_name(row):
    return (clean(row.get("subjectType")), clean(row.get("schoolName")), clean(row.get("majorGroup")))


def source_key_by_code(row):
    return (HISTORY, clean(row.get("school_code")), clean(row.get("major_group")))


def source_key_by_name(row):
    return (HISTORY, clean(row.get("school_name")), clean(row.get("major_group")))


def score_value(row):
    for key in ["predictScore", "filingScore"]:
        try:
            value = int(row.get(key) or 0)
        except Exception:
            value = 0
        if value > 0:
            return value
    return 0


def estimate_adjustment(source_row):
    group = clean(source_row.get("major_group"))
    elective = clean(source_row.get("elective_subject"))
    majors = clean(source_row.get("majors"))
    delta = 0
    if group.startswith("2"):
        delta -= 8
    if group.startswith("8"):
        delta -= 30
    if elective in ["\u5316\u5b66", "\u751f\u7269"]:
        delta += 4
    if elective == "\u601d\u60f3\u653f\u6cbb":
        delta += 2
    if any(word in majors for word in ["\u6cd5\u5b66", "\u6c49\u8bed\u8a00", "\u5e08\u8303", "\u7ecf\u6d4e", "\u8d22\u52a1", "\u65b0\u95fb"]):
        delta += 2
    if "\u4e2d\u5916\u5408\u4f5c" in majors or "\u9884\u79d1" in majors:
        delta -= 8
    return delta


def update_prediction_row(target, source_row, confidence_suffix):
    majors = major_parts(source_row.get("majors"))
    target["province"] = HENAN
    target["subjectType"] = HISTORY
    target["schoolCode"] = clean(source_row.get("school_code")) or target.get("schoolCode")
    target["schoolName"] = clean(source_row.get("school_name")) or target.get("schoolName")
    target["majorGroup"] = clean(source_row.get("major_group")) or target.get("majorGroup")
    target["majorGroupFull"] = group_full(source_row.get("major_group"), source_row.get("elective_subject")) or target.get("majorGroupFull")
    target["planCount"] = int(source_row.get("plan_count") or 0) or target.get("planCount")
    if majors:
        target["majorDirection"] = "\u3001".join(majors)
        target["majorCategory"] = majors[0]
    target["schoolLevel"] = UNDERGRAD if "\u672c\u79d1" in clean(source_row.get("batch_name")) else target.get("schoolLevel") or JUNIOR
    target["sourcePlanYear"] = 2026
    target["sourcePlanFile"] = clean(source_row.get("source_file"))
    target["confidence"] = confidence_suffix
    return target


def build_estimated_row(template_rows, source_row, next_id):
    majors = major_parts(source_row.get("majors"))
    if not template_rows:
        return None
    template = sorted(template_rows, key=lambda item: score_value(item))[len(template_rows) // 2]
    scores = [score_value(item) for item in template_rows if score_value(item) > 0]
    if not scores:
        return None
    predicted = int(round(median(scores) + estimate_adjustment(source_row)))
    predicted = max(459, min(750, predicted))
    row = dict(template)
    row.update({
        "id": next_id,
        "predictYear": 2026,
        "province": HENAN,
        "schoolCode": clean(source_row.get("school_code")) or template.get("schoolCode"),
        "schoolName": clean(source_row.get("school_name")) or template.get("schoolName"),
        "subjectType": HISTORY,
        "majorGroup": clean(source_row.get("major_group")),
        "majorGroupFull": group_full(source_row.get("major_group"), source_row.get("elective_subject")),
        "schoolLevel": UNDERGRAD,
        "planCount": int(source_row.get("plan_count") or 0) or None,
        "filingScore": None,
        "filingRank": None,
        "rangeFloat": None,
        "predictScore": predicted,
        "predictLow": max(0, predicted - 6),
        "predictHigh": min(750, predicted + 6),
        "predictRange": f"{max(0, predicted - 6)}-{min(750, predicted + 6)}",
        "majorDirection": "\u3001".join(majors),
        "majorCategory": majors[0] if majors else UNKNOWN,
        "confidence": "\u6309\u540c\u68212025\u6295\u6863\u7ebf+\u62db\u751f\u4e4b\u53cb2026\u8ba1\u5212\u4f30\u7b97",
        "sourcePlanYear": 2026,
        "sourcePlanFile": clean(source_row.get("source_file")),
    })
    return row


def merge_one(path):
    rows = json.loads(path.read_text(encoding="utf-8"))
    guide_rows = list(csv.DictReader(SOURCE_TSV.open(encoding="utf-8"), delimiter="\t"))
    guide_rows = [row for row in guide_rows if clean(row.get("major_group"))]

    by_code_group = {}
    by_name_group = {}
    by_school = defaultdict(list)
    by_code = defaultdict(list)
    for row in rows:
        if clean(row.get("subjectType")) != HISTORY:
            continue
        by_code_group[row_key_by_code(row)] = row
        by_name_group[row_key_by_name(row)] = row
        by_school[clean(row.get("schoolName"))].append(row)
        by_code[clean(row.get("schoolCode"))].append(row)

    updated = 0
    inserted = 0
    skipped = 0
    next_id = max((int(row.get("id") or 0) for row in rows), default=0) + 1

    for guide in guide_rows:
        target = by_code_group.get(source_key_by_code(guide)) or by_name_group.get(source_key_by_name(guide))
        if target:
            update_prediction_row(target, guide, "\u63092025\u6295\u6863\u7ebf+\u62db\u751f\u4e4b\u53cb2026\u8ba1\u5212")
            updated += 1
            continue

        templates = by_school.get(clean(guide.get("school_name"))) or by_code.get(clean(guide.get("school_code")))
        estimated = build_estimated_row(templates, guide, next_id)
        if estimated:
            rows.append(estimated)
            by_code_group[row_key_by_code(estimated)] = estimated
            by_name_group[row_key_by_name(estimated)] = estimated
            by_school[clean(estimated.get("schoolName"))].append(estimated)
            by_code[clean(estimated.get("schoolCode"))].append(estimated)
            next_id += 1
            inserted += 1
        else:
            skipped += 1

    rows.sort(key=lambda row: (clean(row.get("subjectType")), int(row.get("predictScore") or 0), clean(row.get("schoolName")), clean(row.get("majorGroup"))))
    path.write_text(json.dumps(rows, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    return {"path": str(path), "updated": updated, "inserted": inserted, "skipped": skipped, "total": len(rows)}


def main():
    if not SOURCE_TSV.exists():
        raise SystemExit(f"Missing source TSV: {SOURCE_TSV}")
    stats = [merge_one(path) for path in STATIC_JSON_PATHS]
    print(json.dumps(stats, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
