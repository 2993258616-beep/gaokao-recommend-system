let subjectType = '历史';
const MAX_VISIBLE_ROWS = 3;
const PLAN_COUNT = 6;
const SPECIAL_JUNIOR_COLLEGE_DISPLAYS = new Map([
    ['河南理工大学', '河南理工大学（民政学院·专科批）'],
    ['平顶山学院', '平顶山学院（医药科技学院·专科批）']
]);
const UNDERGRAD_LIKE_SCHOOL_WORDS = ['大学', '学院'];
const VOCATIONAL_SCHOOL_WORDS = [
    '职业', '技术', '高等专科', '专科学校', '职工大学',
    '开放大学', '广播电视大学', '技师', '干部'
];
const $ = id => document.getElementById(id);
let recommendNonce = 0;
let lastCriteriaKey = '';
setupSessionGuard();

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

function resetPlanAndRender() {
    recommendNonce = 0;
    lastCriteriaKey = getCurrentCriteriaKey();
    renderRecommend();
}

function getCurrentCriteriaKey() {
    const score = $('score').value || 500;
    const schoolProvince = $('schoolProvince').value;
    return `${score}|${subjectType}|${schoolProvince}`;
}

function renderRecommend() {
    const score = $('score').value || 500;
    const schoolProvince = $('schoolProvince').value;
    const criteriaKey = `${score}|${subjectType}|${schoolProvince}`;
    if (criteriaKey !== lastCriteriaKey) {
        recommendNonce = 0;
        lastCriteriaKey = criteriaKey;
    }
    $('tagSubject').innerText = subjectType + '类';
    $('tagProvince').innerText = schoolProvince;
    $('summaryText').innerText = `按历史录取数据参考：河南${subjectType}类，预估 ${score} 分，筛选条件为学校地区：${schoolProvince}。`;

    fetch(`/api/recommend?score=${encodeURIComponent(score)}&subjectType=${encodeURIComponent(subjectType)}&schoolProvince=${encodeURIComponent(schoolProvince)}&nonce=${encodeURIComponent(recommendNonce)}`, {
        credentials: 'same-origin',
        cache: 'no-store'
    })
        .then(resp => {
            if (resp.status === 401 || resp.status === 403) {
                window.location.href = '/login?expired=1';
                throw new Error('登录已失效');
            }
            if (!resp.ok) throw new Error('请求失败');
            return resp.json();
        })
        .then(data => {
            const scoreValue = Number(score) || 500;
            const rows = normalizeRecommendRows(data, scoreValue);
            const html = section('rush', '冲', '冲刺推荐', '适合略高于当前分数的院校', rows.rush)
                + section('stable', '稳', '稳妥推荐', '适合重点考虑的匹配院校', rows.stable)
                + section('safe', '保', '保底推荐', '适合保底填报的院校', rows.safe);
            $('resultArea').innerHTML = html;
        })
        .catch(err => {
            $('resultArea').innerHTML = `<div class="empty">加载失败：${escapeHtml(err.message)}</div>`;
        });
}

function section(type, word, title, desc, rows) {
    let body = '';
    if (!rows.length) {
        body = '<div class="empty">当前筛选条件下暂无数据，可以切换学校地区。</div>';
    } else {
        body = `<table class="recommend-table"><thead><tr><th>院校/专业组</th><th>专业</th><th>学校地区</th></tr></thead><tbody>`
            + rows.map(r => `<tr>
                <td><div class="school-name">${escapeHtml(displaySchoolName(r))} ${escapeHtml(r.majorGroup || '')}组</div><div class="sub-info">${escapeHtml(r.schoolType || '')} · ${escapeHtml(displaySchoolLevel(r))} · 河南考生</div></td>
                <td class="major-cell">${renderMajorList(r.majorDirection || r.majorCategory || '')}</td>
                <td>${escapeHtml(r.schoolProvince || '')}</td>
            </tr>`).join('') + '</tbody></table>';
    }
    return `<div class="block ${type}"><div class="block-side"><div class="round">${word}</div><h2>${title}</h2><p>${desc}</p></div><div class="table-wrap">${body}</div></div>`;
}

function normalizeRecommendRows(data, score) {
    return {
        rush: filterByScoreLevel(data.rush || [], score).slice(0, MAX_VISIBLE_ROWS),
        stable: filterByScoreLevel(data.stable || [], score).slice(0, MAX_VISIBLE_ROWS),
        safe: filterByScoreLevel(data.safe || [], score).slice(0, MAX_VISIBLE_ROWS)
    };
}

function filterByScoreLevel(rows, score) {
    if (score < 600) return rows;
    return rows.filter(row => !isJuniorCollege(row));
}

function isJuniorCollege(row) {
    const text = [
        row.schoolType,
        row.schoolLevel,
        row.majorGroupFull,
        row.schoolName
    ].filter(Boolean).join(' ');
    return /专科|高职|高等专科学校/.test(text);
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

function isUndergradLikeJuniorCollege(schoolName) {
    const name = String(schoolName || '');
    return containsAny(name, UNDERGRAD_LIKE_SCHOOL_WORDS) && !containsAny(name, VOCATIONAL_SCHOOL_WORDS);
}

function renderMajorList(value) {
    const text = normalizeMajorName(String(value == null ? '' : value).trim());
    if (!text) return '';
    const parts = text.split(/[、,，;；]/).map(v => normalizeMajorName(v.trim())).filter(Boolean);
    if (parts.length <= 1) {
        return `<span class="major-text">${escapeHtml(text)}</span>`;
    }
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

function containsAny(text, words) {
    return words.some(word => text.includes(word));
}

function escapeHtml(value) {
    return String(value == null ? '' : value)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;')
        .replace(/>/g, '&gt;').replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function setupSessionGuard() {
    let manualLogout = false;
    let closeSent = false;
    const logoutForm = document.querySelector('.logout-form');
    if (logoutForm) {
        logoutForm.addEventListener('submit', () => {
            manualLogout = true;
        });
    }

    const ping = () => {
        fetch('/api/session/ping', {
            method: 'GET',
            credentials: 'same-origin',
            cache: 'no-store'
        }).catch(() => {});
    };
    window.setInterval(ping, 30000);

    const closeSession = () => {
        if (manualLogout || closeSent) return;
        closeSent = true;
        try {
            if (navigator.sendBeacon) {
                navigator.sendBeacon('/api/session/close', new Blob(['close'], {type: 'text/plain'}));
                return;
            }
        } catch (ignored) {}
        try {
            fetch('/api/session/close', {
                method: 'POST',
                credentials: 'same-origin',
                keepalive: true,
                cache: 'no-store'
            }).catch(() => {});
        } catch (ignored) {}
    };

    window.addEventListener('pagehide', closeSession);
    window.addEventListener('beforeunload', closeSession);
}

resetPlanAndRender();
