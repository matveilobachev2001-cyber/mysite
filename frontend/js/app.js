// ============================================
// КОНФИГ
// ============================================
const API = {
    me:       '/api/auth/me',
    logout:   '/api/auth/logout',
    weather:  '/api/weather',
    currency: '/api/currency'
};

const FLAGS = {
    USD: '🇺🇸', EUR: '🇪🇺', GBP: '🇬🇧', JPY: '🇯🇵',
    CHF: '🇨🇭', CNY: '🇨🇳', RUB: '🇷🇺', TRY: '🇹🇷',
    KZT: '🇰🇿', UAH: '🇺🇦', PLN: '🇵🇱', CZK: '🇨🇿'
};

// ============================================
// СОСТОЯНИЕ
// ============================================
const state = {
    token: localStorage.getItem('token'),
    nickname: localStorage.getItem('nickname'),
    currentAction: 'Просто смотрит'
};

// ============================================
// ХЕЛПЕРЫ
// ============================================
async function apiFetch(url, options = {}) {
    const headers = {
        'Content-Type': 'application/json',
        ...(options.headers || {})
    };
    if (state.token) {
        headers['Authorization'] = 'Bearer ' + state.token;
    }

    const res = await fetch(url, { ...options, headers });

    if (res.status === 401) {
        localStorage.removeItem('token');
        localStorage.removeItem('nickname');
        window.location.href = 'login.html';
        throw new Error('Unauthorized');
    }

    return res;
}

function showWeatherPlaceholder(text) {
    const el = document.getElementById('weatherResult');
    el.innerHTML = `<p class="placeholder">${text}</p>`;
}

function escapeHtml(str) {
    return String(str)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;');
}

// ============================================
// АВТОРИЗАЦИЯ
// ============================================
async function initAuth() {
    if (!state.token) {
        window.location.href = 'login.html';
        return false;
    }

    try {
        const res = await apiFetch(API.me);
        if (!res.ok) throw new Error('me failed');

        const user = await res.json();
        state.nickname = user.nickname;
        localStorage.setItem('nickname', user.nickname);

        document.getElementById('userName').textContent = user.nickname;
        return true;
    } catch (err) {
        console.error('Auth error:', err);
        return false;
    }
}

function initLogout() {
    document.getElementById('logoutBtn').addEventListener('click', async () => {
        try {
            await apiFetch(API.logout, { method: 'POST' });
        } catch (_) { }
        localStorage.removeItem('token');
        localStorage.removeItem('nickname');
        window.location.href = 'login.html';
    });
}

// ============================================
// ПОГОДА
// ============================================
function initWeather() {
    const form = document.getElementById('weatherForm');
    const input = document.getElementById('weatherInput');
    const btn = document.getElementById('weatherBtn');

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const city = input.value.trim();
        if (!city) return;

        btn.disabled = true;
        btn.textContent = '…';
        showWeatherPlaceholder('Загрузка...');

        try {
            const res = await apiFetch(`${API.weather}?city=${encodeURIComponent(city)}`);
            const data = await res.json();

            if (!res.ok) {
                showWeatherPlaceholder(data.error || 'Не удалось получить погоду');
                return;
            }

            renderWeather(data);

            // Отправляем статус
            state.currentAction = `Смотрит погоду в ${data.city}`;
            pingActivity(state.currentAction);

        } catch (err) {
            console.error(err);
            showWeatherPlaceholder('Сервер недоступен');
        } finally {
            btn.disabled = false;
            btn.textContent = 'Найти';
        }
    });
}

function renderWeather(w) {
    const el = document.getElementById('weatherResult');
    el.innerHTML = `
        <div class="weather-card">
            <div class="weather-card__city">
                ${w.icon || '🌡️'} ${escapeHtml(w.city)}
            </div>
            <div class="weather-card__desc">
                ${escapeHtml(w.description || '')}
                ${w.country ? '· ' + escapeHtml(w.country) : ''}
            </div>
            <div class="weather-card__temp">
                ${formatTemp(w.temperature)}
            </div>
            <div class="weather-card__details">
                <div class="weather-detail">
                    <div class="weather-detail__label">Ощущается</div>
                    <div class="weather-detail__value">${formatTemp(w.feelsLike)}</div>
                </div>
                <div class="weather-detail">
                    <div class="weather-detail__label">Влажность</div>
                    <div class="weather-detail__value">${w.humidity}%</div>
                </div>
                <div class="weather-detail">
                    <div class="weather-detail__label">Ветер</div>
                    <div class="weather-detail__value">${w.windSpeed.toFixed(1)} м/с</div>
                </div>
            </div>
        </div>
    `;
}

function formatTemp(t) {
    if (t == null || isNaN(t)) return '—';
    return (t > 0 ? '+' : '') + Math.round(t) + '°C';
}

// ============================================
// ВАЛЮТЫ
// ============================================
function initCurrency() {
    const select = document.getElementById('baseCurrency');
    select.addEventListener('change', () => loadCurrency(select.value));
    loadCurrency(select.value);
}

async function loadCurrency(base) {
    const list = document.getElementById('currencyList');
    const dateEl = document.getElementById('currencyDate');

    list.innerHTML = '<li class="placeholder">Загрузка курсов...</li>';
    dateEl.textContent = '—';

    try {
        const res = await apiFetch(`${API.currency}?base=${encodeURIComponent(base)}`);
        const data = await res.json();

        if (!res.ok) {
            list.innerHTML = `<li class="placeholder">${data.error || 'Ошибка'}</li>`;
            return;
        }

        dateEl.textContent = data.date || '—';
        renderCurrency(data);

        // Отправляем статус
        state.currentAction = `Смотрит курс ${data.base}`;
        pingActivity(state.currentAction);

    } catch (err) {
        console.error(err);
        list.innerHTML = '<li class="placeholder">Сервер недоступен</li>';
    }
}

function renderCurrency(data) {
    const list = document.getElementById('currencyList');
    const base = data.base;

    const entries = Object.entries(data.rates)
        .filter(([code]) => code !== base)
        .sort((a, b) => a[0].localeCompare(b[0]));

    if (entries.length === 0) {
        list.innerHTML = '<li class="placeholder">Нет данных</li>';
        return;
    }

    list.innerHTML = entries.map(([code, rate]) => `
        <li class="currency-item">
            <span class="currency-item__code">
                <span class="currency-item__flag">${FLAGS[code] || '🏳️'}</span>
                ${code}
            </span>
            <span class="currency-item__rate">
                ${rate.toFixed(4)}
            </span>
        </li>
    `).join('');
}

// ============================================
// ОНЛАЙН-ПОЛЬЗОВАТЕЛИ
// ============================================
async function pingActivity(actionText) {
    if (!state.token) return;
    try {
        await apiFetch('/api/activity/ping', {
            method: 'POST',
            body: JSON.stringify({ action: actionText })
        });
    } catch (e) { }
}

async function fetchOnline() {
    try {
        console.log("1. Запрашиваем список онлайна...");
        const res = await fetch('/api/activity/list');

        console.log("2. Ответ сервера:", res.status);

        if (!res.ok) {
            document.getElementById('onlineList').innerHTML = `<li class="placeholder">Ошибка сервера: ${res.status}</li>`;
            return;
        }

        const users = await res.json();
        console.log("3. Полученные данные:", users);

        const list = document.getElementById('onlineList');

        if (!users || users.length === 0) {
            list.innerHTML = '<li class="placeholder">Никого нет :(</li>';
            return;
        }

        list.innerHTML = users.map(u => `
            <li class="online-user">
                <span class="online-user__name">${escapeHtml(u.nickname)}</span>
                <span class="online-user__action" title="${escapeHtml(u.action)}">
                    ${escapeHtml(u.action)}
                </span>
            </li>
        `).join('');

    } catch (err) {
        console.error('Ошибка при получении онлайна:', err);
        document.getElementById('onlineList').innerHTML = `<li class="placeholder">Ошибка сети</li>`;
    }
}

function initActivity() {
    pingActivity('Зашел на сайт');
    setInterval(fetchOnline, 10000);
    fetchOnline(); // первый вызов

    setInterval(() => {
        pingActivity(state.currentAction || 'Просто смотрит');
    }, 5000);
}

// ============================================
// СТАРТ
// ============================================
(async function main() {
    const ok = await initAuth();
    if (!ok) return;

    initLogout();
    initWeather();
    initCurrency();
    initActivity(); // <--- вот здесь запуск
})();