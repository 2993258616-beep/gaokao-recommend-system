import json
import re
from collections import Counter
from pathlib import Path


FILES = [
    Path("assets/prediction-lines.json"),
    Path("docs/assets/prediction-lines.json"),
]

TEXT_REPLACEMENTS = [
    ("安微", "安徽"),
    ("数宇", "数字"),
    ("因艺", "园艺"),
    ("因林", "园林"),
    ("政装", "改装"),
    ("客送", "客运"),
    ("安金技术管理", "安全技术管理"),
    ("电于商务", "电子商务"),
    ("电予商务", "电子商务"),
    ("款件技术", "软件技术"),
    ("应急救接技术", "应急救援技术"),
    ("应急教接技术", "应急救援技术"),
    ("宠物养护与别导", "宠物养护与驯导"),
    ("风景国林设计", "风景园林设计"),
    ("智慧域市管理技术", "智慧城市管理技术"),
    ("域市轨道交通机电技术", "城市轨道交通机电技术"),
    ("城市轨道交递运营管理", "城市轨道交通运营管理"),
    ("市致工程技术", "市政工程技术"),
    ("富牧兽医", "畜牧兽医"),
    ("药物制刺", "药物制剂"),
    ("廉复治疗学", "康复治疗学"),
    ("廉复洁疗学", "康复治疗学"),
    ("廉复治疗技术", "康复治疗技术"),
    ("廉复溶疗技术", "康复治疗技术"),
    ("廉复治疗被术", "康复治疗技术"),
    ("廉复作业治疗", "康复作业治疗"),
    ("廉复治疗", "康复治疗"),
    ("廉复学", "康复学"),
    ("廉复", "康复"),
    ("扩理学", "护理学"),
    ("扩理", "护理"),
    ("旋复治疗学", "康复治疗学"),
    ("运动廉复", "运动康复"),
    ("听力与宫语康复学", "听力与言语康复学"),
    ("民航安全技术营理", "民航安全技术管理"),
    ("医学检验技、术", "医学检验技术"),
    ("数据科学与大数据技、术", "数据科学与大数据技术"),
    ("挂术", "技术"),
    ("枝术", "技术"),
    ("植术", "技术"),
    ("拉术", "技术"),
    ("桂术", "技术"),
    ("设术", "技术"),
    ("梳术", "技术"),
    ("较件技术", "软件技术"),
    ("光人机应用技术", "无人机应用技术"),
    ("中草药载培", "中草药栽培"),
    ("药物制剂技术", "药物制剂技术"),
    ("薪品经营", "药品经营"),
    ("口腔医学拉术", "口腔医学技术"),
    ("医学是容", "医学美容"),
    ("医学美客", "医学美容"),
    ("药品质量与安金", "药品质量与安全"),
    ("药品质童与安全", "药品质量与安全"),
    ("保能", "保健"),
    ("健廉管理", "健康管理"),
    ("智葱健康养老", "智慧健康养老"),
    ("智运健康养老", "智慧健康养老"),
    ("智悲健康养老", "智慧健康养老"),
    ("网络营悄", "网络营销"),
    ("网络营棘", "网络营销"),
    ("直电商", "直播电商"),
    ("现代衣业", "现代农业"),
    ("机城制速", "机械制造"),
    ("机植制造", "机械制造"),
    ("机它一体化", "机电一体化"),
    ("贪品智能加工", "食品智能加工"),
    ("含品检验检测", "食品检验检测"),
    ("毅字媒体", "数字媒体"),
    ("高连铁路", "高速铁路"),
    ("新能源材料应用术", "新能源材料应用技术"),
    ("材料循环应用技术", "材料循环应用技术"),
    ("智能制迎装备", "智能制造装备"),
    ("医学影缘技术", "医学影像技术"),
    ("医季生物技术", "医学生物技术"),
    ("医季营养", "医学营养"),
    ("剐药造备", "制药设备"),
    ("剖药设备", "制药设备"),
    ("中医骨協", "中医骨伤"),
    ("针炎推拿", "针灸推拿"),
    ("开封智健康职业学院", "开封智慧健康职业学院"),
    ("关郑州信息工程职业学院", "郑州信息工程职业学院"),
    ("郑州升达经贸蓄理学院", "郑州升达经贸管理学院"),
    ("大连科按学院", "大连科技学院"),
    ("右河子大学", "石河子大学"),
    ("月成都外国语学院", "成都外国语学院"),
    ("阔江师范高等专科学校", "闽江师范高等专科学校"),
    ("毫州学院", "亳州学院"),
    ("北京语离大学", "北京语言大学"),
    ("L山东石油化工学院", "山东石油化工学院"),
    ("北京航空航关大学", "北京航空航天大学"),
    ("陕西脏装工程学院", "陕西服装工程学院"),
    ("湖北空通职业技术学院", "湖北交通职业技术学院"),
    ("山东信息职业技术学院机电一体化技术,智能控制技术,空中乘务,城市轨道交通运营管理,数字媒体技术,虚", "山东信息职业技术学院"),
    ("河南牧业经济学院（（软件类", "河南牧业经济学院（软件类"),
    ("厘门工学院（民办", "厦门工学院（民办"),
    ("厘门工学院(民办", "厦门工学院(民办"),
    ("厦门工学院（民办", "厦门工学院（民办）"),
    ("厦门工学院(民办", "厦门工学院(民办)"),
    ("三峡大学科技学院（独立学院;", "三峡大学科技学院（独立学院）"),
    ("三峡大学科技学院(独立学院;", "三峡大学科技学院(独立学院)"),
    ("河南牧业经济学院卡洛理工国际学院02软件工程（嵌入式开", "河南牧业经济学院卡洛理工国际学院"),
    ("断乡学院", "新乡学院"),
    ("桂林医科大学（原棒林医学院）", "桂林医科大学（原桂林医学院）"),
    ("桂林医科大学(原棒林医学院)", "桂林医科大学(原桂林医学院)"),
    ("桂林医科大学（原桂林医学院", "桂林医科大学（原桂林医学院）"),
    ("桂林医科大学(原桂林医学院", "桂林医科大学(原桂林医学院)"),
    ("威阳师范学院", "咸阳师范学院"),
    ("哈尔澳理工大学", "哈尔滨理工大学"),
    ("江苏大学京江学院（独立学院）", "江苏大学京江学院"),
    ("浙疆第二医学院", "新疆第二医学院"),
    ("西安航空学统", "西安航空学院"),
    ("内票古样技大学包头师范学院", "内蒙古科技大学包头师范学院"),
    ("浙江外国谱学院", "浙江外国语学院"),
    ("贵阳廉养职业大学", "贵阳康养职业大学"),
    ("新矗财经大学", "新疆财经大学"),
    ("长眷工程学院", "长春工程学院"),
    ("街阳师范学院", "衡阳师范学院"),
    ("太原工业攀院", "太原工业学院"),
    ("河南工程学殡", "河南工程学院"),
    ("宁波诺丁汉太学（中外合作办学）", "宁波诺丁汉大学（中外合作办学）"),
    ("宁波诺丁汉太学(中外合作办学)", "宁波诺丁汉大学(中外合作办学)"),
    ("两南财经政法大学", "河南财经政法大学"),
    ("大连海详大学", "大连海洋大学"),
    ("曲卑师范大学", "曲阜师范大学"),
    ("上海应用技术大掌", "上海应用技术大学"),
    ("中置矿业大学", "中国矿业大学"),
    ("北京遭筑大学", "北京建筑大学"),
    ("宁戛大学", "宁夏大学"),
    ("中国政法大学10理科试验班类（中法", "中国政法大学"),
    ("西安交通大学学位项目", "西安交通大学"),
    ("哈尔滨工业大学（威海）双学士学位", "哈尔滨工业大学（威海）"),
    ("哈尔滨工业大学（威海", "哈尔滨工业大学（威海）"),
    ("哈尔滨工业大学（威海））", "哈尔滨工业大学（威海）"),
    ("湛江科技学院（民办", "湛江科技学院（民办）"),
    ("湛江科技学院(民办", "湛江科技学院(民办)"),
    ("Teear", ""),
]

SCHOOL_NAME_FIXES = {
    "天津中医药大学02中医学（5+3一体化": "天津中医药大学",
    "湖南城市学院17微电子科学与工程": "湖南城市学院",
    "伊犁师范大学07国际经济与贸易": "伊犁师范大学",
    "郑州大学(中外合作办学)47临床医学（5+3一体": "郑州大学(中外合作办学)",
    "郑州大学(中外合作办学": "郑州大学(中外合作办学)",
    "\"重庆电子科技职业大学\"\"意\"": "重庆电子科技职业大学",
    "沈阳大学互认课程项目）": "沈阳大学",
    "石河子大学学双学上学位项目）": "石河子大学",
    "陕西农林职业技术大学（原杨凌职业技术学院": "陕西农林职业技术大学（原杨凌职业技术学院）",
    "郑州健康学院（原郑州澍青医学高等专科学校": "郑州健康学院（原郑州澍青医学高等专科学校）",
}

CODE_FIXES = {
    ("贵州医科大学", "5E80"): "5180",
    ("云南商务职业学院", "6088"): "8809",
    ("四川城市职业学院", "6698"): "8699",
    ("重庆海联职业技术学院", "6606"): "9099",
    ("湖南工业职业技术学院", "9133"): "8913",
    ("阿克苏职业技术学院", "7155"): "9715",
    ("广州南洋理工职业学院", "8544"): "9854",
    ("云南农业大学", "2355"): "5235",
}

PROVINCE_FIXES = {
    "硅湖职业技术学院": "江苏",
}

VOCATIONAL_WORDS = ("职业技术学院", "职业学院", "高等专科学校", "专科学校")
VOCATIONAL_UNIVERSITY_WORDS = ("职业技术大学", "职业大学")


def normalize_known_parentheses(text):
    replacements = [
        (r"厦门工学院（民办）+", "厦门工学院（民办）"),
        (r"厦门工学院\(民办\)+", "厦门工学院(民办)"),
        (r"湛江科技学院（民办）+", "湛江科技学院（民办）"),
        (r"湛江科技学院\(民办\)+", "湛江科技学院(民办)"),
        (r"桂林医科大学（原桂林医学院）+", "桂林医科大学（原桂林医学院）"),
        (r"桂林医科大学\(原桂林医学院\)+", "桂林医科大学(原桂林医学院)"),
    ]
    for pattern, replacement in replacements:
        text = re.sub(pattern, replacement, text)
    return text


def clean_text(value):
    if not isinstance(value, str):
        return value
    text = value
    for bad, good in TEXT_REPLACEMENTS:
        text = text.replace(bad, good)
    marker = "__BIOPHARMA__"
    text = text.replace("生物制药技术", marker)
    text = text.replace("物制药技术", "生物制药技术")
    text = text.replace(marker, "生物制药技术")
    marker = "__BIOMED__"
    text = text.replace("生物医学工程", marker)
    text = text.replace("物医学工程", "生物医学工程")
    text = text.replace(marker, "生物医学工程")
    while "生生物医学工程" in text:
        text = text.replace("生生物医学工程", "生物医学工程")
    marker = "__BIODATA__"
    text = text.replace("生物医药数据科学", marker)
    text = text.replace("物医药敷据科学", "生物医药数据科学")
    text = text.replace(marker, "生物医药数据科学")
    while "生生物医药数据科学" in text:
        text = text.replace("生生物医药数据科学", "生物医药数据科学")
    if text.startswith("物技术(中外高水平大学学生交流计划"):
        text = "生" + text
    text = normalize_known_parentheses(text)
    return text.strip(" ,，:：.。·")


def clean_major_group_full(value):
    value = clean_text(value)
    if not isinstance(value, str):
        return value
    match = re.fullmatch(r"(\d{3})普通组", value)
    if match:
        return f"{match.group(1)}（普通组）"
    return value


def clean_school_name(name):
    name = clean_text(name)
    return SCHOOL_NAME_FIXES.get(name, name)


def should_be_junior_level(name):
    if "联办" in (name or "") or "联合办学" in (name or ""):
        return False
    comparable = re.sub(r"[（(]原.*?[）)]", "", name or "")
    if any(word in comparable for word in VOCATIONAL_UNIVERSITY_WORDS):
        return False
    return any(word in comparable for word in VOCATIONAL_WORDS)


def repair_file(file_path):
    rows = json.loads(file_path.read_text(encoding="utf-8"))
    counts = Counter()
    for row in rows:
        before = dict(row)
        for key in ("schoolName", "majorGroupFull", "majorDirection", "majorCategory"):
            if key == "schoolName":
                row[key] = clean_school_name(row.get(key))
            elif key == "majorGroupFull":
                row[key] = clean_major_group_full(row.get(key))
            else:
                row[key] = clean_text(row.get(key))

        for school, province in PROVINCE_FIXES.items():
            if row.get("schoolName") == school:
                row["schoolProvince"] = province

        code_key = (row.get("schoolName"), str(row.get("schoolCode") or ""))
        if code_key in CODE_FIXES:
            row["schoolCode"] = CODE_FIXES[code_key]

        if row.get("schoolLevel") == "本科" and should_be_junior_level(row.get("schoolName")):
            row["schoolLevel"] = "专科"

        rank = row.get("filingRank")
        score = row.get("filingScore")
        if isinstance(rank, int) and (rank > 1_000_000 or (rank < 1000 and isinstance(score, int) and score < 600)):
            row["filingRank"] = None

        for key, value in row.items():
            if before.get(key) != value:
                counts[key] += 1

    file_path.write_text(json.dumps(rows, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    return counts


def main():
    for file_path in FILES:
        counts = repair_file(file_path)
        print(f"{file_path}: {dict(counts)}")


if __name__ == "__main__":
    main()
