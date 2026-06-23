let subjectType = '历史';
let selectedElectives = ['化学', '生物'];
let predictionLines = [];

const PREDICTION_ASSET_VERSION = '2026062310';
const MAX_VISIBLE_ROWS = 6;
const QUERY_LIMIT = 36;
const PLAN_COUNT = 6;
const HISTORY_UNDERGRADUATE_LINE_2025 = 471;
const PHYSICS_UNDERGRADUATE_LINE_2025 = 427;
const NEAR_UNDERGRADUATE_MARGIN = 20;
const HENAN_LOCAL_RECOMMEND_RATIO = 0.3;
const STATIC_LOGIN_USER = 'admin';
const STATIC_LOGIN_HASH = '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9';
const STATIC_LOGIN_KEY = 'gaokao_pages_login_ok';
const STATIC_LOGIN_ACCOUNT_KEY = 'gaokao_pages_login_account';
const STATIC_LOGIN_SESSIONS_PREFIX = 'gaokao_pages_login_sessions:';
const STATIC_LOGIN_MAX_ACTIVE_SESSIONS = 2;
const STATIC_LOGIN_SESSION_TTL = 2 * 60 * 1000;
const STATIC_LOGIN_HEARTBEAT_MS = 10 * 1000;
const STATIC_LOGIN_FALLBACK_USERS = [
    { username: STATIC_LOGIN_USER, passwordHash: STATIC_LOGIN_HASH }
];
const STATIC_LOGIN_INSTANCE_ID = createStaticLoginInstanceId();
const MAJOR_TEXT_FIXES = new Map([
    ['濮阳医学高等专科学校|历史|102', '中医学(大学生村医免费培养计划)'],
    ['漯河医学高等专科学校|历史|102', '临床医学(大学生村医免费培养计划)'],
    ['南阳医学高等专科学校|历史|103', '临床医学(大学生村医免费培养计划)'],
    ['南阳医学高等专科学校|历史|104', '中医学(大学生村医免费培养计划)']
]);
const SPECIAL_JUNIOR_COLLEGE_DISPLAYS = new Map([
    ['河南理工大学', '河南理工大学（民政学院·专科批）'],
    ['平顶山学院', '平顶山学院（医药科技学院·专科批）']
]);
const UNDERGRAD_LIKE_SCHOOL_WORDS = ['大学', '学院'];
const VOCATIONAL_SCHOOL_WORDS = [
    '职业', '技术', '高等专科', '专科学校', '职工大学',
    '开放大学', '广播电视大学', '技师', '干部'
];
const BASE_PROVINCE = '河南';
const PROVINCE_SCORE_SCALE = 1;
const VALID_ADMISSION_MODES = new Set(['group', 'majorSchool', 'traditional']);
const SCORE_LINE_TYPES = ['line', 'special', 'junior'];
const SUBJECT_TYPES = ['历史', '物理'];
const NEW_3_3_PROVINCES = new Set(['北京', '天津', '上海', '浙江', '山东', '海南']);
const TRADITIONAL_PROVINCES = new Set(['新疆', '西藏']);
const CONTROL_LINE_SOURCE = {
    year: 2025,
    title: '各省教育考试院/阳光高考公开录取控制线',
    url: 'https://gaokao.chsi.com.cn/z/gkbmfslq/pcx.jsp',
    summaryUrl: 'https://app.gaokaozhitongche.com/news/h/bOKqoP52'
};
const JUNIOR_LINE_REFERENCE_2024 = new Set([]);
const SCHOOL_PROVINCES = [
    '河南', '北京', '天津', '河北', '山西', '内蒙古',
    '辽宁', '吉林', '黑龙江', '上海', '江苏',
    '浙江', '安徽', '福建', '江西', '山东',
    '湖北', '湖南', '广东', '广西',
    '海南', '重庆', '四川', '贵州', '云南',
    '西藏', '陕西', '甘肃', '青海', '宁夏', '新疆'
];
const PROVINCE_ADMISSION_RULES = {
    河南: { mode: 'group', label: '院校专业组', line: { 历史: 471, 物理: 427 }, special: { 历史: 552, 物理: 535 }, junior: { 历史: 185, 物理: 185 } },
    北京: { mode: 'group', label: '院校专业组', line: { 历史: 430, 物理: 430 }, special: { 历史: 519, 物理: 519 }, junior: { 历史: 120, 物理: 120 } },
    天津: { mode: 'group', label: '院校专业组', line: { 历史: 458, 物理: 458 }, special: { 历史: 547, 物理: 547 }, junior: { 历史: 160, 物理: 160 }, lineYear: 2026, specialYear: 2026, juniorYear: 2025 },
    河北: { mode: 'majorSchool', label: '专业（类）+院校', line: { 历史: 477, 物理: 459 }, special: { 历史: 527, 物理: 499 }, junior: { 历史: 200, 物理: 200 } },
    山西: { mode: 'group', label: '院校专业组', line: { 历史: 443, 物理: 419 }, special: { 历史: 534, 物理: 507 }, junior: { 历史: 180, 物理: 200 } },
    内蒙古: { mode: 'group', label: '院校专业组', line: { 历史: 418, 物理: 375 }, special: { 历史: 523, 物理: 487 }, junior: { 历史: 160, 物理: 160 } },
    辽宁: { mode: 'majorSchool', label: '专业（类）+院校', line: { 历史: 437, 物理: 367 }, special: { 历史: 522, 物理: 515 }, junior: { 历史: 150, 物理: 150 } },
    吉林: { mode: 'group', label: '院校专业组', line: { 历史: 384, 物理: 340 }, special: { 历史: 493, 物理: 479 }, junior: { 历史: 160, 物理: 160 } },
    黑龙江: { mode: 'group', label: '院校专业组', line: { 历史: 405, 物理: 360 }, special: { 历史: 480, 物理: 472 }, junior: { 历史: 160, 物理: 160 } },
    上海: { mode: 'group', label: '院校专业组', line: { 历史: 403, 物理: 403 }, special: { 历史: 504, 物理: 504 }, junior: { 历史: 100, 物理: 100 }, lineYear: 2026, specialYear: 2026, juniorYear: 2025 },
    江苏: { mode: 'group', label: '院校专业组', line: { 历史: 482, 物理: 463 }, special: { 历史: 537, 物理: 519 }, junior: { 历史: 220, 物理: 220 } },
    浙江: { mode: 'majorSchool', label: '专业（类）+院校', line: { 历史: 490, 物理: 490 }, special: { 历史: 592, 物理: 592 }, junior: { 历史: 268, 物理: 268 } },
    安徽: { mode: 'group', label: '院校专业组', line: { 历史: 477, 物理: 461 }, special: { 历史: 515, 物理: 514 }, junior: { 历史: 200, 物理: 200 } },
    福建: { mode: 'group', juniorMode: 'majorSchool', label: '院校专业组', line: { 历史: 450, 物理: 441 }, special: { 历史: 531, 物理: 520 }, junior: { 历史: 235, 物理: 235 } },
    江西: { mode: 'group', label: '院校专业组', line: { 历史: 486, 物理: 429 }, special: { 历史: 539, 物理: 505 }, junior: { 历史: 290, 物理: 240 } },
    山东: { mode: 'majorSchool', label: '专业（类）+院校', line: { 历史: 441, 物理: 441 }, special: { 历史: 521, 物理: 521 }, junior: { 历史: 150, 物理: 150 } },
    湖北: { mode: 'group', label: '院校专业组', line: { 历史: 442, 物理: 426 }, special: { 历史: 536, 物理: 516 }, junior: { 历史: 200, 物理: 200 } },
    湖南: { mode: 'group', label: '院校专业组', line: { 历史: 446, 物理: 405 }, special: { 历史: 503, 物理: 476 }, junior: { 历史: 200, 物理: 200 } },
    广东: { mode: 'group', label: '院校专业组', line: { 历史: 464, 物理: 436 }, special: { 历史: 557, 物理: 534 }, junior: { 历史: 215, 物理: 200 } },
    广西: { mode: 'group', label: '院校专业组', line: { 历史: 402, 物理: 370 }, special: { 历史: 518, 物理: 495 }, junior: { 历史: 200, 物理: 200 } },
    海南: { mode: 'group', label: '院校专业组', line: { 历史: 480, 物理: 480 }, special: { 历史: 568, 物理: 568 }, junior: { 历史: 255, 物理: 255 } },
    重庆: { mode: 'majorSchool', label: '专业（类）+院校', line: { 历史: 438, 物理: 425 }, special: { 历史: 515, 物理: 498 }, junior: { 历史: 180, 物理: 180 } },
    四川: { mode: 'group', label: '院校专业组', line: { 历史: 467, 物理: 438 }, special: { 历史: 533, 物理: 518 }, junior: { 历史: 150, 物理: 150 } },
    贵州: { mode: 'majorSchool', label: '专业（类）+院校', line: { 历史: 458, 物理: 387 }, special: { 历史: 517, 物理: 483 }, junior: { 历史: 180, 物理: 180 } },
    云南: { mode: 'group', label: '院校专业组', line: { 历史: 465, 物理: 430 }, special: { 历史: 555, 物理: 495 }, junior: { 历史: 180, 物理: 180 } },
    西藏: { mode: 'traditional', label: '传统文理院校志愿', line: { 历史: 315, 物理: 305 }, special: { 历史: 410, 物理: 400 }, junior: { 历史: 255, 物理: 222 } },
    陕西: { mode: 'group', label: '院校专业组', line: { 历史: 414, 物理: 394 }, special: { 历史: 497, 物理: 473 }, junior: { 历史: 200, 物理: 200 } },
    甘肃: { mode: 'group', label: '院校专业组', line: { 历史: 412, 物理: 374 }, special: { 历史: 500, 物理: 475 }, junior: { 历史: 160, 物理: 180 } },
    青海: { mode: 'majorSchool', label: '专业（类）+院校', line: { 历史: 405, 物理: 350 }, special: { 历史: 450, 物理: 420 }, junior: { 历史: 150, 物理: 150 } },
    宁夏: { mode: 'group', label: '院校专业组', line: { 历史: 404, 物理: 372 }, special: { 历史: 482, 物理: 441 }, junior: { 历史: 150, 物理: 150 } },
    新疆: { mode: 'traditional', label: '传统文理院校志愿', line: { 历史: 330, 物理: 280 }, special: { 历史: 451, 物理: 421 }, junior: { 历史: 140, 物理: 140 } }
};
const SCHOOL_PROVINCE_COMPETITION_DELTA = {
    北京: 14,
    上海: 12,
    江苏: 8,
    浙江: 8,
    广东: 7,
    天津: 5,
    湖北: 4,
    山东: 4,
    福建: 3,
    重庆: 2,
    陕西: 2,
    四川: 1,
    湖南: 1,
    河南: 0,
    河北: -1,
    安徽: -1,
    江西: -2,
    辽宁: -3,
    山西: -3,
    广西: -4,
    云南: -4,
    吉林: -5,
    黑龙江: -5,
    内蒙古: -6,
    贵州: -6,
    甘肃: -7,
    宁夏: -7,
    青海: -8,
    新疆: -8,
    西藏: -10
};
const LOCAL_PROVINCE_ADVANTAGE = {
    北京: -3,
    上海: -3,
    天津: -4,
    江苏: -4,
    浙江: -4,
    山东: -5,
    广东: -5,
    河南: -7,
    河北: -7,
    山西: -7,
    安徽: -7,
    江西: -7,
    湖北: -6,
    湖南: -6,
    广西: -8,
    重庆: -6,
    四川: -6,
    贵州: -8,
    云南: -8,
    陕西: -6,
    甘肃: -9,
    青海: -9,
    宁夏: -9,
    新疆: -9,
    西藏: -10
};
const ELECTIVE_DEFAULTS_BY_SUBJECT = {
    历史: ['化学', '生物'],
    物理: ['化学', '生物'],
    综合: ['化学', '生物'],
    文科: ['化学', '生物'],
    理科: ['化学', '生物']
};
const RESELECT_OPTIONS_3_1_2 = ['化学', '生物', '政治', '地理'];
const ELECTIVE_OPTIONS_3_3 = ['物理', '化学', '生物', '政治', '历史', '地理'];
const ELECTIVE_OPTIONS = ELECTIVE_OPTIONS_3_3;
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
let manualRecommendStarted = false;
let staticLoginUsersPromise = null;
let staticLoginHeartbeat = null;
let staticLoginAutoReleaseBound = false;

setupStaticLogin();

document.querySelectorAll('.subject').forEach(btn => {
    btn.addEventListener('click', () => {
        subjectType = btn.dataset.value;
        updateSubjectControls({ fillMissing: true });
        resetPlanAndRender();
    });
});

document.querySelectorAll('.elective').forEach(btn => {
    btn.addEventListener('click', () => {
        toggleElective(btn.dataset.value);
        updateSubjectControls({ fillMissing: false });
        resetPlanAndRender();
    });
});

$('recommendBtn').addEventListener('click', () => {
    const criteriaKey = getCurrentCriteriaKey();
    if (criteriaKey !== lastCriteriaKey) {
        recommendNonce = 0;
        lastCriteriaKey = criteriaKey;
        manualRecommendStarted = true;
    } else if (manualRecommendStarted) {
        recommendNonce = (recommendNonce + 1) % PLAN_COUNT;
    } else {
        recommendNonce = 0;
        manualRecommendStarted = true;
    }
    renderRecommend();
});

$('schoolProvince').addEventListener('change', () => {
    syncSubjectControlsForProvince();
    resetPlanAndRender();
});
$('score').addEventListener('change', resetPlanAndRender);

function setupStaticLogin() {
    const loginView = $('loginView');
    const appView = $('appView');
    if (!loginView || !appView) {
        bootApp();
        return;
    }

    const setLoginError = message => {
        const error = $('loginError');
        if (!error) return;
        error.innerText = message || '';
        error.hidden = !message;
    };
    const showLogin = message => {
        document.body.classList.add('locked');
        loginView.hidden = false;
        appView.hidden = true;
        const pass = $('loginPass');
        if (pass) pass.value = '';
        setLoginError(message || '');
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
        const username = $('loginUser').value.trim();
        const ok = await verifyStaticLogin(username, $('loginPass').value);
        if (!ok) {
            setLoginError('账号或密码错误，请重新输入。');
            return;
        }
        if (!hasStaticLoginCapacity(username)) {
            setLoginError('该账号已达到 2 个设备在线，请先退出其中一个后再登录。');
            return;
        }
        startStaticLoginSession(username);
        setLoginError('');
        showApp();
    });
    $('staticLogout').addEventListener('click', () => {
        endStaticLoginSession();
        showLogin();
    });

    const storedUsername = sessionStorage.getItem(STATIC_LOGIN_ACCOUNT_KEY);
    if (sessionStorage.getItem(STATIC_LOGIN_KEY) === '1' && storedUsername && hasStaticLoginCapacity(storedUsername)) {
        startStaticLoginSession(storedUsername);
        showApp();
    } else {
        endStaticLoginSession(false);
        showLogin();
    }
    bindStaticLoginAutoRelease(showLogin);
}

function startStaticLoginSession(username) {
    const normalizedUsername = normalizeStaticUsername(username);
    sessionStorage.setItem(STATIC_LOGIN_KEY, '1');
    sessionStorage.setItem(STATIC_LOGIN_ACCOUNT_KEY, normalizedUsername);
    refreshStaticLoginSession(normalizedUsername);
    startStaticLoginHeartbeat(normalizedUsername);
}

function endStaticLoginSession(removeSession = true) {
    const username = sessionStorage.getItem(STATIC_LOGIN_ACCOUNT_KEY);
    if (removeSession && username) releaseStaticLoginSession(username);
    clearStaticLoginHeartbeat();
    sessionStorage.removeItem(STATIC_LOGIN_KEY);
    sessionStorage.removeItem(STATIC_LOGIN_ACCOUNT_KEY);
}

function hasStaticLoginCapacity(username) {
    const normalizedUsername = normalizeStaticUsername(username);
    const sessions = readStaticLoginSessions(normalizedUsername);
    if (sessions.some(session => session.sessionId === getStaticLoginSessionId())) return true;
    return sessions.length < STATIC_LOGIN_MAX_ACTIVE_SESSIONS;
}

function refreshStaticLoginSession(username) {
    const normalizedUsername = normalizeStaticUsername(username);
    if (!normalizedUsername) return false;
    const sessions = readStaticLoginSessions(normalizedUsername);
    const currentSessionId = getStaticLoginSessionId();
    const otherSessions = sessions.filter(session => session.sessionId !== currentSessionId);
    if (otherSessions.length >= STATIC_LOGIN_MAX_ACTIVE_SESSIONS) return false;
    const now = Date.now();
    writeStaticLoginSessions(normalizedUsername, otherSessions.concat({
        username: normalizedUsername,
        sessionId: currentSessionId,
        updatedAt: now,
        expiresAt: now + STATIC_LOGIN_SESSION_TTL
    }));
    return true;
}

function releaseStaticLoginSession(username) {
    const normalizedUsername = normalizeStaticUsername(username);
    const currentSessionId = getStaticLoginSessionId();
    const sessions = readStaticLoginSessions(normalizedUsername)
        .filter(session => session.sessionId !== currentSessionId);
    writeStaticLoginSessions(normalizedUsername, sessions);
}

function readStaticLoginSessions(username) {
    const normalizedUsername = normalizeStaticUsername(username);
    if (!normalizedUsername) return [];
    try {
        const value = localStorage.getItem(staticAccountSessionsKey(normalizedUsername));
        if (!value) return [];
        const parsed = JSON.parse(value);
        const sessions = Array.isArray(parsed) ? parsed : [];
        const now = Date.now();
        const activeSessions = sessions.filter(session => session && session.sessionId && Number(session.expiresAt || 0) > now);
        if (activeSessions.length !== sessions.length) {
            writeStaticLoginSessions(normalizedUsername, activeSessions);
        }
        return activeSessions;
    } catch (error) {
        localStorage.removeItem(staticAccountSessionsKey(normalizedUsername));
        return [];
    }
}

function writeStaticLoginSessions(username, sessions) {
    const normalizedUsername = normalizeStaticUsername(username);
    if (!normalizedUsername) return;
    if (!sessions.length) {
        localStorage.removeItem(staticAccountSessionsKey(normalizedUsername));
        return;
    }
    localStorage.setItem(staticAccountSessionsKey(normalizedUsername), JSON.stringify(sessions));
}

function startStaticLoginHeartbeat(username) {
    clearStaticLoginHeartbeat();
    staticLoginHeartbeat = setInterval(() => refreshStaticLoginSession(username), STATIC_LOGIN_HEARTBEAT_MS);
}

function clearStaticLoginHeartbeat() {
    if (!staticLoginHeartbeat) return;
    clearInterval(staticLoginHeartbeat);
    staticLoginHeartbeat = null;
}

function getStaticLoginSessionId() {
    return STATIC_LOGIN_INSTANCE_ID;
}

function createStaticLoginInstanceId() {
    return window.crypto && window.crypto.randomUUID
        ? window.crypto.randomUUID()
        : `${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

function normalizeStaticUsername(username) {
    return String(username || '').trim().toLowerCase();
}

function staticAccountSessionsKey(username) {
    return `${STATIC_LOGIN_SESSIONS_PREFIX}${normalizeStaticUsername(username)}`;
}

function bindStaticLoginAutoRelease(showLogin) {
    if (staticLoginAutoReleaseBound) return;
    staticLoginAutoReleaseBound = true;
    const releaseCurrentPageLogin = () => endStaticLoginSession();
    window.addEventListener('pagehide', releaseCurrentPageLogin);
    window.addEventListener('beforeunload', releaseCurrentPageLogin);
    window.addEventListener('pageshow', event => {
        if (event.persisted && sessionStorage.getItem(STATIC_LOGIN_KEY) !== '1') showLogin();
    });
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
    try {
        validateProvinceRuleCoverage();
        renderProvinceOptions();
        syncSubjectControlsForProvince();
        const response = await fetch(`./assets/prediction-lines.json?v=${PREDICTION_ASSET_VERSION}`, { cache: 'no-store' });
        if (!response.ok) throw new Error('数据文件读取失败');
        predictionLines = await response.json();
        resetPlanAndRender();
    } catch (err) {
        $('resultArea').innerHTML = `<div class="empty">加载失败：${escapeHtml(err.message)}</div>`;
    }
}

function renderProvinceOptions() {
    $('schoolProvince').innerHTML = SCHOOL_PROVINCES
        .map(p => `<option value="${escapeHtml(p)}">${escapeHtml(displayProvinceName(p))}</option>`)
        .join('');
    $('schoolProvince').value = BASE_PROVINCE;
}

function syncSubjectControlsForProvince() {
    const province = $('schoolProvince') ? $('schoolProvince').value || BASE_PROVINCE : BASE_PROVINCE;
    const options = subjectOptionsForProvince(province);
    if (!options.some(option => option.value === subjectType)) {
        subjectType = options[0].value;
    }
    const tabs = document.querySelector('.subject-tabs');
    if (tabs) {
        tabs.innerHTML = options.map(option =>
            `<button class="subject ${option.value === subjectType ? 'active' : ''}" data-value="${escapeHtml(option.value)}" type="button">${escapeHtml(option.label)}</button>`
        ).join('');
        bindSubjectButtons();
    }
    const electiveTabs = document.querySelector('.elective-tabs');
    if (electiveTabs) {
        electiveTabs.innerHTML = currentElectiveOptions().map(item =>
            `<button class="elective ${selectedElectives.includes(item) ? 'active' : ''}" data-value="${escapeHtml(item)}" type="button">${escapeHtml(item)}</button>`
        ).join('');
        bindElectiveButtons();
    }
    const isTraditional = provinceExamMode(province) === 'traditional';
    document.querySelectorAll('.elective-tabs,.elective-label').forEach(el => {
        el.style.display = isTraditional ? 'none' : '';
    });
    updateSubjectControls({ fillMissing: true });
}

function bindSubjectButtons() {
    document.querySelectorAll('.subject').forEach(btn => {
        btn.addEventListener('click', () => {
            subjectType = btn.dataset.value;
            updateSubjectControls({ fillMissing: true });
            resetPlanAndRender();
        });
    });
}

function bindElectiveButtons() {
    document.querySelectorAll('.elective').forEach(btn => {
        btn.addEventListener('click', () => {
            toggleElective(btn.dataset.value);
            updateSubjectControls({ fillMissing: false });
            resetPlanAndRender();
        });
    });
}

function provinceExamMode(province) {
    if (TRADITIONAL_PROVINCES.has(province)) return 'traditional';
    if (NEW_3_3_PROVINCES.has(province)) return 'new33';
    return 'new312';
}

function subjectOptionsForProvince(province) {
    const examMode = provinceExamMode(province);
    if (examMode === 'traditional') {
        return [
            { value: '文科', label: '文科' },
            { value: '理科', label: '理科' }
        ];
    }
    if (examMode === 'new33') {
        return [{ value: '综合', label: '综合类' }];
    }
    return [
        { value: '历史', label: '历史组' },
        { value: '物理', label: '物理组' }
    ];
}

function updateSubjectControls({ fillMissing = true } = {}) {
    normalizeSelectedElectives(fillMissing);
    document.querySelectorAll('.subject').forEach(btn => {
        btn.classList.toggle('active', btn.dataset.value === subjectType);
    });
    document.querySelectorAll('.elective').forEach(btn => {
        btn.classList.toggle('active', selectedElectives.includes(btn.dataset.value));
    });
}

function normalizeSelectedElectives(fillMissing = true) {
    const options = currentElectiveOptions();
    const unique = [];
    for (const item of selectedElectives) {
        if (options.includes(item) && !unique.includes(item)) unique.push(item);
    }
    if (!fillMissing) {
        selectedElectives = unique.slice(0, 2);
        return;
    }
    const defaults = ELECTIVE_DEFAULTS_BY_SUBJECT[subjectType] || ELECTIVE_OPTIONS.slice(0, 2);
    for (const item of defaults) {
        if (unique.length >= 2) break;
        if (options.includes(item) && !unique.includes(item)) unique.push(item);
    }
    selectedElectives = unique.slice(0, 2);
}

function toggleElective(value) {
    if (!currentElectiveOptions().includes(value)) return;
    normalizeSelectedElectives(true);
    if (selectedElectives.includes(value)) return;
    selectedElectives = selectedElectives.slice(-1).concat(value);
    normalizeSelectedElectives(true);
}

function currentElectiveOptions() {
    const province = $('schoolProvince') ? $('schoolProvince').value || BASE_PROVINCE : BASE_PROVINCE;
    if (provinceExamMode(province) === 'traditional') return [];
    return RESELECT_OPTIONS_3_1_2;
}

function selectedElectiveText() {
    normalizeSelectedElectives(false);
    return selectedElectives.join('、');
}

function displayProvinceName(province) {
    return province || BASE_PROVINCE;
}

function scoreLineSubject(subject) {
    if (subject === '物理' || subject === '理科') return '物理';
    return '历史';
}

function recommendationSubject(subject, province) {
    if (subject === '物理' || subject === '理科') return '物理';
    if (subject === '综合') {
        const electives = selectedElectiveText();
        return /物理|化学|生物/.test(electives) ? '物理' : '历史';
    }
    return '历史';
}

function displaySubjectName(subject, province = BASE_PROVINCE) {
    if (subject === '综合') return '综合类';
    if (subject === '文科' || subject === '理科') return subject;
    return `${subject}组`;
}

function subjectSummaryLabel(subject, province = BASE_PROVINCE) {
    const examMode = provinceExamMode(province);
    if (examMode === 'traditional') return displaySubjectName(subject, province);
    if (examMode === 'new33') return `${displaySubjectName(subject, province)}（选科：${selectedElectiveText()}）`;
    return `${displaySubjectName(subject, province)}（${selectedElectiveText()}）`;
}

function validateProvinceRuleCoverage() {
    const errors = [];
    for (const province of SCHOOL_PROVINCES) {
        const rule = PROVINCE_ADMISSION_RULES[province];
        if (!rule) {
            errors.push(`${province}缺少录取规则`);
            continue;
        }
        if (!VALID_ADMISSION_MODES.has(rule.mode)) errors.push(`${province}录取模式无效`);
        if (rule.juniorMode && !VALID_ADMISSION_MODES.has(rule.juniorMode)) errors.push(`${province}专科录取模式无效`);
        if (!rule.label) errors.push(`${province}缺少规则名称`);
        for (const lineType of SCORE_LINE_TYPES) {
            if (!rule[lineType]) {
                errors.push(`${province}缺少${lineType}分数线`);
                continue;
            }
            for (const subject of SUBJECT_TYPES) {
                const value = Number(rule[lineType][subject]);
                if (!Number.isFinite(value) || value <= 0 || value > 750) {
                    errors.push(`${province}${subject}${lineType}分数线无效`);
                }
            }
        }
    }
    const extraRules = Object.keys(PROVINCE_ADMISSION_RULES)
        .filter(province => !SCHOOL_PROVINCES.includes(province));
    if (extraRules.length) errors.push(`存在未展示省份规则：${extraRules.join('、')}`);
    if (errors.length) throw new Error(`省份录取规则配置不完整：${errors.join('；')}`);
}

function provinceRule(province) {
    const currentProvince = province || BASE_PROVINCE;
    const rule = PROVINCE_ADMISSION_RULES[currentProvince];
    if (!rule) throw new Error(`${currentProvince}缺少本省录取规则`);
    return rule;
}

function provinceLine(subject, province) {
    const item = provinceRule(province).line;
    return scoreLineSubject(subject) === '物理' ? item.物理 : item.历史;
}

function provinceSpecialLine(subject, province) {
    const item = provinceRule(province).special || provinceRule(province).line;
    return scoreLineSubject(subject) === '物理' ? item.物理 : item.历史;
}

function provinceJuniorLine(subject, province) {
    const item = provinceRule(province).junior || { 历史: 150, 物理: 150 };
    return scoreLineSubject(subject) === '物理' ? item.物理 : item.历史;
}

function toProvinceScore(baseScore, subject, candidateProvince) {
    if (!candidateProvince || candidateProvince === BASE_PROVINCE) return clamp(Math.round(Number(baseScore || 0)), 100, 750);
    const baseLine = provinceLine(subject, BASE_PROVINCE);
    const selectedLine = provinceLine(subject, candidateProvince);
    const translated = selectedLine + ((Number(baseScore || 0) - baseLine) / PROVINCE_SCORE_SCALE);
    return clamp(Math.round(translated), 100, 750);
}

function ruleModeForRow(row) {
    const rule = provinceRule(row.candidateProvince || BASE_PROVINCE);
    return row.schoolLevel === '专科' && rule.juniorMode ? rule.juniorMode : rule.mode;
}

function provinceRuleMeta(subject, candidateProvince) {
    const rule = provinceRule(candidateProvince);
    const line = provinceLine(subject, candidateProvince);
    const special = provinceSpecialLine(subject, candidateProvince);
    const junior = provinceJuniorLine(subject, candidateProvince);
    const lineYear = scoreLineYear(rule, 'line');
    const specialYear = scoreLineYear(rule, 'special');
    const juniorYear = JUNIOR_LINE_REFERENCE_2024.has(candidateProvince)
        ? 2024
        : scoreLineYear(rule, 'junior');
    const newestYear = Math.max(lineYear, specialYear, juniorYear);
    const lineText = scoreLineText('本科线', line, lineYear);
    const specialText = scoreLineText('特控线', special, specialYear);
    const juniorText = scoreLineText('专科线', junior, juniorYear, newestYear);
    const sourceNote = `，控制线来源：${scoreLineSourceYears(lineYear, specialYear, juniorYear)}公开数据`;
    if (candidateProvince === BASE_PROVINCE) {
        return `《招生之友》院校专业组口径，${lineText}，${specialText}，${juniorText}${sourceNote}`;
    }
    return `${rule.label}口径，${lineText}，${specialText}，${juniorText}${sourceNote}`;
}

function scoreLineYear(rule, type) {
    return Number(rule[`${type}Year`] || rule.sourceYear || CONTROL_LINE_SOURCE.year);
}

function scoreLineText(label, value, year, referenceYear = CONTROL_LINE_SOURCE.year) {
    const suffix = year === CONTROL_LINE_SOURCE.year && year === referenceYear
        ? ''
        : (year < referenceYear ? `（暂按${year}公开线参考）` : `（${year}）`);
    return `${label} ${value} 分${suffix}`;
}

function scoreLineSourceYears(...years) {
    return Array.from(new Set(years.filter(Boolean)))
        .sort((a, b) => a - b)
        .map(year => `${year}年`)
        .join('、');
}

function resetPlanAndRender() {
    recommendNonce = 0;
    lastCriteriaKey = getCurrentCriteriaKey();
    manualRecommendStarted = false;
    renderRecommend();
}

function getCurrentCriteriaKey() {
    const score = Number($('score').value || 500);
    const candidateProvince = $('schoolProvince').value || BASE_PROVINCE;
    return `${score}|${subjectType}|${selectedElectiveText()}|${candidateProvince}`;
}

function renderRecommend() {
    if (!predictionLines.length) return;

    const score = Number($('score').value || 500);
    const candidateProvince = $('schoolProvince').value || BASE_PROVINCE;
    const criteriaKey = `${score}|${subjectType}|${selectedElectiveText()}|${candidateProvince}`;
    if (criteriaKey !== lastCriteriaKey) {
        recommendNonce = 0;
        lastCriteriaKey = criteriaKey;
        manualRecommendStarted = false;
    }
    $('tagSubject').innerText = displaySubjectName(subjectType, candidateProvince);
    $('tagElective').innerText = provinceExamMode(candidateProvince) === 'traditional' ? '不涉及' : selectedElectiveText();
    $('tagProvince').innerText = displayProvinceName(candidateProvince);

    const dataSubject = recommendationSubject(subjectType, candidateProvince);
    let rows = recommend(score, dataSubject, candidateProvince);
    if (shouldResetRecommendationBatch(rows, candidateProvince)) {
        recommendNonce = 0;
        rows = recommend(score, dataSubject, candidateProvince);
    }
    renderSummaryText(score, candidateProvince);
    $('resultArea').innerHTML = section('rush', '冲', '冲刺推荐', '适合略高于当前分数的院校', rows.rush)
        + section('stable', '稳', '稳妥推荐', '适合重点考虑的匹配院校', rows.stable)
        + section('safe', '保', '保底推荐', '适合保底填报的院校', rows.safe);
}

function renderSummaryText(score, candidateProvince) {
    $('summaryText').innerText = `第 ${recommendNonce + 1} 批`;
}

function hasRecommendationRows(rows) {
    return Boolean(rows && ['rush', 'stable', 'safe'].some(bucket => Array.isArray(rows[bucket]) && rows[bucket].length));
}

function shouldResetRecommendationBatch(rows, schoolProvince) {
    if (recommendNonce <= 0) return false;
    return !hasRecommendationRows(rows);
}

function hasMeaningfulRegionalBatch(rows) {
    const counts = ['rush', 'stable', 'safe'].map(bucket => Array.isArray(rows[bucket]) ? rows[bucket].length : 0);
    return counts.every(count => count > 0) && counts.reduce((total, count) => total + count, 0) >= MAX_VISIBLE_ROWS * 2;
}

function recommend(score, subject, schoolProvince) {
    const limits = henanLimitsByBucket(score, subject, schoolProvince);
    const rushCandidates = recommendBucket(score, subject, schoolProvince, '冲刺', limits[0]);
    const stableCandidates = recommendBucket(score, subject, schoolProvince, '稳妥', limits[1]);
    const safeCandidates = recommendBucket(score, subject, schoolProvince, '保底', limits[2]);
    const previousBatchKeys = previousRecommendationKeys(score, subject, schoolProvince,
        rushCandidates, stableCandidates, safeCandidates);

    const used = new Map();
    const rushRows = takeUniqueRows(varyCandidateOrder(rushCandidates, score, subject, schoolProvince, '冲刺'), used, previousBatchKeys);
    const allRegions = true;
    let stableRows;
    let safeRows;
    if (allRegions) {
        stableRows = takeUniqueRows(varyCandidateOrder(stableCandidates, score, subject, schoolProvince, '稳妥'), used, previousBatchKeys);
        safeRows = takeUniqueRows(varyCandidateOrder(safeCandidates, score, subject, schoolProvince, '保底'), used, previousBatchKeys);
    } else {
        stableRows = takeUniqueRows(varyCandidateOrder(stableCandidates, score, subject, schoolProvince, '稳妥'), used, previousBatchKeys);
        safeRows = takeUniqueRows(varyCandidateOrder(safeCandidates, score, subject, schoolProvince, '保底'), used, previousBatchKeys);
    }
    rebalanceScarceHighScoreRows(score, subject, rushRows, stableRows, safeRows, schoolProvince);
    const canonicalRows = canonicalizeRows(score, rushRows, stableRows, safeRows,
        rushCandidates, stableCandidates, safeCandidates, previousBatchKeys);
    return {
        rush: canonicalRows.rush.map(row => polishPrediction(row, score, '冲刺')),
        stable: canonicalRows.stable.map(row => polishPrediction(row, score, '稳妥')),
        safe: canonicalRows.safe.map(row => polishPrediction(row, score, '保底'))
    };
}

function previousRecommendationKeys(score, subject, schoolProvince, rushCandidates, stableCandidates, safeCandidates) {
    const keys = new Set();
    const currentNonce = recommendNonce;
    for (let plan = 0; plan < currentNonce; plan++) {
        recommendNonce = plan;
        const used = new Map();
        const rushRows = takeUniqueRows(varyCandidateOrder(rushCandidates, score, subject, schoolProvince, '冲刺'), used, keys);
        const stableRows = takeUniqueRows(varyCandidateOrder(stableCandidates, score, subject, schoolProvince, '稳妥'), used, keys);
        const safeRows = takeUniqueRows(varyCandidateOrder(safeCandidates, score, subject, schoolProvince, '保底'), used, keys);
        rebalanceScarceHighScoreRows(score, subject, rushRows, stableRows, safeRows, schoolProvince);
        const canonicalRows = canonicalizeRows(score, rushRows, stableRows, safeRows,
            rushCandidates, stableCandidates, safeCandidates, keys);
        [...canonicalRows.rush, ...canonicalRows.stable, ...canonicalRows.safe].forEach(row => keys.add(rowKey(row)));
    }
    recommendNonce = currentNonce;
    return keys;
}

function recommendBucket(score, subject, schoolProvince, bucket, preferredHenanCount) {
    const allRegions = true;
    const nearLineJuniorCollege = shouldUseQualityJuniorCollege(score, subject, bucket, schoolProvince);
    const allowUndergraduate = score >= undergraduateLine(subject, schoolProvince) && !nearLineJuniorCollege;
    const allowJuniorCollege = score < undergraduateLine(subject, schoolProvince) || nearLineJuniorCollege;
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
    if (preferredHenanCount <= 0) return pool;
    let henan = queryCandidatesWithFallback(score, subject, schoolProvince, bucket, allowUndergraduate,
        allowJuniorCollege, nearLineJuniorCollege, Math.max(preferredHenanCount + MAX_VISIBLE_ROWS, 9), BASE_PROVINCE);
    if (bucket === '冲刺' && henan.length < preferredHenanCount && allowUndergraduate && !allowJuniorCollege) {
        const henanReserve = queryCandidatesWithFallback(score, subject, schoolProvince, '冲刺高分兜底',
            allowUndergraduate, allowJuniorCollege, nearLineJuniorCollege, Math.max(preferredHenanCount + MAX_VISIBLE_ROWS, 9), BASE_PROVINCE);
        henan = mergeFallbackRows(henan, henanReserve, Math.max(preferredHenanCount + MAX_VISIBLE_ROWS, 9));
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

function rebalanceScarceHighScoreRows(score, subject, rushRows, stableRows, safeRows, candidateProvince = BASE_PROVINCE) {
    if (!score || score < 680 || score < undergraduateLine(subject, candidateProvince) + 100) return;
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

function canonicalizeRows(score, rushRows, stableRows, safeRows, rushCandidates, stableCandidates, safeCandidates, excludedKeys = new Set()) {
    const result = { rush: [], stable: [], safe: [] };
    const used = new Set();
    const sources = [rushRows, stableRows, safeRows, rushCandidates, stableCandidates, safeCandidates];
    for (const source of sources) {
        if (!source) continue;
        for (const row of source) {
            if (!row) continue;
            const key = rowKey(row);
            if (used.has(key) || excludedKeys.has(key)) continue;
            const bucket = canonicalBucket(row, score);
            if (result[bucket].length >= MAX_VISIBLE_ROWS) continue;
            result[bucket].push(row);
            used.add(key);
        }
    }
    rebalanceEmptyCanonicalRows(result);
    fillCanonicalRows(result, score, sources, excludedKeys);
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

function fillCanonicalRows(result, score, sources, excludedKeys = new Set()) {
    const used = new Set([...result.rush, ...result.stable, ...result.safe].map(rowKey));
    fillCanonicalBucket(result.rush, 'rush', score, used, sources, excludedKeys);
    fillCanonicalBucket(result.stable, 'stable', score, used, sources, excludedKeys);
    fillCanonicalBucket(result.safe, 'safe', score, used, sources, excludedKeys);
}

function fillCanonicalBucket(rows, bucket, score, used, sources, excludedKeys = new Set()) {
    for (const source of sources) {
        if (!source) continue;
        for (const row of source) {
            if (!row || rows.length >= MAX_VISIBLE_ROWS) return;
            const key = rowKey(row);
            if (used.has(key) || excludedKeys.has(key) || canonicalBucket(row, score) !== bucket) continue;
            rows.push(row);
            used.add(key);
        }
    }
}

function queryCandidatesWithFallback(score, subject, schoolProvince, bucket, allowUndergraduate, allowJuniorCollege, preferQualityJuniorCollege, limit, schoolProvinceFilter = '') {
    const first = queryCandidates(score, subject, schoolProvince, bucket, allowUndergraduate, allowJuniorCollege,
        preferQualityJuniorCollege, limit, schoolProvinceFilter);
    if (!preferQualityJuniorCollege || first.length >= MAX_VISIBLE_ROWS) {
        return first;
    }
    const merged = new Map(first.map(row => [rowKey(row), row]));
    const fallback = queryCandidates(score, subject, schoolProvince, bucket, allowUndergraduate, allowJuniorCollege,
        false, limit, schoolProvinceFilter);
    for (const row of fallback) {
        merged.set(rowKey(row), row);
        if (merged.size >= limit) break;
    }
    return Array.from(merged.values());
}

function queryCandidates(score, subject, schoolProvince, bucket, allowUndergraduate, allowJuniorCollege, preferQualityJuniorCollege, limit, schoolProvinceFilter = '') {
    const line = undergraduateLine(subject, schoolProvince);
    const [low, high] = scoreBand(score, bucket, allowUndergraduate, allowJuniorCollege, line);

    return predictionLines
        .filter(row => row.subjectType === subject)
        .filter(row => row.schoolProvince && row.schoolProvince !== '未识别')
        .filter(row => !schoolProvinceFilter || row.schoolProvince === schoolProvinceFilter)
        .filter(row => schoolProvince !== BASE_PROVINCE || isHenanGuideCandidate(row))
        .filter(row => row.majorDirection && row.majorDirection.trim() && !row.majorDirection.includes('未提供'))
        .filter(row => row.majorCategory && row.majorCategory.trim() && !['理', '术', '技', '管理', '商务', '包含', '未提供', '专业', '验技术', '方向'].includes(row.majorCategory))
        .filter(row => electiveRequirementMatches(row, schoolProvince))
        .filter(row => !preferQualityJuniorCollege || isHighQualityJuniorCollege(row))
        .filter(row => allowUndergraduate || row.schoolLevel !== '本科')
        .filter(row => allowJuniorCollege || row.schoolLevel !== '专科')
        .filter(row => !allowUndergraduate || allowJuniorCollege || row.schoolLevel === '本科')
        .filter(row => allowUndergraduate || !allowJuniorCollege || row.schoolLevel === '专科')
        .map(row => toProvinceCandidate(row, schoolProvince))
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

function toProvinceCandidate(row, candidateProvince = BASE_PROVINCE) {
    const copy = { ...row };
    const delta = candidateProvince === BASE_PROVINCE ? 0 : nationalAdmissionAdjustment(copy, candidateProvince);
    copy.nationalDelta = delta;
    copy.candidateProvince = candidateProvince;
    copy.admissionMode = ruleModeForRow(copy);
    copy.nationalEstimateLabel = candidateProvince === BASE_PROVINCE
        ? (Number(copy.sourcePlanYear) === 2026 ? '招生之友2026计划' : '按2025投档线')
        : `${displayProvinceName(candidateProvince)}线差+本省规则估算`;
    copy.filingScore = toProvinceScore(Number(copy.filingScore || copy.predictScore || 0) + delta, copy.subjectType, candidateProvince);
    copy.predictScore = toProvinceScore(Number(copy.predictScore || copy.filingScore || 0) + delta, copy.subjectType, candidateProvince);
    copy.predictLow = toProvinceScore(Number(copy.predictLow || copy.predictScore - 6) + delta, copy.subjectType, candidateProvince);
    copy.predictHigh = toProvinceScore(Number(copy.predictHigh || copy.predictScore + 6) + delta, copy.subjectType, candidateProvince);
    copy.predictRange = `${copy.predictLow}-${copy.predictHigh}`;
    copy.confidence = copy.nationalEstimateLabel;
    return copy;
}

function nationalAdmissionAdjustment(row, candidateProvince = BASE_PROVINCE) {
    if (!candidateProvince || candidateProvince === BASE_PROVINCE) return 0;
    const schoolProvince = row.schoolProvince || '';
    let delta = Number(SCHOOL_PROVINCE_COMPETITION_DELTA[schoolProvince] || 0);
    if (schoolProvince === candidateProvince) {
        delta += Number(LOCAL_PROVINCE_ADVANTAGE[candidateProvince] || -6);
    }
    const text = [row.schoolName, row.majorDirection, row.majorCategory, row.majorGroupFull].filter(Boolean).join('');
    if (row.schoolLevel === '本科') {
        if (containsAny(text, ['北京大学', '清华大学', '复旦大学', '上海交通大学', '浙江大学', '南京大学', '中国人民大学'])) delta += 10;
        else if (containsAny(text, ['大学']) && Number(row.predictScore || row.filingScore || 0) >= 590) delta += 4;
        if (containsAny(text, ['中外合作', '合作办学', '预科'])) delta -= 8;
    } else if (row.schoolLevel === '专科') {
        delta -= 2;
        if (isHighQualityJuniorCollege(row)) delta += 5;
        if (schoolProvince === candidateProvince) delta -= 4;
    }
    if (containsAny(text, ['临床医学', '口腔医学', '法学', '汉语言文学', '师范', '计算机', '人工智能', '电气'])) delta += 3;
    return clamp(Math.round(delta), -18, 18);
}

function electiveRequirementMatches(row, candidateProvince = BASE_PROVINCE) {
    const examMode = provinceExamMode(candidateProvince);
    if (examMode === 'traditional') return true;
    const text = [row.schoolName, row.majorGroupFull, row.majorDirection, row.majorCategory].filter(Boolean).join('');
    const selected = new Set(selectedElectives);
    const hasPhysics = subjectType === '物理' || subjectType === '理科' || selected.has('物理');
    const hasHistory = subjectType === '历史' || subjectType === '文科' || selected.has('历史');
    if (/物理/.test(text) && !hasPhysics) return false;
    if (/历史/.test(text) && !hasHistory) return false;
    if (/化学/.test(text) && !selected.has('化学')) return false;
    if (/生物/.test(text) && !selected.has('生物')) return false;
    if (/政治|思想政治/.test(text) && !selected.has('政治')) return false;
    if (/地理/.test(text) && !selected.has('地理')) return false;
    return true;
}

function shouldUseQualityJuniorCollege(score, subject, bucket, candidateProvince = BASE_PROVINCE) {
    const line = undergraduateLine(subject, candidateProvince);
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
    return scoreOf(b) - scoreOf(a);
}

function takeUniqueRows(candidates, used, excludedKeys = new Set()) {
    const rows = [];
    for (const row of candidates) {
        const key = rowKey(row);
        if (used.has(key) || excludedKeys.has(key)) continue;
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
    if (schoolProvince !== BASE_PROVINCE) return [0, 0, 0];
    const totalTarget = Math.max(1, Math.round(MAX_VISIBLE_ROWS * 3 * HENAN_LOCAL_RECOMMEND_RATIO));
    const baseCount = Math.floor(totalTarget / 3);
    const extraCount = totalTarget % 3;
    const basePlan = [0, 1, 2].map(index => baseCount + (index < extraCount ? 1 : 0));
    return rotateHenanLimits(score, subject,
        basePlan,
        [basePlan[1], basePlan[2], basePlan[0]],
        [basePlan[2], basePlan[0], basePlan[1]]);
}

function henanFirstPageRule(score) {
    if (score >= 160 && score <= 300) return { min: 5, max: 6 };
    if (score > 300 && score <= 600) return { min: 3, max: 4 };
    return null;
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

function enforceHenanFirstPageRows(rows, score, subject, excludedKeys = new Set()) {
    const rule = henanFirstPageRule(score);
    if (!rule) return;
    const targetCount = targetHenanFirstPageCount(score, subject, rule);
    ensureHenanBucketCoverage(rows, score, subject, excludedKeys);
    fillHenanFirstPageRows(rows, score, subject, targetCount, excludedKeys);
    trimHenanFirstPageRows(rows, rule.max);
    ensureHenanBucketCoverage(rows, score, subject, excludedKeys);
    fillHenanFirstPageRows(rows, score, subject, targetCount, excludedKeys);
    trimHenanFirstPageRows(rows, rule.max);
}

function targetHenanFirstPageCount(score, subject, rule) {
    const spread = rule.max - rule.min;
    if (spread <= 0) return rule.min;
    let seed = (score || 0) + recommendNonce;
    if (subject === '物理') seed += 17;
    return rule.min + (((seed % (spread + 1)) + (spread + 1)) % (spread + 1));
}

function ensureHenanBucketCoverage(rows, score, subject, excludedKeys = new Set()) {
    const used = currentRowKeys(rows);
    excludedKeys.forEach(key => used.add(key));
    for (const config of henanBucketConfigs(score, subject)) {
        if (rows[config.key].some(isHenan)) continue;
        const candidate = findHenanCandidateForBucket(score, subject, config.bucket, used);
        if (candidate && addOrReplaceHenanRow(rows[config.key], candidate, used, score, subject, config.key)) {
            used.add(rowKey(candidate));
        }
    }
}

function fillHenanFirstPageRows(rows, score, subject, minCount, excludedKeys = new Set()) {
    const used = currentRowKeys(rows);
    excludedKeys.forEach(key => used.add(key));
    let count = countHenanFirstPageRows(rows);
    while (count < minCount) {
        let changed = false;
        for (const config of henanBucketConfigs(score, subject)) {
            if (count >= minCount) return;
            const candidate = findHenanCandidateForBucket(score, subject, config.bucket, used);
            if (!candidate) continue;
            if (addOrReplaceHenanRow(rows[config.key], candidate, used, score, subject, config.key)) {
                used.add(rowKey(candidate));
                count = countHenanFirstPageRows(rows);
                changed = true;
            }
        }
        if (!changed) return;
    }
}

function henanBucketConfigs(score, subject) {
    const configs = [
        { key: 'rush', bucket: '冲刺' },
        { key: 'stable', bucket: '稳妥' },
        { key: 'safe', bucket: '保底' }
    ];
    const order = rotatedHenanBuckets(score, subject);
    return order.map(bucket => configs.find(config => config.bucket === bucket)).filter(Boolean);
}

function findHenanCandidateForBucket(score, subject, bucket, used) {
    const nearLineJuniorCollege = shouldUseQualityJuniorCollege(score, subject, bucket);
    const allowUndergraduate = score >= undergraduateLine(subject) && !nearLineJuniorCollege;
    const allowJuniorCollege = score < undergraduateLine(subject) || nearLineJuniorCollege;
    const candidates = queryCandidatesWithFallback(score, subject, '河南', bucket, allowUndergraduate,
        allowJuniorCollege, nearLineJuniorCollege, QUERY_LIMIT);
    for (const row of candidates) {
        if (isUsableHenanCandidate(row, used)) return row;
    }
    const fallbackBuckets = bucket === '冲刺'
        ? ['冲刺高分兜底']
        : (bucket === '稳妥' ? ['保底同省补足'] : ['保底扩展', '保底兜底', '保底同省补足']);
    for (const fallbackBucket of fallbackBuckets) {
        const fallback = queryCandidatesWithFallback(score, subject, '河南', fallbackBucket, allowUndergraduate,
            allowJuniorCollege, nearLineJuniorCollege, QUERY_LIMIT);
        for (const row of fallback) {
            if (isUsableHenanCandidate(row, used)) return row;
        }
    }
    for (const row of queryAnyHenanCandidates(score, subject)) {
        if (isUsableHenanCandidate(row, used)) return row;
    }
    return null;
}

function queryAnyHenanCandidates(score, subject) {
    const line = undergraduateLine(subject);
    const juniorOnly = score < line + NEAR_UNDERGRADUATE_MARGIN;
    return predictionLines
        .filter(row => row.province === '河南')
        .filter(row => row.subjectType === subject)
        .filter(row => row.schoolProvince === '河南')
        .filter(row => row.schoolProvince && row.schoolProvince !== '未识别')
        .filter(row => row.majorDirection && row.majorDirection.trim() && !row.majorDirection.includes('未提供'))
        .filter(row => row.majorCategory && row.majorCategory.trim() && !['理', '术', '技', '管理', '商务', '包含', '未提供', '专业', '验技术', '方向'].includes(row.majorCategory))
        .filter(row => !juniorOnly || row.schoolLevel === '专科')
        .sort((a, b) => {
            const distance = Math.abs(scoreOf(a) - score) - Math.abs(scoreOf(b) - score);
            if (distance !== 0) return distance;
            return scoreOf(b) - scoreOf(a);
        })
        .slice(0, QUERY_LIMIT);
}

function isUsableHenanCandidate(row, used) {
    return row && isHenan(row) && !used.has(rowKey(row));
}

function currentRowKeys(rows) {
    return new Set([...rows.rush, ...rows.stable, ...rows.safe].map(rowKey));
}

function trimHenanFirstPageRows(rows, maxCount) {
    let count = countHenanFirstPageRows(rows);
    if (count <= maxCount) return;
    for (const bucket of ['safe', 'stable', 'rush']) {
        for (let i = rows[bucket].length - 1; i >= 0 && count > maxCount; i--) {
            if (!isHenan(rows[bucket][i])) continue;
            if (countHenanRows(rows[bucket]) <= 1) continue;
            rows[bucket].splice(i, 1);
            count--;
        }
    }
}

function countHenanFirstPageRows(rows) {
    return [...rows.rush, ...rows.stable, ...rows.safe].filter(isHenan).length;
}

function countHenanRows(rows) {
    return rows.filter(isHenan).length;
}

function addOrReplaceHenanRow(rows, candidate, used = new Set(), score = 0, subject = '', bucketKey = '') {
    if (!rows || !candidate) return false;
    if (used.has(rowKey(candidate))) return false;
    if (rows.length < MAX_VISIBLE_ROWS) {
        const slots = Array.from({ length: rows.length + 1 }, (_, index) => index);
        rows.splice(naturalHenanIndex(slots, candidate, score, subject, bucketKey), 0, candidate);
        return true;
    }
    const replaceableIndexes = rows
        .map((row, index) => isHenan(row) ? -1 : index)
        .filter(index => index >= 0);
    if (!replaceableIndexes.length) return false;
    rows[naturalHenanIndex(replaceableIndexes, candidate, score, subject, bucketKey)] = candidate;
    return true;
}

function naturalHenanIndex(indexes, candidate, score, subject, bucketKey) {
    const preferred = preferredHenanSlot(score, subject, bucketKey);
    const tieBreak = seededOffset(`${score}|${subject}|${bucketKey}|${recommendNonce}|${rowKey(candidate)}`, indexes.length);
    return indexes.slice().sort((a, b) => {
        const distance = Math.abs(a - preferred) - Math.abs(b - preferred);
        if (distance !== 0) return distance;
        return ((a + tieBreak) % MAX_VISIBLE_ROWS) - ((b + tieBreak) % MAX_VISIBLE_ROWS);
    })[0];
}

function preferredHenanSlot(score, subject, bucketKey) {
    const patterns = {
        rush: [1, 0, 2],
        stable: [0, 2, 1],
        safe: [2, 1, 0]
    };
    let seed = score || 0;
    if (subject === '物理') seed += 17;
    const plan = (((seed + recommendNonce) % MAX_VISIBLE_ROWS) + MAX_VISIBLE_ROWS) % MAX_VISIBLE_ROWS;
    const slots = patterns[bucketKey];
    if (slots) return slots[plan];
    return plan;
}

function polishPrediction(row, score, bucket) {
    const copy = { ...row };
    let predicted = copy.predictScore + displayBoost(copy);
    predicted = clampToBucket(predicted, score, bucket);
    const band = copy.schoolLevel === '本科' ? 6 : 8;
    if (copy.schoolLevel === '本科') predicted = Math.max(predicted, undergraduateLine(copy.subjectType, copy.candidateProvince) + 2);
    if (copy.schoolLevel === '专科') predicted = Math.min(predicted, undergraduateLine(copy.subjectType, copy.candidateProvince) + 18);
    copy.predictScore = predicted;
    copy.predictLow = clamp(predicted - band, 0, 750);
    copy.predictHigh = clamp(predicted + band, 0, 750);
    copy.predictRange = `${copy.predictLow}-${copy.predictHigh}`;
    copy.rangeFloat = predicted - scoreOf(copy);
    copy.confidence = copy.nationalEstimateLabel || '全国录取估算';
    return copy;
}

function displayBoost(row) {
    const text = [row.schoolName, row.majorGroupFull, row.majorDirection, row.majorCategory].filter(Boolean).join('');
    let delta = row.schoolLevel === '本科' ? 2 : 1;
    if (containsAny(text, ['临床医学', '口腔医学', '法学', '汉语言文学', '师范', '计算机', '软件', '人工智能', '大数据', '电子信息', '电气', '自动化', '护理'])) delta += 1;
    if (containsAny(text, ['中外合作', '合作办学'])) delta -= 1;
    return clamp(delta, 0, 4);
}

function section(type, word, title, desc, rows) {
    let body = '';
    if (!rows.length) {
        body = '<div class="empty">当前条件下暂未匹配到院校，可以调整分数或再选科目。</div>';
    } else {
        body = `<div class="recommend-grid-head">院校</div><div class="recommend-grid">`
            + rows.map(r => `<div class="school-item">
                    <div class="school-name">${escapeHtml(displaySchoolName(r))}${displayMajorGroupSuffix(r)}</div>
                    <div class="school-meta">
                        <span>${escapeHtml(displaySchoolLevel(r))}</span>
                        <span class="school-region">${escapeHtml(r.schoolProvince || '')}</span>
                        ${displaySchoolCodeMeta(r)}
                    </div>
                </div>`).join('') + '</div>';
    }
    return `<div class="block ${type}"><div class="block-side"><div class="round">${word}</div><h2>${title}</h2><p>${desc}</p></div><div class="table-wrap">${body}</div></div>`;
}

function displaySchoolName(row) {
    if (!row || !row.schoolName) return '';
    if (row.schoolLevel !== '专科') return row.schoolName || '';
    const specialName = SPECIAL_JUNIOR_COLLEGE_DISPLAYS.get(row.schoolName);
    if (specialName) return specialName;
    if (isUndergradLikeJuniorCollege(row.schoolName)) return `${row.schoolName}（专科批）`;
    return row.schoolName || '';
}

function displaySchoolLevel(row) {
    if (row && row.schoolLevel === '专科'
        && (SPECIAL_JUNIOR_COLLEGE_DISPLAYS.has(row.schoolName) || isUndergradLikeJuniorCollege(row.schoolName))) {
        return '专科批';
    }
    return row.schoolLevel || '';
}

function displayMajorGroupSuffix(row) {
    if (!row || ruleModeForRow(row) !== 'group' || !row.majorGroup) return '';
    return ` ${escapeHtml(row.majorGroup)}组`;
}

function displaySchoolCodeMeta(row) {
    if (!row || !row.schoolCode) return '';
    return `<span>院校代码${escapeHtml(row.schoolCode)}</span>`;
}

function displayApplicationSubline(row) {
    if (!row) return '';
    const mode = ruleModeForRow(row);
    if (mode !== 'majorSchool') return '';
    const major = resolveApplicationMajorText(row);
    if (!major) return '';
    return `<div class="school-subline">专业（类）：${escapeHtml(major)}</div>`;
}

function displayAdmissionMode(row) {
    const mode = ruleModeForRow(row);
    if (mode === 'group') return '院校专业组';
    if (mode === 'majorSchool') return '专业+院校';
    return '院校志愿';
}

function isUndergradLikeJuniorCollege(schoolName) {
    const name = String(schoolName || '');
    return containsAny(name, UNDERGRAD_LIKE_SCHOOL_WORDS) && !containsAny(name, VOCATIONAL_SCHOOL_WORDS);
}

function renderMajorList(value) {
    const text = normalizeMajorName(String(value == null ? '' : value).trim());
    if (!isRenderableMajorText(text)) return '';
    const parts = text.split(/[、,，;；]/).map(v => normalizeMajorName(v.trim())).filter(isRenderableMajorText);
    if (parts.length <= 1) return `<span class="major-text">${escapeHtml(text)}</span>`;
    const visibleParts = parts.slice(0, 3);
    const suffix = parts.length > 3 ? '<span class="major-chip more">等</span>' : '';
    return `<div class="major-list">${visibleParts.map(v => `<span class="major-chip">${escapeHtml(v)}</span>`).join('')}${suffix}</div>`;
}

function resolveMajorText(row) {
    const fixed = MAJOR_TEXT_FIXES.get(`${row.schoolName}|${row.subjectType}|${row.majorGroup}`);
    const candidates = [row.majorDirection, row.majorCategory]
        .map(value => normalizeMajorName(String(value || '').trim()))
        .filter(isRenderableMajorText);
    if (candidates.length) return candidates[0];
    return fixed || '';
}

function resolveApplicationMajorText(row) {
    const fixed = MAJOR_TEXT_FIXES.get(`${row.schoolName}|${row.subjectType}|${row.majorGroup}`);
    const candidates = [
        row.majorCategory,
        ...splitMajorCandidates(row.majorDirection),
        fixed
    ]
        .map(value => normalizeMajorName(String(value || '').trim()))
        .filter(isCleanApplicationMajorText);
    return candidates[0] || '';
}

function splitMajorCandidates(value) {
    return String(value || '')
        .split(/[、,，;；/]/)
        .map(item => item.trim())
        .filter(Boolean);
}

function normalizeMajorName(value) {
    return value
        .replace(/^划[)）]?$/, '')
        .replace(/^中压学/, '中医学')
        .replace(/^格床医学/, '临床医学')
        .replace(/竹医/g, '村医')
        .replace(/免赀/g, '免费')
        .replace(/诗划/g, '计划')
        .replace(/培养计划$/, '培养计划)')
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

function isRenderableMajorText(value) {
    const text = String(value || '').trim();
    if (!text) return false;
    if (/^划[)）]?$/.test(text)) return false;
    if (/^[)）]+$/.test(text)) return false;
    return true;
}

function isCleanApplicationMajorText(value) {
    const text = String(value || '').trim();
    if (!isRenderableMajorText(text)) return false;
    if (text.length > 22) return false;
    if (/[0-9]\d{2,}|元|收费|项目|校区|建议|英语成绩|只招|不招|要求|地点|年\)|人$|点[:：]/.test(text)) return false;
    if (/^[()（）]+/.test(text)) return false;
    return true;
}

function undergraduateLine(subject, candidateProvince = BASE_PROVINCE) {
    return provinceLine(subject, candidateProvince);
}

function scoreOf(row) {
    return Number(row.filingScore || row.predictScore || 0);
}

function isHenanGuideCandidate(row) {
    return row
        && row.province === BASE_PROVINCE
        && String(row.schoolCode || '').trim()
        && String(row.majorGroup || '').trim()
        && String(row.majorGroupFull || '').trim()
        && Number(row.planCount) > 0;
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
