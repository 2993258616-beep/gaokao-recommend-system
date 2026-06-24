import json
import re
from collections import Counter, defaultdict
from pathlib import Path


DATA_FILE = Path("assets/prediction-lines.json")
DOCS_DATA_FILE = Path("docs/assets/prediction-lines.json")
REPORT_FILE = Path("docs/data-audit-report.md")

KNOWN_TYPO_FRAGMENTS = [
    "安微",
    "数宇",
    "因艺",
    "政装",
    "客送",
    "廉复",
    "扩理学",
    "药物制刺",
    "Teear",
]

VOCATIONAL_WORDS = ("职业技术学院", "职业学院", "高等专科学校", "专科学校")
VOCATIONAL_UNIVERSITY_WORDS = ("职业技术大学", "职业大学")


def load_rows(path):
    return json.loads(path.read_text(encoding="utf-8"))


def norm_school_name(name):
    name = str(name or "").strip(" ,，:：.。·")
    name = name.replace("(", "（").replace(")", "）")
    name = re.sub(r"）+$", "）", name)
    for suffix in ("（民办）", "（独立学院）", "（中外合作办学）"):
        name = name.replace(suffix, "")
    return name


def should_be_junior_level(name):
    comparable = re.sub(r"[（(]原.*?[）)]", "", name or "")
    if any(word in comparable for word in VOCATIONAL_UNIVERSITY_WORDS):
        return False
    return any(word in comparable for word in VOCATIONAL_WORDS)


def collect_issues(rows):
    issues = defaultdict(list)
    for row in rows:
        text = " | ".join(str(row.get(key) or "") for key in ("schoolName", "majorGroupFull", "majorDirection", "majorCategory"))
        code = str(row.get("schoolCode") or "")
        school_name = str(row.get("schoolName") or "")
        rank = row.get("filingRank")
        score = row.get("filingScore")

        if not re.fullmatch(r"\d{4}", code):
            issues["invalid_code"].append(row)
        if row.get("schoolProvince") in ("", None, "未识别"):
            issues["unknown_school_province"].append(row)
        if re.search(r"[A-Za-z]{2,}", school_name):
            issues["ascii_in_school_name"].append(row)
        if school_name != school_name.strip(" ,，:：.。·"):
            issues["punctuation_in_school_name"].append(row)
        if any(fragment in text for fragment in KNOWN_TYPO_FRAGMENTS) or re.search(r"(?<!生)物制药技术", text):
            issues["known_typo_fragments"].append(row)
        if isinstance(rank, int) and (rank > 1_000_000 or (rank < 1000 and isinstance(score, int) and score < 600)):
            issues["suspicious_rank"].append(row)
        if row.get("schoolLevel") == "本科" and should_be_junior_level(school_name):
            issues["vocational_marked_undergrad"].append(row)
        if not re.fullmatch(r"\d{3}", str(row.get("majorGroup") or "")):
            issues["bad_major_group"].append(row)
        if row.get("majorGroupFull") and not re.match(r"^\d{3}[（(].*[）)]$", str(row.get("majorGroupFull"))):
            issues["bad_major_group_full"].append(row)
        if any(isinstance(row.get(key), int) and not 0 <= row.get(key) <= 750 for key in ("filingScore", "predictScore", "predictLow", "predictHigh")):
            issues["score_out_of_range"].append(row)
        if isinstance(row.get("predictLow"), int) and isinstance(row.get("predictHigh"), int) and row["predictLow"] > row["predictHigh"]:
            issues["bad_predict_range"].append(row)

    by_code_subject_group = defaultdict(list)
    by_code_subject = defaultdict(list)
    for row in rows:
        by_code_subject_group[(row.get("schoolCode"), row.get("subjectType"), row.get("majorGroup"))].append(row)
        by_code_subject[(row.get("schoolCode"), row.get("subjectType"))].append(row)

    serious_group_conflicts = []
    for key, grouped_rows in by_code_subject_group.items():
        names = defaultdict(list)
        for row in grouped_rows:
            names[norm_school_name(row.get("schoolName"))].append(row)
        if len(names) > 1:
            serious_group_conflicts.append((key, names))

    subject_conflicts = []
    for key, grouped_rows in by_code_subject.items():
        names = defaultdict(list)
        for row in grouped_rows:
            names[norm_school_name(row.get("schoolName"))].append(row)
        if len(names) > 1:
            subject_conflicts.append((key, names))

    return issues, serious_group_conflicts, subject_conflicts


def row_summary(row):
    return (
        f"id={row.get('id')} code={row.get('schoolCode')} "
        f"{row.get('schoolName')} {row.get('subjectType')}组{row.get('majorGroup')} "
        f"{row.get('schoolLevel')} score={row.get('filingScore')} rank={row.get('filingRank')}"
    )


def conflict_summary(conflict):
    key, names = conflict
    parts = []
    for name, rows in names.items():
        sample = rows[0]
        parts.append(f"{name}(id={sample.get('id')}, score={sample.get('filingScore')})")
    return f"{key}: " + " / ".join(parts)


def build_report(rows, docs_equal, issues, serious_group_conflicts, subject_conflicts):
    lines = [
        "# 数据审计报告",
        "",
        "- 数据文件：`assets/prediction-lines.json`",
        f"- 总行数：{len(rows)}",
        f"- 与 `docs/assets/prediction-lines.json` 一致：{'是' if docs_equal else '否'}",
        f"- 科类分布：{dict(Counter(row.get('subjectType') for row in rows))}",
        f"- 学校地区数：{len(set(row.get('schoolProvince') for row in rows))}",
        "",
        "## 剩余问题计数",
        "",
    ]
    for key in sorted(issues):
        lines.append(f"- {key}: {len(issues[key])}")
    lines.extend([
        f"- same_code_subject_group_conflicts: {len(serious_group_conflicts)}",
        f"- same_code_subject_conflicts: {len(subject_conflicts)}",
        "",
        "## 需回源核验的代码/名称冲突",
        "",
        "### 同一院校代码+科类+专业组",
        "",
    ])
    if serious_group_conflicts:
        for conflict in serious_group_conflicts[:80]:
            lines.append(f"- {conflict_summary(conflict)}")
    else:
        lines.append("- 暂无同一院校代码+科类+专业组对应不同院校名的问题。")

    lines.extend([
        "",
        "### 同一院校代码+科类（跨专业组）",
        "",
    ])
    if subject_conflicts:
        for conflict in subject_conflicts[:100]:
            lines.append(f"- {conflict_summary(conflict)}")
    else:
        lines.append("- 暂无同一院校代码+科类对应不同院校名的问题。")

    lines.extend(["", "## 样例问题", ""])
    for key in sorted(issues):
        if not issues[key]:
            continue
        lines.append(f"### {key}")
        for row in issues[key][:20]:
            lines.append(f"- {row_summary(row)}")
        lines.append("")

    return "\n".join(lines).rstrip() + "\n"


def main():
    rows = load_rows(DATA_FILE)
    docs_equal = DATA_FILE.read_bytes() == DOCS_DATA_FILE.read_bytes()
    issues, serious_group_conflicts, subject_conflicts = collect_issues(rows)
    report = build_report(rows, docs_equal, issues, serious_group_conflicts, subject_conflicts)
    REPORT_FILE.write_text(report, encoding="utf-8")

    print(f"rows={len(rows)} docs_equal={docs_equal}")
    for key in sorted(issues):
        print(f"{key}={len(issues[key])}")
    print(f"same_code_subject_group_conflicts={len(serious_group_conflicts)}")
    print(f"same_code_subject_conflicts={len(subject_conflicts)}")
    print(f"report={REPORT_FILE}")


if __name__ == "__main__":
    main()
