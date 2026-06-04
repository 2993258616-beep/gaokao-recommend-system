import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CleanBrokenMajorNames {
    private static final String URL = "jdbc:h2:file:./data/gaokao_recommend_db;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;AUTO_SERVER=TRUE";
    private static final String USER = "sa";

    private static final List<String> NOISE_WORDS = Arrays.asList(
            "未提供", "包含", "专业组", "再选科", "再选科目", "再选科自", "录取", "位次", "平均", "人数", "页",
            "专地机", "专亚型", "传业", "专业维", "专业级", "专业银", "专业镇", "专业硕士"
    );

    public static void main(String[] args) throws Exception {
        Class.forName("org.h2.Driver");
        Connection conn = DriverManager.getConnection(URL, USER, "");
        conn.setAutoCommit(false);
        try {
            String backup = "data/backup-before-major-name-clean-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".zip";
            Statement st = conn.createStatement();
            st.execute("BACKUP TO '" + backup.replace("\\", "/") + "'");

            Map<String, String> admissionMajors = loadAdmissionMajors(conn);
            int scanned = 0;
            int updated = 0;

            PreparedStatement select = conn.prepareStatement(
                    "SELECT id, school_name, subject_type, major_group, major_group_full, school_type, school_level, major_direction, major_category " +
                            "FROM prediction_line ORDER BY id"
            );
            PreparedStatement update = conn.prepareStatement(
                    "UPDATE prediction_line SET major_direction=?, major_category=? WHERE id=?"
            );
            ResultSet rs = select.executeQuery();
            while (rs.next()) {
                scanned++;
                long id = rs.getLong("id");
                String schoolName = nvl(rs.getString("school_name"));
                String subjectType = nvl(rs.getString("subject_type"));
                String majorGroup = nvl(rs.getString("major_group"));
                String groupFull = nvl(rs.getString("major_group_full"));
                String schoolType = nvl(rs.getString("school_type"));
                String schoolLevel = nvl(rs.getString("school_level"));
                String majorDirection = nvl(rs.getString("major_direction"));
                String majorCategory = nvl(rs.getString("major_category"));

                String context = schoolName + " " + groupFull + " " + schoolType + " " + schoolLevel + " " + majorCategory + " " + majorDirection;
                String source = firstNonBlank(majorDirection, majorCategory);
                boolean brokenBefore = isBrokenMajorList(source);

                String cleaned = cleanMajorList(source, context);
                String exactKey = key(schoolName, subjectType, majorGroup);
                String official = admissionMajors.get(exactKey);
                if (isBetterOfficial(official, cleaned, brokenBefore)) {
                    cleaned = official;
                }
                if (isBrokenMajorList(cleaned)) {
                    cleaned = fallbackMajors(context, schoolLevel);
                }

                String cleanedCategory = firstMajor(cleaned);
                boolean needsUpdate = brokenBefore
                        || !majorDirection.equals(cleaned)
                        || isBrokenCategory(majorCategory)
                        || !majorCategory.equals(cleanedCategory);
                if (needsUpdate && !cleaned.isEmpty()) {
                    update.setString(1, cleaned);
                    update.setString(2, cleanedCategory);
                    update.setLong(3, id);
                    update.addBatch();
                    updated++;
                }
            }
            update.executeBatch();
            int admissionUpdated = cleanAdmissionGroups(conn);
            conn.commit();
            System.out.println("BACKUP=" + backup);
            System.out.println("SCANNED=" + scanned);
            System.out.println("UPDATED=" + updated);
            System.out.println("ADMISSION_UPDATED=" + admissionUpdated);
        } catch (Exception ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.close();
        }
    }

    private static Map<String, String> loadAdmissionMajors(Connection conn) throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        PreparedStatement ps = conn.prepareStatement(
                "SELECT school_name, source_subject, subject_type, batch_name, major_group, majors FROM admission_major_group " +
                        "WHERE majors IS NOT NULL AND TRIM(majors)<>'' ORDER BY id"
        );
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            String schoolName = nvl(rs.getString("school_name"));
            String subject = firstNonBlank(rs.getString("source_subject"), rs.getString("subject_type"));
            String group = nvl(rs.getString("major_group"));
            String majors = cleanMajorList(rs.getString("majors"), schoolName + " " + subject + " " + nvl(rs.getString("batch_name")));
            if (!isBrokenMajorList(majors)) {
                map.put(key(schoolName, subject, group), majors);
            }
        }
        return map;
    }

    private static int cleanAdmissionGroups(Connection conn) throws Exception {
        int updated = 0;
        PreparedStatement select = conn.prepareStatement(
                "SELECT id, school_name, source_subject, subject_type, batch_name, majors FROM admission_major_group " +
                        "WHERE majors IS NOT NULL AND TRIM(majors)<>'' ORDER BY id"
        );
        PreparedStatement update = conn.prepareStatement(
                "UPDATE admission_major_group SET majors=?, major_count=? WHERE id=?"
        );
        ResultSet rs = select.executeQuery();
        while (rs.next()) {
            long id = rs.getLong("id");
            String context = nvl(rs.getString("school_name")) + " " +
                    firstNonBlank(rs.getString("source_subject"), rs.getString("subject_type")) + " " +
                    nvl(rs.getString("batch_name"));
            String oldMajors = nvl(rs.getString("majors"));
            String cleaned = cleanMajorList(oldMajors, context);
            if (cleaned.length() == 0 || isBrokenMajorList(cleaned)) {
                cleaned = fallbackMajors(context, "");
            }
            if (!cleaned.equals(oldMajors) && cleaned.length() > 0) {
                update.setString(1, cleaned);
                update.setInt(2, countMajors(cleaned));
                update.setLong(3, id);
                update.addBatch();
                updated++;
            }
        }
        update.executeBatch();
        return updated;
    }

    private static boolean isBetterOfficial(String official, String cleaned, boolean brokenBefore) {
        if (official == null || official.trim().isEmpty() || isBrokenMajorList(official)) {
            return false;
        }
        if (brokenBefore) {
            return true;
        }
        return countMajors(cleaned) < 2 && countMajors(official) >= 2;
    }

    private static String cleanMajorList(String value, String context) {
        if (value == null) {
            return "";
        }
        String normalized = value
                .replace('，', '、')
                .replace(',', '、')
                .replace(';', '、')
                .replace('；', '、')
                .replace("数据科学与大数据技、术", "数据科学与大数据技术")
                .replace("大数据技、术", "大数据技术")
                .replace("大数、据", "大数据")
                .replace("大数据与、会计", "大数据与会计")
                .replace("大数据、与会计", "大数据与会计")
                .replace("L大数据与财务管理", "大数据与财务管理")
                .replace("l大数据与财务管理", "大数据与财务管理")
                .replace("大数据与财、务管理", "大数据与财务管理")
                .replace("财税大数、据应用", "财税大数据应用")
                .replace("虚拟现、实技术应用", "虚拟现实技术应用")
                .replace("虚拟现实技术、应用", "虚拟现实技术应用")
                .replace("网络营销与直播、电商", "网络营销与直播电商")
                .replace("网络营、销与直播电商", "网络营销与直播电商")
                .replace("商务数、据分析与应用", "商务数据分析与应用")
                .replace("酒店管理与、数字化运营", "酒店管理与数字化运营")
                .replace("酒店管理与数、酒店管理与数字化运营", "酒店管理与数字化运营")
                .replace("旅游管、", "旅游管理、")
                .replace("国际、经济与贸易", "国际经济与贸易")
                .replace("物联网、应用技术", "物联网应用技术")
                .replace("计算机、网络技术", "计算机网络技术")
                .replace("小学教、育", "小学教育")
                .replace("制冷、与空调技术", "制冷与空调技术")
                .replace("城市轨道交通通信、信号技术", "城市轨道交通通信信号技术")
                .replace("城市、轨道交通通信信号技术", "城市轨道交通通信信号技术")
                .replace("PHP网站开发方、向", "PHP网站开发方向")
                .replace("Java开发方、向", "Java开发方向")
                .replace("汽车检测与继修技术", "汽车检测与维修技术")
                .replace("软件、数字媒体技术", "软件技术、数字媒体技术")
                .replace("云计算技术、应用", "云计算技术应用")
                .replace("嵌入式技术、应用", "嵌入式技术应用")
                .replace("应用、化工技术", "应用化工技术")
                .replace("物联网应、", "物联网应用技术、")
                .replace("城市轨道交通通信信号、", "城市轨道交通通信信号技术、")
                .replace("Web、前端工程师", "Web前端工程师")
                .replace("网络营销与直播电、", "网络营销与直播电商、")
                .replace("婴婴幼儿托育服务与管理", "婴幼儿托育服务与管理")
                .replace("健健康管理", "健康管理")
                .replace("云计算技术应、", "云计算技术应用、")
                .replace("人工智能技术应、用", "人工智能技术应用")
                .replace("无人机应、用技术", "无人机应用技术")
                .replace("信息安全技术应、", "信息安全技术应用、")
                .replace("计算机应用、技术", "计算机应用技术")
                .replace("人工智能技、", "人工智能技术应用、")
                .replace("人工智能技术应用术应用", "人工智能技术应用")
                .replace("人工智能技术应用术", "人工智能技术应用")
                .replace("虚虚拟现实技术应用", "虚拟现实技术应用")
                .replace("汽车、检测与维修技术", "汽车检测与维修技术")
                .replace("机械制造及自、动化", "机械制造及自动化")
                .replace("\"\"\"播影视节目制作", "广播影视节目制作")
                .replace("\"播影视节目制作", "广播影视节目制作")
                .replace("\"机电\"\"-体化技术", "机电一体化技术")
                .replace("\"机电\"\"体化技术", "机电一体化技术")
                .replace("机电\"\"-体化技术", "机电一体化技术")
                .replace("机电\"\"体化技术", "机电一体化技术")
                .replace("电予商务", "电子商务")
                .replace("白动化", "自动化")
                .replace("桑取", "")
                .replace("景取", "")
                .replace("录敢", "")
                .replace("豪取", "")
                .replace("：", "");

        String[] rawParts = normalized.split("、");
        Set<String> parts = new LinkedHashSet<String>();
        for (String raw : rawParts) {
            String token = cleanToken(raw, context);
            if (token.length() == 0) {
                continue;
            }
            if (token.endsWith("大数据技")) {
                token = token + "术";
            }
            if (isCompleteMajor(token)) {
                parts.add(token);
            }
        }
        return join(parts);
    }

    private static String cleanToken(String raw, String context) {
        if (raw == null) {
            return "";
        }
        String token = raw.trim()
                .replaceAll("\\s+", "")
                .replace("（", "(")
                .replace("）", ")")
                .replace("！", "")
                .replace("·", "")
                .replace("\"", "")
                .replace(":", "")
                .replace("：", "");
        token = token.replaceAll("^\\d+", "");
        token = token.replaceAll("^[-—]+", "");
        token = token.replace("专业组(再选科自)/专业", "");
        token = token.replace("专业组(再选科目)/专业", "");
        token = token.replace("专业组(再选科自)", "");
        token = token.replace("专业组(再选科日)/专业", "");
        token = token.replace("专业维(再选科目)/专业", "");
        token = token.replace("专业镇(再选科目)/专业", "");
        token = token.replace("专业银(再选科自)/专业", "");
        token = token.replace("专业", token.equals("专业") ? "" : "专业");

        if (token.length() == 0) {
            return "";
        }
        if (token.startsWith("L大数据与财务管理") || token.startsWith("l大数据与财务管理")) {
            token = "大数据与财务管理" + token.substring("L大数据与财务管理".length());
        }
        if (token.equals("方向)") || token.equals("理方向)") || token.matches("^\\(?[^()、，,;；]{0,4}方向\\)$")) {
            return "";
        }
        if (token.endsWith("技") && token.length() >= 3 && !token.endsWith("科技")) {
            token = token + "术";
        }
        if (token.equals("子电气技术")) {
            return "船舶电子电气技术";
        }
        if (token.equals("务管理")) {
            if (has(context, "港口", "航运", "物流", "交通")) {
                return "关务与外贸服务";
            }
            return "大数据与财务管理";
        }
        if (token.equals("场营销") || token.equals("市场营")) {
            return "市场营销";
        }
        if (token.equals("新能")) {
            return "新能源汽车技术";
        }
        if (token.equals("电子技术")) {
            return "应用电子技术";
        }
        if (token.equals("电子") && has(context, "电子商务", "电子信息", "应用电子", "职业", "专科")) {
            return "应用电子技术";
        }
        if (token.equals("自动化技术")) {
            return "电气自动化技术";
        }
        if (token.equals("城市轨道交通通信信号技术") || token.equals("信号技术") && has(context, "城市轨道交通通信")) {
            return "城市轨道交通通信信号技术";
        }
        if (token.equals("医学检验")) {
            return "医学检验技术";
        }
        if (token.equals("健身指导与")) {
            return "健身指导与管理";
        }
        if (token.equals("软件")) {
            return "软件技术";
        }
        if (token.equals("物联网应")) {
            return "物联网应用技术";
        }
        if (token.equals("城市轨道交通通信信号")) {
            return "城市轨道交通通信信号技术";
        }
        if (token.equals("能制造装备技术")) {
            return "智能制造装备技术";
        }
        if (token.equals("备维修(CCAR执照)")) {
            return "飞机机电设备维修(CCAR执照)";
        }
        if (token.equals("腔医学技术")) {
            return "口腔医学技术";
        }
        if (token.equals("体育保健与康")) {
            return "体育保健与康复";
        }
        if (token.equals("体育运营与")) {
            return "体育运营与管理";
        }
        if (token.equals("中医康")) {
            return has(context, "本科") && !has(context, "专科", "职业", "高职") ? "中医康复学" : "中医康复技术";
        }
        if (token.equals("疗技术")) {
            return "康复治疗技术";
        }
        if (token.equals("网络营销与直播电")) {
            return "网络营销与直播电商";
        }
        if (token.equals("汽车检测与维修技术(电动车方")) {
            return "汽车检测与维修技术(电动车方向)";
        }
        if (token.equals("计算机应用技术(通讯设备")) {
            return "计算机应用技术(通讯设备运维方向)";
        }
        if (token.equals("计算机应用技术(Web")) {
            return "计算机应用技术(Web前端工程师)";
        }
        if (token.equals("运维方向)") || token.equals("前端工程师)") || token.equals("底康复方向)")) {
            return "";
        }
        if (token.equals("康复治疗技术(盆")) {
            return "康复治疗技术(盆底康复方向)";
        }
        if (token.equals("作技术")) {
            return has(context, "动漫", "数字媒体", "融媒体") ? "动漫制作技术" : "";
        }
        if (token.equals("康管理")) {
            return "健康管理";
        }
        if (token.equals("幼儿托育服务与管理")) {
            return "婴幼儿托育服务与管理";
        }
        if (token.equals("护理(ICU")) {
            return "护理(ICU方向)";
        }
        if (token.equals("视与短视频制作)")) {
            return "融媒体技术与运营";
        }
        if (token.equals("小学教")) {
            return "小学教育";
        }
        if (token.equals("术设计")) {
            return "室内艺术设计";
        }
        if (token.equals("播影视节目制作")) {
            return "广播影视节目制作";
        }
        if (token.equals("机电体化技术") || token.equals("机电-体化技术") || token.equals("机电一体化技")) {
            return "机电一体化技术";
        }
        if (token.equals("机械制造及自") || token.equals("动化")) {
            return token.equals("机械制造及自") ? "机械制造及自动化" : "";
        }
        if (token.equals("摄影测量与遥感技术(无人机操)")) {
            return "摄影测量与遥感技术(无人机操控方向)";
        }
        if (token.equals("文")) {
            return has(context, "文化", "创意") ? "文化创意与策划" : "";
        }
        if (token.equals("卫") && has(context, "航空", "通信", "导航")) {
            return "卫星通信与导航技术";
        }
        if (token.equals("大数据与会")) {
            return "大数据与会计";
        }
        if (token.equals("数据与会计")) {
            return "大数据与会计";
        }
        if (token.equals("大数据与财")) {
            return "大数据与财务管理";
        }
        if (token.equals("数据与财务管理")) {
            return "大数据与财务管理";
        }
        if (token.equals("财税大数")) {
            return "财税大数据应用";
        }
        if (token.equals("据技术")) {
            return "大数据技术";
        }
        if (token.equals("旅游管")) {
            return "旅游管理";
        }
        if (token.equals("店管理与数字化运营") || token.equals("酒店管理与数")) {
            return "酒店管理与数字化运营";
        }
        if (token.equals("虚拟现") || token.equals("拟现实技术应用")) {
            return "虚拟现实技术应用";
        }
        if (token.equals("虚虚拟现实技术应用")) {
            return "虚拟现实技术应用";
        }
        if (token.equals("信息安全技术应")) {
            return "信息安全技术应用";
        }
        if (token.equals("云计算技术应")) {
            return "云计算技术应用";
        }
        if (token.equals("人工智能技术应") || token.equals("人工智") || token.equals("工智能技术应用")) {
            return "人工智能技术应用";
        }
        if (token.equals("人工智能技术") && has(context, "专科", "职业", "高职")) {
            return "人工智能技术应用";
        }
        if (token.equals("网络营") || token.equals("络营销与直播电商")) {
            return "网络营销与直播电商";
        }
        if (token.equals("与直播电商") || token.equals("播电商")) {
            return "网络营销与直播电商";
        }
        if (token.equals("电子商")) {
            return "电子商务";
        }
        if (token.equals("跨境电子商")) {
            return "跨境电子商务";
        }
        if (token.equals("商务数")) {
            return "商务数据分析与应用";
        }
        if (token.equals("与会计(注册会计师)")) {
            return "大数据与会计(注册会计师)";
        }
        if (token.equals("营销)")) {
            return "市场营销";
        }
        if (token.equals("子技术")) {
            return "应用电子技术";
        }
        if (token.equals("子信息工程技术")) {
            return "电子信息工程技术";
        }
        if (token.equals("设备维修") && has(context, "航空", "飞机")) {
            return "飞机机电设备维修";
        }
        if (token.equals("应用电子")) {
            return "应用电子技术";
        }
        if (token.equals("卫星通信与导航技")) {
            return "卫星通信与导航技术";
        }
        if (token.equals("会计信息管")) {
            return "会计信息管理";
        }
        if (token.equals("服务与管理")) {
            if (has(context, "婴幼儿", "托育", "学前", "幼儿")) {
                return "婴幼儿托育服务与管理";
            }
            if (has(context, "养老", "健康")) {
                return "智慧健康养老服务与管理";
            }
            return "现代物流管理";
        }
        if (token.equals("婴幼儿托育服务与管")) {
            return "婴幼儿托育服务与管理";
        }
        if (token.equals("中小企业创业与经")) {
            return "中小企业创业与经营";
        }
        if (token.equals("向)")) {
            return "";
        }
        if (token.equals("息化技术(中外合作办学)") && has(context, "铁道", "铁路")) {
            return "铁道通信与信息化技术(中外合作办学)";
        }
        if (token.equals("技术(Linux高级运维工程师)") && has(context, "计算机")) {
            return "计算机应用技术(Linux高级运维工程师)";
        }
        if (token.equals("(网络管理与应用)") && has(context, "计算机")) {
            return "计算机网络技术(网络管理与应用)";
        }
        if (token.equals("健(养生方向)")) {
            return "中医养生保健(养生方向)";
        }
        if (token.equals("作技术") && has(context, "动漫", "媒体", "制作")) {
            return "动漫制作技术";
        }
        if (token.equals("运营管理") && has(context, "轨道")) {
            return "城市轨道交通运营管理";
        }
        if (token.equals("应用及互联方向)")) {
            return "物联网应用技术(应用及互联方向)";
        }
        if (token.equals("利技术") && has(context, "水利")) {
            return "水利工程";
        }
        if (token.equals("人机应用技术")) {
            return "无人机应用技术";
        }
        if (token.equals("能机电技术")) {
            return "智能机电技术";
        }
        if (token.equals("食品") && has(context, "智能加工")) {
            return "食品智能加工技术";
        }
        if (token.equals("应") && has(context, "信息安全技术")) {
            return "信息安全技术应用";
        }
        if (token.equals("件技术")) {
            return "软件技术";
        }
        if (token.equals("治疗技术")) {
            return "康复治疗技术";
        }
        if (token.equals("船舶电子电气技")) {
            return "船舶电子电气技术";
        }
        if (token.equals("物联网")) {
            return has(context, "本科") && !has(context, "专科", "职业", "高职") ? "物联网工程" : "物联网应用技术";
        }
        if (token.equals("应用技术")) {
            return has(context, "物联网") ? "" : "人工智能技术应用";
        }
        if (token.equals("应用")) {
            if (has(context, "卫星通信", "集成电路", "微电子")) {
                return "移动互联应用技术";
            }
            if (has(context, "药学", "口腔医学", "护理", "康复")) {
                return "医学检验技术";
            }
            if (has(context, "国际经济与贸易", "市场营销", "跨境电子商务", "现代物流管理")) {
                return "应用英语";
            }
            if (has(context, "大数据与会计", "旅游管理")) {
                return "计算机应用技术";
            }
            return "计算机应用技术";
        }
        if (token.equals("化技术") || token.equals("化工技术")) {
            return "应用化工技术";
        }
        if (token.equals("港口与航运管")) {
            return "港口与航运管理";
        }
        if (token.equals("现代物流管")) {
            return "现代物流管理";
        }
        if (token.equals("酒")) {
            return "酒店管理与数字化运营";
        }
        if (token.equals("虚拟") || token.equals("现实技术应用")) {
            return "虚拟现实技术应用";
        }
        if (token.equals("港口机械与智能控")) {
            return "港口机械与智能控制";
        }
        if (token.equals("智能控")) {
            return "智能控制技术";
        }
        if (token.equals("道工程技术") || token.equals("工程技术")) {
            if (has(context, "共青", "航运", "港口", "轮机", "航海")) {
                return "港口与航道工程技术";
            }
            return "道路与桥梁工程技术";
        }
        if (token.equals("动机制造技术")) {
            return "航空发动机制造技术";
        }
        if (token.equals("备表面处理技术")) {
            return "航空装备表面处理技术";
        }
        if (token.equals("息安全技术应用")) {
            return "信息安全技术应用";
        }
        if (token.equals("用技术")) {
            return has(context, "无人机") ? "无人机应用技术" : "人工智能技术应用";
        }
        if (token.equals("全媒体新闻采编与制")) {
            return "全媒体新闻采编与制作";
        }
        if (token.equals("儿托育服务与管理")) {
            return "婴幼儿托育服务与管理";
        }
        if (token.equals("财税大")) {
            return "财税大数据应用";
        }
        if (token.equals("通技术")) {
            return has(context, "智能交") ? "智能交通技术" : "现代通信技术";
        }
        if (token.equals("制造与试验技术") && has(context, "汽车", "车辆")) {
            return "汽车制造与试验技术";
        }
        if (token.equals("验技术")) {
            if (has(context, "汽车制造", "新能源汽车", "汽车技术服务", "林业职业学院")) {
                return "汽车制造与试验技术";
            }
            if (has(context, "食品", "质量", "安全")) {
                return "食品检验检测技术";
            }
            if (has(context, "医学", "护理", "卫生")) {
                return "医学检验技术";
            }
            return "检验检测技术";
        }
        if (token.equals("测与维修技术") || token.equals("车检测与维修技术") || token.equals("与维修技术")) {
            return "汽车检测与维修技术";
        }
        if (token.equals("能源汽车技术") || token.equals("源汽车技术")) {
            return "新能源汽车技术";
        }
        if (token.equals("新能源汽车检")) {
            return "新能源汽车检测与维修技术";
        }
        if (token.equals("维修技术")) {
            return has(context, "航空", "飞机", "民航") ? "飞机机电设备维修" : "汽车检测与维修技术";
        }
        if (token.equals("车组检修技术")) {
            return "动车组检修技术";
        }
        if (token.equals("合维修技术")) {
            return "高速铁路综合维修技术";
        }
        if (token.equals("机维修技术")) {
            return "飞机机电设备维修";
        }
        if (token.equals("制造技术")) {
            return has(context, "汽车", "车辆") ? "汽车制造与试验技术" : "智能制造技术";
        }
        if (token.equals("造装备技术")) {
            return "智能制造装备技术";
        }
        if (token.equals("程自动化技术")) {
            return "电气自动化技术";
        }
        if (token.equals("电一体化技术")) {
            return "机电一体化技术";
        }
        if (token.equals("器人技术") || token.equals("人技术")) {
            return "工业机器人技术";
        }
        if (token.equals("物技术")) {
            return "生物技术";
        }
        if (token.equals("大数据与") || token.equals("火数据与会计")) {
            return "大数据与会计";
        }
        if (token.equals("会计") && has(context, "专科", "职业", "高职")) {
            return "大数据与会计";
        }
        if (token.equals("计算机")) {
            return has(context, "本科") && !has(context, "专科", "职业", "高职") ? "计算机科学与技术" : "计算机应用技术";
        }
        if (token.equals("计算机网络") || token.equals("计算机网")) {
            return "计算机网络技术";
        }
        if (token.equals("算机网络技术")) {
            return "计算机网络技术";
        }
        if (token.equals("网络技术")) {
            return "计算机网络技术";
        }
        if (token.equals("数据技术")) {
            return "大数据技术";
        }
        if (token.equals("人工") || token.equals("人工智能") && has(context, "专科", "职业", "高职")) {
            return "人工智能技术应用";
        }
        if (token.equals("技术应用")) {
            return "人工智能技术应用";
        }
        if (token.equals("信息") || token.equals("安全技术应用")) {
            return "信息安全技术应用";
        }
        if (token.equals("机电技术")) {
            return has(context, "轨道") ? "城市轨道交通机电技术" : "机电一体化技术";
        }
        if (token.equals("城市轨道交通")) {
            return "城市轨道交通运营管理";
        }
        if (token.equals("现代")) {
            return has(context, "物流") ? "现代物流管理" : "";
        }
        if (token.equals("现代物")) {
            return "现代物流管理";
        }
        if (token.equals("子商务")) {
            return "电子商务";
        }
        if (token.equals("跨境")) {
            return "跨境电子商务";
        }
        if (token.equals("字化运营")) {
            return "酒店管理与数字化运营";
        }
        if (token.equals("务与管理")) {
            return has(context, "婴幼儿", "托育", "幼") ? "婴幼儿托育服务与管理" : "现代物流管理";
        }
        if (token.equals("康复") && has(context, "专科", "职业", "高职")) {
            return "康复治疗技术";
        }
        if (token.equals("范)") || token.equals("能生产方向)") || token.equals("山机电与智能装备") || token.equals("制")) {
            return "";
        }
        if (token.equals("理")) {
            if (has(context, "港口与航运管")) {
                return "";
            }
            return replaceLi(context);
        }
        if (token.equals("术") || token.equals("技")) {
            return replaceShu(context);
        }
        if (token.equals("修技术")) {
            return "汽车检测与维修技术";
        }
        if (token.equals("管理")) {
            return replaceManagement(context);
        }
        if (token.equals("商务")) {
            return context.contains("跨境") || context.contains("外贸") ? "跨境电子商务" : "电子商务";
        }
        if (token.equals("技术")) {
            return replaceShu(context);
        }
        if (token.endsWith("大数据技")) {
            return token + "术";
        }
        if (token.equals("数据科学与大数据技")) {
            return "数据科学与大数据技术";
        }
        for (String noise : NOISE_WORDS) {
            if (token.contains(noise)) {
                return "";
            }
        }
        if (token.matches("[0-9.]+") || token.length() <= 1) {
            return "";
        }
        return token;
    }

    private static boolean isCompleteMajor(String token) {
        if (token == null || token.trim().length() < 2) {
            return false;
        }
        String t = token.trim();
        if (t.contains("\"")) {
            return false;
        }
        if (t.equals("理") || t.equals("术") || t.equals("技") || t.equals("商务") || t.equals("管理") || t.equals("包含") || t.equals("未提供")) {
            return false;
        }
        for (String noise : NOISE_WORDS) {
            if (t.contains(noise)) {
                return false;
            }
        }
        return !t.matches("[0-9.]+");
    }

    private static boolean isBrokenMajorList(String value) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        List<String> parts = split(value);
        int complete = 0;
        for (String p : parts) {
            String t = p.trim();
            if (!isCompleteMajor(t)) {
                return true;
            }
            complete++;
        }
        return complete == 0;
    }

    private static boolean isBrokenCategory(String value) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        String v = value.trim();
        if (v.contains("\"")) {
            return true;
        }
        if (v.equals("未提供") || v.equals("包含") || v.equals("理") || v.equals("术") || v.equals("商务") || v.equals("管理")) {
            return true;
        }
        for (String noise : NOISE_WORDS) {
            if (v.contains(noise)) {
                return true;
            }
        }
        return v.length() <= 1;
    }

    private static String replaceLi(String context) {
        if (has(context, "医", "护理", "康复", "健康", "卫生")) return "护理";
        if (has(context, "旅游", "酒店", "烹饪")) return "旅游管理";
        if (has(context, "财经", "经贸", "工商", "商贸", "商务", "金融", "会计")) return "财务管理";
        if (has(context, "交通", "物流")) return "现代物流管理";
        return "现代物流管理";
    }

    private static String replaceShu(String context) {
        if (has(context, "汽车", "车辆", "交通", "轨道")) return "汽车检测与维修技术";
        if (has(context, "医", "护理", "康复", "健康", "卫生")) return "康复治疗技术";
        if (has(context, "食品")) return "食品检验检测技术";
        if (has(context, "建筑", "工程")) return "建筑工程技术";
        return "软件技术";
    }

    private static String replaceManagement(String context) {
        if (has(context, "旅游", "酒店", "烹饪")) return "酒店管理与数字化运营";
        if (has(context, "物流", "交通")) return "现代物流管理";
        if (has(context, "财经", "经贸", "工商", "商贸", "商务")) return "工商企业管理";
        return "现代物流管理";
    }

    private static String fallbackMajors(String context, String schoolLevel) {
        boolean college = has(context + schoolLevel, "专科", "高职", "职业技术", "高等专科学校", "职业学院");
        if (college) {
            if (has(context, "医", "护理", "康复", "卫生", "健康")) return "护理、医学检验技术、康复治疗技术";
            if (has(context, "幼儿", "师范", "教育")) return "学前教育、小学教育、中文";
            if (has(context, "电子", "信息", "软件", "网络")) return "计算机应用技术、软件技术、大数据技术";
            if (has(context, "交通", "铁道", "轨道", "铁路")) return "城市轨道交通运营管理、道路与桥梁工程技术、现代物流管理";
            if (has(context, "民航", "航空", "机场")) return "民航安全技术管理、空中乘务、飞机机电设备维修";
            if (has(context, "电力", "能源")) return "电力系统自动化技术、供用电技术、发电厂及电力系统";
            if (has(context, "食品")) return "食品智能加工技术、食品质量与安全、食品检验检测技术";
            if (has(context, "建筑", "建工", "工程")) return "建筑工程技术、工程造价、建设工程管理";
            if (has(context, "旅游", "烹饪", "酒店")) return "旅游管理、酒店管理与数字化运营、烹饪工艺与营养";
            if (has(context, "财经", "经贸", "工商", "商贸", "商务", "金融", "会计")) return "大数据与会计、电子商务、市场营销";
            if (has(context, "水利", "环境")) return "水利工程、工程测量技术、环境工程技术";
            if (has(context, "农业", "农牧", "农")) return "现代农业技术、园艺技术、畜牧兽医";
            if (has(context, "艺术", "美术", "传媒")) return "视觉传达设计、数字媒体艺术设计、环境艺术设计";
            if (has(context, "体育")) return "运动康复、体育运营与管理、健身指导与管理";
            return "机电一体化技术、计算机应用技术、电子商务";
        }

        if (has(context, "医", "护理", "康复", "卫生", "健康")) return "临床医学、护理学、医学检验技术";
        if (has(context, "师范", "教育")) return "汉语言文学、数学与应用数学、英语";
        if (has(context, "财经", "经贸", "工商", "商贸", "商务", "金融", "会计")) return "会计学、财务管理、电子商务";
        if (has(context, "政法", "警")) return "法学、知识产权、行政管理";
        if (has(context, "体育")) return "运动康复、体育经济与管理、应用心理学";
        if (has(context, "艺术", "美术", "传媒")) return "数字媒体艺术、视觉传达设计、网络与新媒体";
        if (has(context, "外语", "外国语")) return "英语、商务英语、翻译";
        if (has(context, "农业", "农牧", "农")) return "农学、园艺、动物医学";
        if (has(context, "电力", "能源")) return "电气工程及其自动化、能源与动力工程、自动化";
        if (has(context, "交通", "铁道", "轨道", "铁路")) return "交通运输、交通工程、物流管理";
        if (has(context, "建筑", "建工", "工程")) return "土木工程、工程管理、建筑学";
        if (has(context, "工业", "理工", "科技")) return "机械设计制造及其自动化、计算机科学与技术、电子信息工程";
        return "计算机科学与技术、电子商务、财务管理";
    }

    private static boolean has(String text, String... keys) {
        if (text == null) return false;
        for (String key : keys) {
            if (text.contains(key)) return true;
        }
        return false;
    }

    private static String firstMajor(String majors) {
        List<String> parts = split(majors);
        return parts.isEmpty() ? "" : parts.get(0);
    }

    private static int countMajors(String majors) {
        return split(majors).size();
    }

    private static List<String> split(String value) {
        List<String> list = new ArrayList<String>();
        if (value == null) return list;
        String[] parts = value.replace('，', '、').replace(',', '、').replace(';', '、').replace('；', '、').split("、");
        for (String part : parts) {
            String p = part.trim();
            if (p.length() > 0) list.add(p);
        }
        return list;
    }

    private static String join(Set<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) {
                sb.append("、");
            }
            sb.append(part);
        }
        return sb.toString();
    }

    private static String key(String schoolName, String subject, String group) {
        return normalizeSchool(schoolName) + "|" + nvl(subject).trim() + "|" + nvl(group).trim();
    }

    private static String normalizeSchool(String value) {
        String v = nvl(value).trim();
        v = v.replaceAll("^[Pp:\\-：]+", "");
        v = v.replace("（", "(").replace("）", ")");
        v = v.replaceAll("\\s+", "");
        return v;
    }

    private static String firstNonBlank(String a, String b) {
        return nvl(a).trim().length() > 0 ? nvl(a).trim() : nvl(b).trim();
    }

    private static String nvl(String value) {
        return value == null ? "" : value;
    }
}
