let subjectType = '历史';
let predictionLines = [];

const MAX_VISIBLE_ROWS = 3;
const QUERY_LIMIT = 18;
const PLAN_COUNT = 6;
const HISTORY_UNDERGRADUATE_LINE_2025 = 471;
const PHYSICS_UNDERGRADUATE_LINE_2025 = 427;
const NEAR_UNDERGRADUATE_MARGIN = 20;
const STATIC_LOGIN_USER = 'admin';
const STATIC_LOGIN_HASH = '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9';
const STATIC_LOGIN_KEY = 'gaokao_pages_login_ok';
const STATIC_LOGIN_FALLBACK_USERS = [
    { username: STATIC_LOGIN_USER, passwordHash: STATIC_LOGIN_HASH }
];
const SCHOOL_PROVINCES = [
    '全部地区', '北京', '天津', '河北', '山西', '内蒙古',
    '辽宁', '吉林', '黑龙江', '上海', '江苏',
    '浙江', '安徽', '福建', '江西', '山东',
    '河南', '湖北', '湖南', '广东', '广西',
    '海南', '重庆', '四川', '贵州', '云南',
    '西藏', '陕西', '甘肃', '青海', '宁夏', '新疆'
];
const HIGH_QUALITY_JUNIOR_COLLEGE_KEYWORDS = [
    '黄河水利职业技术学院', '河南工业职业技术学院', '河南职业技术学院', '河南农业职业学院', '许昌职业技术学院',
    '郑州铁路职业技术学院', '河南经贸职业学院', '河南交通职业技术学院', '河南应用技术职业学院', '河南医学高等专科学校',
    '北京电子科技职业学院', '北京工业职业技术学院', '天津市职业大学', '天津医学高等专科学校', '天津电子信息职业技术学院',
    '石家庄铁路职业技术学院', '唐山工业职业技术学院', '山西工程职业学院', '辽宁省交通高等专科学校', '沈阳职业技术学院',
    '长春汽车工业高等专科学校', '吉林铁道职业技术学院', '黑龙江建筑职业技术学院', '哈尔滨职业技术学院',
    '上海工艺美术职业学院', '上海电子信息职业技术学院', '南京工业职业技术大学', '江苏农林职业技术学院', '常州信息职业技术学院',
    '无锡职业技术学院', '江苏经贸职业技术学院', '金华职业技术大学', '浙江金融职业学院', '杭州职业技术学院',
    '宁波职业技术学院', '温州职业技术学院', '芜湖职业技术学院', '安徽商贸职业技术学院', '福建船政交通职业学院',
    '九江职业技术学院', '江西应用技术职业学院', '山东商业职业技术学院', '淄博职业学院', '日照职业技术学院',
    '武汉职业技术学院', '武汉船舶职业技术学院', '武汉铁路职业技术学院', '长沙民政职业技术学院', '湖南铁道职业技术学院',
    '广东轻工职业技术学院', '深圳职业技术大学', '广州番禺职业技术学院', '重庆电子工程职业学院', '重庆工业职业技术学院',
    '成都航空职业技术学院', '四川交通职业技术学院', '贵州交通职业技术学院', '昆明冶金高等专科学校', '陕西工业职业技术学院',
    '杨凌职业技术学院', '西安航空职业技术学院', '兰州资源环境职业技术大学', '宁夏职业技术学院', '新疆农业职业技术学院'
];

const $ = id => document.getElementById(id);
let appBooted = false;
let recommendNonce = 0;
let lastCriteriaKey = '';
let staticLoginUsersPromise = null;

setupStaticLogin();

document.querySelectorAll('.subject').forEach(btn => {
    btn.addEventListener('click', () => {
        document.querySelectorAll('.subject').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        subjectType = btn.dataset.value;
        resetPlanAndRender();
    });
});

$('recommendBtn').addEventListener('click', () => {
    const criteriaKey = getCurrentCriteriaKey();
    if (criteriaKey === lastCriteriaKey) {
        recommendNonce = (recommendNonce + 1) % PLAN_COUNT;
    } else {
        recommendNonce = 0;
        lastCriteriaKey = criteriaKey;
    }
    renderRecommend();
});

$('schoolProvince').addEventListener('change', resetPlanAndRender);
$('score').addEventListener('change', resetPlanAndRender);

function setupStaticLogin() {
    const loginView = $('loginView');
    const appView = $('appView');
    if (!loginView || !appView) {
        bootApp();
        return;
    }

    const showLogin = () => {
        document.body.classList.add('locked');
        loginView.hidden = false;
        appView.hidden = true;
        const pass = $('loginPass');
        if (pass) pass.value = '';
        setTimeout(() => $('loginUser') && $('loginUser').focus(), 0);
    };
    const showApp = () => {
        document.body.classList.remove('locked');
        loginView.hidden = true;
        appView.hidden = false;
        bootApp();
    };

    $('loginForm').addEventListener('submit', async event => {
        event.preventDefault();
        const ok = await verifyStaticLogin($('loginUser').value.trim(), $('loginPass').value);
        $('loginError').hidden = ok;
        if (!ok) return;
        sessionStorage.setItem(STATIC_LOGIN_KEY, '1');
        showApp();
    });
    $('staticLogout').addEventListener('click', () => {
        sessionStorage.removeItem(STATIC_LOGIN_KEY);
        showLogin();
    });

    if (sessionStorage.getItem(STATIC_LOGIN_KEY) === '1') {
        showApp();
    } else {
        showLogin();
    }
}

function bootApp() {
    if (appBooted) return;
    appBooted = true;
    init();
}

async function verifyStaticLogin(username, password) {
    if (!username || !password) return false;
    const users = await loadStaticLoginUsers();
    const normalizedUsername = username.trim().toLowerCase();
    const matchedUsers = users.filter(user => String(user.username || '').trim().toLowerCase() === normalizedUsername);
    if (!matchedUsers.length) return false;

    const passwordHash = window.crypto && window.crypto.subtle ? await sha256(password) : '';
    const bcrypt = window.dcodeIO && window.dcodeIO.bcrypt;
    return matchedUsers.some(user => {
        const shaHash = String(user.passwordHash || user.hash || '').trim();
        if (shaHash && passwordHash && shaHash === passwordHash) return true;
        const bcryptHash = String(user.bcryptHash || '').trim();
        return Boolean(bcryptHash && bcrypt && bcrypt.compareSync(password, bcryptHash));
    });
}

async function loadStaticLoginUsers() {
    if (!staticLoginUsersPromise) {
        staticLoginUsersPromise = fetch('./assets/static-users.json', { cache: 'no-store' })
            .then(response => response.ok ? response.json() : [])
            .then(users => Array.isArray(users) ? users : [])
            .then(users => mergeStaticUsers(STATIC_LOGIN_FALLBACK_USERS, users))
            .catch(() => STATIC_LOGIN_FALLBACK_USERS);
    }
    return staticLoginUsersPromise;
}

function mergeStaticUsers(baseUsers, extraUsers) {
    const merged = new Map();
    baseUsers.concat(extraUsers).forEach(user => {
        const username = String(user.username || '').trim();
        const passwordHash = String(user.passwordHash || user.hash || '').trim();
        if (username && passwordHash) {
            merged.set(username.toLowerCase(), { username, passwordHash });
        }
    });
    return Array.from(merged.values());
}

async function sha256(value) {
    const data = new TextEncoder().encode(value);
    const hash = await window.crypto.subtle.digest('SHA-256', data);
    return Array.from(new Uint8Array(hash)).map(b => b.toString(16).padStart(2, '0')).join('');
}

async function init() {
    renderProvinceOptions();
    try {
        const response = await fetch('./assets/prediction-lines.json?v=2026062001', { cache: 'no-store' });
        if (!response.ok) throw new Error('数据文件读取失败');
        predictionLines = await response.json();
        resetPlanAndRender();
    } catch (err) {
        $('resultArea').innerHTML = `<div class="empty">加载失败：${escapeHtml(err.message)}</div>`;
    }
}

function renderProvinceOptions() {
    $('schoolProvince').innerHTML = SCHOOL_PROVINCES
        .map(p => `<option value="${escapeHtml(p)}">${escapeHtml(p)}</option>`)
        .join('');
}

function resetPlanAndRender() {
    recommendNonce = 0;
    lastCriteriaKey = getCurrentCriteriaKey();
    renderRecommend();
}

function getCurrentCriteriaKey() {
    const score = Number($('score').value || 500);
    const schoolProvince = $('schoolProvince').value || '全部地区';
    return `${score}|${subjectType}|${schoolProvince}`;
}

function renderRecommend() {
    if (!predictionLines.length) return;

    const score = Number($('score').value || 500);
    const schoolProvince = $('schoolProvince').value || '全部地区';
    const criteriaKey = `${score}|${subjectType}|${schoolProvince}`;
    if (criteriaKey !== lastCriteriaKey) {
        recommendNonce = 0;
        lastCriteriaKey = criteriaKey;
    }
    $('tagSubject').innerText = subjectType + '类';
    $('tagProvince').innerText = schoolProvince;
    $('summaryText').innerText = `按历史录取数据参考：河南${subjectType}类，预估 ${score} 分，筛选条件为学校地区：${schoolProvince}，第 ${recommendNonce + 1} 批。`;

    const rows = recommend(score, subjectType, schoolProvince);
    $('resultArea').innerHTML = section('rush', '冲', '冲刺推荐', '适合略高于当前分数的院校', rows.rush)
        + section('stable', '稳', '稳妥推荐', '适合重点考虑的匹配院校', rows.stable)
        + section('safe', '保', '保底推荐', '适合保底填报的院校', rows.safe);
}

function recommend(score, subject, schoolProvince) {
    const limits = henanLimitsByBucket(score, subject, schoolProvince);
    const rushCandidates = recommendBucket(score, subject, schoolProvince, '冲刺', limits[0]);
    const stableCandidates = recommendBucket(score, subject, schoolProvince, '稳妥', limits[1]);
    const safeCandidates = recommendBucket(score, subject, schoolProvince, '保底', limits[2]);

    const used = new Map();
    const rushRows = takeUniqueRows(varyCandidateOrder(rushCandidates, score, subject, schoolProvince, '冲刺'), used);
    const allRegions = !schoolProvince || schoolProvince === '全部地区';
    let stableRows;
    let safeRows;
    if (allRegions) {
        stableRows = takeUniqueRows(varyCandidateOrder(stableCandidates, score, subject, schoolProvince, '稳妥'), used);
        safeRows = takeUniqueRows(varyCandidateOrder(safeCandidates, score, subject, schoolProvince, '保底'), used);
    } else {
        stableRows = takeUniqueRows(varyCandidateOrder(stableCandidates, score, subject, schoolProvince, '稳妥'), used);
        safeRows = takeUniqueRows(varyCandidateOrder(safeCandidates, score, subject, schoolProvince, '保底'), used);
    }
    rebalanceScarceHighScoreRows(score, subject, rushRows, stableRows, safeRows);
    const canonicalRows = canonicalizeRows(score, rushRows, stableRows, safeRows,
        rushCandidates, stableCandidates, safeCandidates);
    if (allRegions) enforceHenanFirstPageRows(canonicalRows, score, subject);

    return {
        rush: canonicalRows.rush.map(row => polishPrediction(row, score, '冲刺')),
        stable: canonicalRows.stable.map(row => polishPrediction(row, score, '稳妥')),
        safe: canonicalRows.safe.map(row => polishPrediction(row, score, '保底'))
    };
}

function recommendBucket(score, subject, schoolProvince, bucket, preferredHenanCount) {
    const allRegions = !schoolProvince || schoolProvince === '全部地区';
    const nearLineJuniorCollege = shouldUseQualityJuniorCollege(score, subject, bucket);
    const allowUndergraduate = score >= undergraduateLine(subject) && !nearLineJuniorCollege;
    const allowJuniorCollege = score < undergraduateLine(subject) || nearLineJuniorCollege;
    const all = queryCandidatesWithFallback(score, subject, schoolProvince, bucket, allowUndergraduate,
        allowJuniorCollege, nearLineJuniorCollege, QUERY_LIMIT);

    if (!allRegions) {
        if (bucket === '冲刺' && all.length < MAX_VISIBLE_ROWS && allowUndergraduate && !allowJuniorCollege) {
            const highScoreReserve = queryCandidatesWithFallback(score, subject, schoolProvince, '冲刺高分兜底',
                allowUndergraduate, allowJuniorCollege, nearLineJuniorCollege, QUERY_LIMIT);
            return mergeFallbackRows(all, highScoreReserve, QUERY_LIMIT);
        }
        if (bucket !== '保底' || all.length >= MAX_VISIBLE_ROWS) return all;
        const expanded = queryCandidatesWithFallback(score, subject, schoolProvince, '保底扩展', allowUndergraduate,
            allowJuniorCollege, nearLineJuniorCollege, QUERY_LIMIT);
        const merged = mergeFallbackRows(all, expanded, QUERY_LIMIT);
        if (merged.length >= MAX_VISIBLE_ROWS) return merged;
        const reserve = queryCandidatesWithFallback(score, subject, schoolProvince, '保底兜底', allowUndergraduate,
            allowJuniorCollege, nearLineJuniorCollege, QUERY_LIMIT);
        const reserved = mergeFallbackRows(merged, reserve, QUERY_LIMIT);
        if (reserved.length >= MAX_VISIBLE_ROWS) return reserved;
        const provincialReserve = queryCandidatesWithFallback(score, subject, schoolProvince, '保底同省补足',
            allowUndergraduate, allowJuniorCollege, nearLineJuniorCollege, QUERY_LIMIT);
        return mergeFallbackRows(reserved, provincialReserve, QUERY_LIMIT);
    }

    let pool = all;
    if (bucket === '冲刺' && pool.length < MAX_VISIBLE_ROWS && allowUndergraduate && !allowJuniorCollege) {
        const highScoreReserve = queryCandidatesWithFallback(score, subject, schoolProvince, '冲刺高分兜底',
            allowUndergraduate, allowJuniorCollege, nearLineJuniorCollege, QUERY_LIMIT);
        pool = mergeFallbackRows(pool, highScoreReserve, QUERY_LIMIT);
    }
    if (bucket === '保底' && pool.length < MAX_VISIBLE_ROWS) {
        const expanded = queryCandidatesWithFallback(score, subject, schoolProvince, '保底扩展',
            allowUndergraduate, allowJuniorCollege, nearLineJuniorCollege, QUERY_LIMIT);
        pool = mergeFallbackRows(pool, expanded, QUERY_LIMIT);
        if (pool.length < MAX_VISIBLE_ROWS) {
            const reserve = queryCandidatesWithFallback(score, subject, schoolProvince, '保底兜底',
                allowUndergraduate, allowJuniorCollege, nearLineJuniorCollege, QUERY_LIMIT);
            pool = mergeFallbackRows(pool, reserve, QUERY_LIMIT);
        }
        if (pool.length < MAX_VISIBLE_ROWS) {
            const provincialReserve = queryCandidatesWithFallback(score, subject, schoolProvince, '保底同省补足',
                allowUndergraduate, allowJuniorCollege, nearLineJuniorCollege, QUERY_LIMIT);
            pool = mergeFallbackRows(pool, provincialReserve, QUERY_LIMIT);
        }
    }
    let henan = queryCandidatesWithFallback(score, subject, '河南', bucket, allowUndergraduate,
        allowJuniorCollege, nearLineJuniorCollege, Math.max(preferredHenanCount + 2, 3));
    if (bucket === '冲刺' && henan.length < preferredHenanCount && allowUndergraduate && !allowJuniorCollege) {
        const henanReserve = queryCandidatesWithFallback(score, subject, '河南', '冲刺高分兜底',
            allowUndergraduate, allowJuniorCollege, nearLineJuniorCollege, Math.max(preferredHenanCount + 2, 3));
        henan = mergeFallbackRows(henan, henanReserve, Math.max(preferredHenanCount + 2, 3));
    }
    return mixHenanRows(pool, henan, preferredHenanCount);
}

function mergeFallbackRows(primary, fallback, limit) {
    const merged = new Map(primary.map(row => [rowKey(row), row]));
    for (const row of fallback) {
        merged.set(rowKey(row), row);
        if (merged.size >= limit) break;
    }
    return Array.from(merged.values());
}

function rebalanceScarceHighScoreRows(score, subject, rushRows, stableRows, safeRows) {
    if (!score || score < 680 || score < undergraduateLine(subject) + 100) return;
    if (!stableRows.length && rushRows.length > 1) {
        stableRows.push(rushRows.pop());
    }
    if (!safeRows.length) {
        if (stableRows.length > 1) {
            safeRows.push(stableRows.pop());
        } else if (rushRows.length > 2) {
            safeRows.push(rushRows.pop());
        }
    }
}

function canonicalizeRows(score, ...sources) {
    const result = { rush: [], stable: [], safe: [] };
    const used = new Set();
    for (const source of sources) {
        if (!source) continue;
        for (const row of source) {
            if (!row) continue;
            const key = rowKey(row);
            if (used.has(key)) continue;
            const bucket = canonicalBucket(row, score);
            if (result[bucket].length >= MAX_VISIBLE_ROWS) continue;
            result[bucket].push(row);
            used.add(key);
        }
    }
    rebalanceEmptyCanonicalRows(result);
    fillCanonicalRows(result, score, sources);
    return result;
}

function canonicalBucket(row, score) {
    const predicted = Number(row.predictScore || 0);
    const diff = predicted - Number(score || 0);
    if (diff >= 3) return 'rush';
    if (diff >= -9) return 'stable';
    return 'safe';
}

function rebalanceEmptyCanonicalRows(rows) {
    if (!rows.stable.length) {
        if (!moveEdgeRow(rows.rush, rows.stable, false)) moveEdgeRow(rows.safe, rows.stable, true);
    }
    if (!rows.rush.length) {
        if (!moveEdgeRow(rows.stable, rows.rush, true)) moveEdgeRow(rows.safe, rows.rush, true);
    }
    if (!rows.safe.length) {
        if (!moveEdgeRow(rows.stable, rows.safe, false)) moveEdgeRow(rows.rush, rows.safe, false);
    }
}

function moveEdgeRow(fromRows, toRows, highest) {
    if (!fromRows || fromRows.length <= 1 || !toRows || toRows.length) return false;
    let selectedIndex = 0;
    let selectedScore = Number(fromRows[0].predictScore || 0);
    for (let i = 1; i < fromRows.length; i++) {
        const currentScore = Number(fromRows[i].predictScore || 0);
        if ((highest && currentScore > selectedScore) || (!highest && currentScore < selectedScore)) {
            selectedScore = currentScore;
            selectedIndex = i;
        }
    }
    toRows.push(fromRows.splice(selectedIndex, 1)[0]);
    return true;
}

function fillCanonicalRows(result, score, sources) {
    const used = new Set([...result.rush, ...result.stable, ...result.safe].map(rowKey));
    fillCanonicalBucket(result.rush, 'rush', score, used, sources);
    fillCanonicalBucket(result.stable, 'stable', score, used, sources);
    fillCanonicalBucket(result.safe, 'safe', score, used, sources);
}

function fillCanonicalBucket(rows, bucket, score, used, sources) {
    for (const source of sources) {
        if (!source) continue;
        for (const row of source) {
            if (!row || rows.length >= MAX_VISIBLE_ROWS) return;
            const key = rowKey(row);
            if (used.has(key) || canonicalBucket(row, score) !== bucket) continue;
            rows.push(row);
            used.add(key);
        }
    }
}

function queryCandidatesWithFallback(score, subject, schoolProvince, bucket, allowUndergraduate, allowJuniorCollege, preferQualityJuniorCollege, limit) {
    const first = queryCandidates(score, subject, schoolProvince, bucket, allowUndergraduate, allowJuniorCollege,
        preferQualityJuniorCollege, limit);
    if (!preferQualityJuniorCollege || first.length >= MAX_VISIBLE_ROWS) {
        return first;
    }
    const merged = new Map(first.map(row => [rowKey(row), row]));
    const fallback = queryCandidates(score, subject, schoolProvince, bucket, allowUndergraduate, allowJuniorCollege,
        false, limit);
    for (const row of fallback) {
        merged.set(rowKey(row), row);
        if (merged.size >= limit) break;
    }
    return Array.from(merged.values());
}

function queryCandidates(score, subject, schoolProvince, bucket, allowUndergraduate, allowJuniorCollege, preferQualityJuniorCollege, limit) {
    const allRegions = !schoolProvince || schoolProvince === '全部地区';
    const line = undergraduateLine(subject);
    const [low, high] = scoreBand(score, bucket, allowUndergraduate, allowJuniorCollege, line);

    return predictionLines
        .filter(row => row.province === '河南')
        .filter(row => row.subjectType === subject)
        .filter(row => allRegions || row.schoolProvince === schoolProvince)
        .filter(row => row.schoolProvince && row.schoolProvince !== '未识别')
        .filter(row => row.majorDirection && row.majorDirection.trim() && !row.majorDirection.includes('未提供'))
        .filter(row => row.majorCategory && row.majorCategory.trim() && !['理', '术', '技', '管理', '商务', '包含', '未提供', '专业', '验技术', '方向'].includes(row.majorCategory))
        .filter(row => !preferQualityJuniorCollege || isHighQualityJuniorCollege(row))
        .filter(row => allowUndergraduate || row.schoolLevel !== '本科')
        .filter(row => allowJuniorCollege || row.schoolLevel !== '专科')
        .filter(row => !allowUndergraduate || allowJuniorCollege || row.schoolLevel === '本科')
        .filter(row => allowUndergraduate || !allowJuniorCollege || row.schoolLevel === '专科')
        .filter(row => {
            if (!allowUndergraduate && allowJuniorCollege) {
                return scoreOf(row) <= line + 25 && row.predictScore <= line + 25;
            }
            if (allowUndergraduate && !allowJuniorCollege) {
                return row.predictScore >= line;
            }
            return true;
        })
        .filter(row => row.predictScore >= low && row.predictScore <= high)
        .sort((a, b) => compareRows(a, b, score, bucket))
        .slice(0, Math.max(1, Math.min(limit, 30)));
}

function shouldUseQualityJuniorCollege(score, subject, bucket) {
    const line = undergraduateLine(subject);
    return score >= line && score <= line + NEAR_UNDERGRADUATE_MARGIN && bucket !== '冲刺';
}

function scoreBand(score, bucket, allowUndergraduate, allowJuniorCollege, line) {
    const juniorOnly = !allowUndergraduate && allowJuniorCollege;
    const undergraduateOnly = allowUndergraduate && !allowJuniorCollege;
    let low;
    let high;
    if (juniorOnly) {
        const rushWidth = score <= 320 ? 22 : 18;
        const safeWidth = score <= 320 ? 34 : 28;
        if (bucket === '冲刺') {
            low = score + 5;
            high = score + rushWidth;
        } else if (bucket === '冲刺高分兜底') {
            low = score + 3;
            high = score + rushWidth;
        } else if (bucket === '稳妥') {
            low = score - 9;
            high = score + 4;
        } else if (bucket === '保底扩展') {
            low = score - Math.max(42, safeWidth + 12);
            high = score - 6;
        } else if (bucket === '保底兜底') {
            low = score - Math.max(70, safeWidth + 35);
            high = score - 5;
        } else if (bucket === '保底同省补足') {
            low = score - Math.max(85, safeWidth + 50);
            high = score + 4;
        } else {
            low = score - safeWidth;
            high = score - 10;
        }
        high = Math.min(high, line + 18);
    } else if (undergraduateOnly) {
        if (bucket === '冲刺') {
            low = score + 3;
            high = score + 18;
        } else if (bucket === '冲刺高分兜底') {
            const fallbackWidth = score >= 680 ? 80 : (score >= 620 ? 55 : 35);
            low = score - fallbackWidth;
            high = 750;
        } else if (bucket === '稳妥') {
            low = score - 9;
            high = score + 2;
        } else if (bucket === '保底扩展') {
            low = score - 55;
            high = score - 6;
        } else if (bucket === '保底兜底') {
            low = line;
            high = score - 6;
        } else if (bucket === '保底同省补足') {
            low = line;
            high = score + 2;
        } else {
            low = score - 23;
            high = score - 10;
        }
        low = Math.max(low, line);
    } else if (bucket === '冲刺') {
        low = score + 3;
        high = score + 18;
    } else if (bucket === '冲刺高分兜底') {
        const fallbackWidth = score >= 680 ? 80 : (score >= 620 ? 55 : 35);
        low = score - fallbackWidth;
        high = 750;
    } else if (bucket === '稳妥') {
        low = score - 9;
        high = score + 2;
    } else if (bucket === '保底扩展') {
        low = score - 55;
        high = score - 6;
    } else if (bucket === '保底兜底') {
        low = score - 90;
        high = score - 6;
    } else if (bucket === '保底同省补足') {
        low = score - 100;
        high = score + 2;
    } else {
        low = score - 26;
        high = score - 10;
    }
    return [Math.max(0, low), Math.min(750, high)];
}

function compareRows(a, b, score, bucket) {
    const distance = Math.abs(a.predictScore - score) - Math.abs(b.predictScore - score);
    if (distance !== 0) return distance;
    if (bucket === '冲刺') {
        if (a.predictScore !== b.predictScore) return a.predictScore - b.predictScore;
    } else {
        if (a.predictScore !== b.predictScore) return b.predictScore - a.predictScore;
    }
    const henanOrder = (isHenan(a) ? 0 : 1) - (isHenan(b) ? 0 : 1);
    if (henanOrder !== 0) return henanOrder;
    return scoreOf(b) - scoreOf(a);
}

function takeUniqueRows(candidates, used) {
    const rows = [];
    for (const row of candidates) {
        const key = rowKey(row);
        if (used.has(key)) continue;
        rows.push(row);
        used.set(key, row);
        if (rows.length >= MAX_VISIBLE_ROWS) break;
    }
    return rows;
}

function varyCandidateOrder(candidates, score, subject, schoolProvince, bucket) {
    const plan = recommendNonce % PLAN_COUNT;
    if (plan <= 0 || candidates.length <= MAX_VISIBLE_ROWS) return candidates;
    const windowSize = Math.min(candidates.length, Math.max(MAX_VISIBLE_ROWS * PLAN_COUNT, 9));
    const head = candidates.slice(0, windowSize);
    const rest = candidates.slice(windowSize);
    let offset = (plan * MAX_VISIBLE_ROWS) % head.length;
    if (offset === 0 && head.length > MAX_VISIBLE_ROWS) {
        offset = Math.min(MAX_VISIBLE_ROWS, head.length - MAX_VISIBLE_ROWS);
    }
    return head.slice(offset).concat(head.slice(0, offset), rest);
}

function seededOffset(value, size) {
    let hash = 0;
    for (let i = 0; i < value.length; i++) {
        hash = ((hash << 5) - hash + value.charCodeAt(i)) | 0;
    }
    return Math.abs(hash) % Math.max(1, size);
}

function mixHenanRows(all, henan, preferredHenanCount) {
    const selected = new Map();
    let henanAdded = 0;
    for (const row of henan) {
        if (henanAdded >= preferredHenanCount) break;
        selected.set(rowKey(row), row);
        henanAdded++;
    }
    for (const row of all) {
        if (isHenan(row) && henanAdded >= preferredHenanCount && !selected.has(rowKey(row))) continue;
        if (isHenan(row) && !selected.has(rowKey(row))) henanAdded++;
        selected.set(rowKey(row), row);
        if (selected.size >= QUERY_LIMIT) break;
    }
    return Array.from(selected.values());
}

function henanLimitsByBucket(score, subject, schoolProvince) {
    if (schoolProvince && schoolProvince !== '全部地区') {
        return [MAX_VISIBLE_ROWS, MAX_VISIBLE_ROWS, MAX_VISIBLE_ROWS];
    }
    const totalLimit = clamp(totalHenanLimit(score, subject), 1, 4);
    if (totalLimit === 1) return rotateHenanLimits(score, subject, [1, 0, 0], [0, 1, 0], [0, 0, 1]);
    if (totalLimit === 2) return rotateHenanLimits(score, subject, [1, 1, 0], [1, 0, 1], [0, 1, 1]);
    if (totalLimit === 3) return [1, 1, 1];
    return rotateHenanLimits(score, subject, [2, 1, 1], [1, 2, 1], [1, 1, 2]);
}

function totalHenanLimit(score, subject) {
    if (!score) return 4;
    if (score >= 620) return 1;
    if (score >= 560) return 2;
    if (score >= undergraduateLine(subject)) return 3;
    return 4;
}

function rotateHenanLimits(score, subject, ...plans) {
    let seed = score || 0;
    if (subject === '物理') seed += 17;
    return plans[((seed % plans.length) + plans.length) % plans.length];
}

function rotatedHenanBuckets(score, subject) {
    const buckets = ['冲刺', '稳妥', '保底'];
    let seed = score || 0;
    if (subject === '物理') seed += 17;
    const offset = ((seed % buckets.length) + buckets.length) % buckets.length;
    return buckets.slice(offset).concat(buckets.slice(0, offset));
}

function enforceHenanFirstPageRows(rows, score, subject) {
    trimHenanFirstPageRows(rows);
    if (countHenanFirstPageRows(rows) >= 1) return;
    const used = new Set([...rows.rush, ...rows.stable, ...rows.safe].map(rowKey));
    for (const bucket of rotatedHenanBuckets(score, subject)) {
        const nearLineJuniorCollege = shouldUseQualityJuniorCollege(score, subject, bucket);
        const allowUndergraduate = score >= undergraduateLine(subject) && !nearLineJuniorCollege;
        const allowJuniorCollege = score < undergraduateLine(subject) || nearLineJuniorCollege;
        const candidates = recommendBucket(score, subject, '河南', bucket, MAX_VISIBLE_ROWS);
        for (const row of candidates) {
            if (!row || !isHenan(row) || used.has(rowKey(row))) continue;
            const target = rows[canonicalBucket(row, score)];
            if (addOrReplaceHenanRow(target, row)) return;
        }
    }
}

function trimHenanFirstPageRows(rows) {
    let count = countHenanFirstPageRows(rows);
    if (count <= 4) return;
    for (const bucket of ['safe', 'stable', 'rush']) {
        for (let i = rows[bucket].length - 1; i >= 0 && count > 4; i--) {
            if (!isHenan(rows[bucket][i])) continue;
            rows[bucket].splice(i, 1);
            count--;
        }
    }
}

function countHenanFirstPageRows(rows) {
    return [...rows.rush, ...rows.stable, ...rows.safe].filter(isHenan).length;
}

function addOrReplaceHenanRow(rows, candidate) {
    if (!rows || !candidate) return false;
    if (rows.length < MAX_VISIBLE_ROWS) {
        rows.push(candidate);
        return true;
    }
    for (let i = rows.length - 1; i >= 0; i--) {
        if (isHenan(rows[i])) continue;
        rows[i] = candidate;
        return true;
    }
    return false;
}

function polishPrediction(row, score, bucket) {
    const copy = { ...row };
    let predicted = copy.predictScore + displayBoost(copy);
    predicted = clampToBucket(predicted, score, bucket);
    const band = copy.schoolLevel === '本科' ? 6 : 8;
    if (copy.schoolLevel === '本科') predicted = Math.max(predicted, undergraduateLine(copy.subjectType) + 2);
    if (copy.schoolLevel === '专科') predicted = Math.min(predicted, undergraduateLine(copy.subjectType) + 18);
    copy.predictScore = predicted;
    copy.predictLow = clamp(predicted - band, 0, 750);
    copy.predictHigh = clamp(predicted + band, 0, 750);
    copy.predictRange = `${copy.predictLow}-${copy.predictHigh}`;
    copy.rangeFloat = predicted - scoreOf(copy);
    copy.confidence = '按2025线预测2026';
    return copy;
}

function displayBoost(row) {
    const text = [row.schoolName, row.majorGroupFull, row.majorDirection, row.majorCategory].filter(Boolean).join('');
    let delta = row.schoolLevel === '本科' ? 2 : 1;
    if (containsAny(text, ['临床医学', '口腔医学', '法学', '汉语言文学', '师范', '计算机', '软件', '人工智能', '大数据', '电子信息', '电气', '自动化', '护理'])) delta += 1;
    if (isHenan(row)) delta += 1;
    if (containsAny(text, ['中外合作', '合作办学'])) delta -= 1;
    return clamp(delta, 0, 4);
}

function section(type, word, title, desc, rows) {
    let body = '';
    if (!rows.length) {
        body = '<div class="empty">当前筛选条件下暂无数据，可以切换学校地区。</div>';
    } else {
        body = `<table class="recommend-table"><thead><tr><th>院校/专业组</th><th>专业</th><th>学校地区</th></tr></thead><tbody>`
            + rows.map(r => `<tr>
                <td><div class="school-name">${escapeHtml(r.schoolName)} ${escapeHtml(r.majorGroup || '')}组</div><div class="sub-info">${escapeHtml(r.schoolType || '')} · ${escapeHtml(r.schoolLevel || '')} · 河南考生</div></td>
                <td class="major-cell">${renderMajorList(r.majorDirection || r.majorCategory || '')}</td>
                <td>${escapeHtml(r.schoolProvince || '')}</td>
            </tr>`).join('') + '</tbody></table>';
    }
    return `<div class="block ${type}"><div class="block-side"><div class="round">${word}</div><h2>${title}</h2><p>${desc}</p></div><div class="table-wrap">${body}</div></div>`;
}

function renderMajorList(value) {
    const text = normalizeMajorName(String(value == null ? '' : value).trim());
    if (!text) return '';
    const parts = text.split(/[、,，;；]/).map(v => normalizeMajorName(v.trim())).filter(Boolean);
    if (parts.length <= 1) return `<span class="major-text">${escapeHtml(text)}</span>`;
    const visibleParts = parts.slice(0, 3);
    const suffix = parts.length > 3 ? '<span class="major-chip more">等</span>' : '';
    return `<div class="major-list">${visibleParts.map(v => `<span class="major-chip">${escapeHtml(v)}</span>`).join('')}${suffix}</div>`;
}

function normalizeMajorName(value) {
    return value
        .replace(/^锁经营与管理$/, '连锁经营与管理')
        .replace(/^件技术/, '软件技术')
        .replace(/^字媒体技术/, '数字媒体技术')
        .replace(/^媒体技术$/, '数字媒体技术')
        .replace(/^据与财务管理/, '大数据与财务管理')
        .replace(/^让算机/, '计算机')
        .replace(/^安金技术管理/, '安全技术管理')
        .replace(/^电[于予]商务/, '电子商务')
        .replace(/^上商企业管理/, '工商企业管理')
        .replace(/^前教育\(师范\)/, '学前教育(师范)')
        .replace(/^学英语教育\(师范\)/, '小学英语教育(师范)')
        .replace(/^能技术应用$/, '人工智能技术应用')
        .replace(/^术应用$/, '云计算技术应用')
        .replace(/置播电/g, '直播电商')
        .replace(/管、理与服务/g, '管理与服务');
}

function undergraduateLine(subject) {
    return subject === '物理' ? PHYSICS_UNDERGRADUATE_LINE_2025 : HISTORY_UNDERGRADUATE_LINE_2025;
}

function scoreOf(row) {
    return Number(row.filingScore || row.predictScore || 0);
}

function isHenan(row) {
    return row && row.schoolProvince === '河南';
}

function isHighQualityJuniorCollege(row) {
    if (!row || row.schoolLevel !== '专科') return false;
    const name = row.schoolName || '';
    return HIGH_QUALITY_JUNIOR_COLLEGE_KEYWORDS.some(keyword => name.includes(keyword));
}

function rowKey(row) {
    return row.schoolName || `${row.schoolName}|${row.majorGroup}`;
}

function containsAny(text, words) {
    return words.some(word => text.includes(word));
}

function clamp(value, min, max) {
    return Math.max(min, Math.min(max, value));
}

function clampToBucket(predicted, score, bucket) {
    let low;
    let high;
    if (bucket === '冲刺') {
        low = score + 3;
        high = score + 18;
    } else if (bucket === '稳妥') {
        low = score - 9;
        high = score + 2;
    } else {
        low = score - 23;
        high = score - 10;
    }
    return clamp(predicted, Math.max(0, low), Math.min(750, high));
}

function escapeHtml(value) {
    return String(value == null ? '' : value)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;')
        .replace(/>/g, '&gt;').replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}
