const form = document.getElementById('loginForm');
const input = document.getElementById('nicknameInput');
const errorEl = document.getElementById('loginError');
const submitBtn = document.getElementById('loginSubmit');

// Уже вошли? Отправляем на главную
if (localStorage.getItem('token')) {
    window.location.href = 'index.html';
}

form.addEventListener('submit', async (e) => {
    e.preventDefault();
    hideError();

    const nickname = input.value.trim();

    if (nickname.length < 3) {
        showError('Ник должен быть минимум 3 символа');
        return;
    }
    if (nickname.length > 20) {
        showError('Ник должен быть максимум 20 символов');
        return;
    }
    if (!/^[a-zA-Z0-9_а-яА-ЯёЁ]+$/.test(nickname)) {
        showError('Только буквы, цифры и _');
        return;
    }

    submitBtn.disabled = true;
    submitBtn.textContent = 'Входим...';

    try {
        const res = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ nickname })
        });

        const data = await res.json();

        if (!res.ok) {
            showError(data.error || 'Не удалось войти');
            return;
        }

        localStorage.setItem('token', data.token);
        localStorage.setItem('nickname', data.user.nickname);
        window.location.href = 'index.html';

    } catch (err) {
        console.error(err);
        showError('Сервер недоступен. Попробуй позже');
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Войти';
    }
});

function showError(msg) {
    errorEl.textContent = msg;
    errorEl.classList.remove('hidden');
}

function hideError() {
    errorEl.classList.add('hidden');
}