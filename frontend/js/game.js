let gameInterval = null;
let myShips = new Array(100).fill(0);
let isReady = false;
let isMyTurn = false;
let gameEnded = false;

// Переменные для ручной расстановки
let selectedShipSize = 4;
let isHorizontal = true;
let availableShips = { 4: 1, 3: 2, 2: 3, 1: 4 };

const playBtn = document.getElementById('playBtn');
const leaveBtn = document.getElementById('leaveBtn');
const lobby = document.getElementById('gameLobby');
const placementArea = document.getElementById('placementArea');
const battleArea = document.getElementById('battleArea');
const statusBadge = document.getElementById('gameStatus');

// --- РУЧНАЯ РАССТАНОВКА ---

function selectShip(size) {
    selectedShipSize = size;
    updateShipButtons();
}

function rotateShip() {
    isHorizontal = !isHorizontal;
}

function clearBoard() {
    myShips = new Array(100).fill(0);
    availableShips = { 4: 1, 3: 2, 2: 3, 1: 4 };
    selectedShipSize = 4;
    updateShipButtons();
    renderPlacementGrid();
    document.getElementById('readyBtn').disabled = true;
}

function updateShipButtons() {
    [1, 2, 3, 4].forEach(size => {
        const btn = document.getElementById(`btn-${size}`);
        if (availableShips[size] > 0) {
            btn.disabled = false;
            btn.textContent = `🟦 ${size}-палубный (${availableShips[size]})`;
        } else {
            btn.disabled = true;
            btn.textContent = `🟦 ${size}-палубный (0)`;
        }
        if (size === selectedShipSize) {
            btn.classList.add('active');
        } else {
            btn.classList.remove('active');
        }
    });
}

// Проверка можно ли поставить корабль (учитывая соседние клетки)
function canPlace(row, col, size, isHoriz, currentShips) {
    if (isHoriz && col + size > 10) return false;
    if (!isHoriz && row + size > 10) return false;

    for (let i = 0; i < size; i++) {
        const r = isHoriz ? row : row + i;
        const c = isHoriz ? col + i : col;
        for (let dr = -1; dr <= 1; dr++) {
            for (let dc = -1; dc <= 1; dc++) {
                const nr = r + dr, nc = c + dc;
                if (nr >= 0 && nr < 10 && nc >= 0 && nc < 10) {
                    if (currentShips[nr * 10 + nc] === 1) return false;
                }
            }
        }
    }
    return true;
}

// ПРЕВЬЮ (наведение мышки)
function handleHover(r, c) {
    clearHover();
    if (availableShips[selectedShipSize] <= 0) return;

    const isValid = canPlace(r, c, selectedShipSize, isHorizontal, myShips);
    const className = isValid ? 'preview-valid' : 'preview-invalid';

    // Подсвечиваем клетки (даже если нельзя поставить, чтобы игрок видел почему)
    for (let i = 0; i < selectedShipSize; i++) {
        const nr = isHorizontal ? r : r + i;
        const nc = isHorizontal ? c + i : c;

        if (nr >= 0 && nr < 10 && nc >= 0 && nc < 10) {
            const idx = nr * 10 + nc;
            const cell = document.getElementById('placementGrid').children[idx];
            // Не перекрашиваем уже поставленные корабли
            if (cell && myShips[idx] !== 1) {
                cell.classList.add(className);
            }
        }
    }
}

function clearHover() {
    document.querySelectorAll('#placementGrid .bs-cell').forEach(c => {
        c.classList.remove('preview-valid', 'preview-invalid');
    });
}

function handlePlacementClick(r, c) {
    if (availableShips[selectedShipSize] <= 0) return;
    if (!canPlace(r, c, selectedShipSize, isHorizontal, myShips)) return;

    for (let i = 0; i < selectedShipSize; i++) {
        const nr = isHorizontal ? r : r + i;
        const nc = isHorizontal ? c + i : c;
        myShips[nr * 10 + nc] = 1;
    }

    availableShips[selectedShipSize]--;

    // Автовыбор следующего размера
    if (availableShips[selectedShipSize] === 0) {
        if (availableShips[4] > 0) selectedShipSize = 4;
        else if (availableShips[3] > 0) selectedShipSize = 3;
        else if (availableShips[2] > 0) selectedShipSize = 2;
        else if (availableShips[1] > 0) selectedShipSize = 1;
    }

    updateShipButtons();
    renderPlacementGrid();

    if (availableShips[4] === 0 && availableShips[3] === 0 && availableShips[2] === 0 && availableShips[1] === 0) {
        document.getElementById('readyBtn').disabled = false;
    }
}

// СЛУЧАЙНАЯ РАССТАНОВКА
function randomizePlacement() {
    clearBoard();
    const shipsToPlace = [4, 3, 3, 2, 2, 2, 1, 1, 1, 1];

    shipsToPlace.forEach(size => {
        let placed = false;
        while (!placed) {
            const isHoriz = Math.random() > 0.5;
            const row = Math.floor(Math.random() * 10);
            const col = Math.floor(Math.random() * 10);

            if (canPlace(row, col, size, isHoriz, myShips)) {
                for (let i = 0; i < size; i++) {
                    const r = isHoriz ? row : row + i;
                    const c = isHoriz ? col + i : col;
                    myShips[r * 10 + c] = 1;
                }
                placed = true;
            }
        }
    });

    // Обнуляем доступные для ручной расстановки, так как всё уже занято
    availableShips = { 4: 0, 3: 0, 2: 0, 1: 0 };
    updateShipButtons();
    renderPlacementGrid();
    document.getElementById('readyBtn').disabled = false;
}

function renderPlacementGrid() {
    const grid = document.getElementById('placementGrid');
    grid.innerHTML = '';
    for (let i = 0; i < 100; i++) {
        const cell = document.createElement('div');
        cell.className = 'bs-cell';
        if (myShips[i] === 1) cell.classList.add('ship');

        // Добавляем события мыши
        cell.onmouseover = () => handleHover(Math.floor(i / 10), i % 10);
        cell.onmouseout = clearHover;
        cell.onclick = () => handlePlacementClick(Math.floor(i / 10), i % 10);

        grid.appendChild(cell);
    }
}

document.getElementById('readyBtn').addEventListener('click', async () => {
    document.getElementById('readyBtn').disabled = true;
    document.getElementById('readyBtn').textContent = 'Ожидание врага...';
    await apiFetch('/api/game/place', {
        method: 'POST',
        body: JSON.stringify({ ships: myShips })
    });
    isReady = true;
});

// --- ОТРИСОВКА БОЯ ---
function renderGrid(elementId, ships, shots, isEnemy) {
    const grid = document.getElementById(elementId);
    grid.innerHTML = '';
    for (let i = 0; i < 100; i++) {
        const cell = document.createElement('div');
        cell.className = 'bs-cell';

        const r = Math.floor(i / 10), c = i % 10;
        const isShip = ships[i] === 1;
        const shot = shots ? shots[r][c] : 0;

        if (isShip && (!isEnemy || shot === 2)) cell.classList.add('ship');
        if (shot === 1) cell.classList.add('miss');
        if (shot === 2) cell.classList.add('hit');

        if (isEnemy && shot === 0 && !gameEnded && isMyTurn) {
            cell.onclick = () => shoot(r, c);
        } else {
            cell.classList.add('disabled');
        }
        grid.appendChild(cell);
    }
}

async function shoot(row, col) {
    document.querySelectorAll('#enemyGrid .bs-cell').forEach(c => c.classList.add('disabled'));
    await apiFetch('/api/game/shoot', {
        method: 'POST',
        body: JSON.stringify({ row, col })
    });
    checkState();
}

// --- ИГРОВОЙ ЦИКЛ ---
playBtn.addEventListener('click', async () => {
    playBtn.disabled = true;
    playBtn.textContent = 'Поиск соперника...';
    state.currentAction = 'Ищет игру (Морской бой)';
    pingActivity(state.currentAction);

    await apiFetch('/api/game/join', { method: 'POST' });
    gameInterval = setInterval(checkState, 1500);
});

async function checkState() {
    try {
        const res = await apiFetch('/api/game/state');
        if (!res.ok) throw new Error("Ошибка сервера: " + res.status);

        const data = await res.json();

        if (data.status === 'none') return leaveGame();
        if (data.status === 'waiting') return;

        if (data.status === 'placement') {
            lobby.classList.add('hidden');
            placementArea.classList.remove('hidden');

            state.currentAction = 'Расставляет корабли (Морской бой)';
            pingActivity(state.currentAction);

            if (isReady) {
                document.getElementById('readyBtn').textContent = 'Ожидание врага...';
            } else {
                updateShipButtons();
                // Если сетка пустая (только перешли в статус), рендерим её
                if (document.getElementById('placementGrid').children.length === 0) {
                    renderPlacementGrid();
                }
            }
        }
        else if (data.status === 'playing' || data.status === 'finished') {
            lobby.classList.add('hidden');
            placementArea.classList.add('hidden');
            battleArea.classList.remove('hidden');

            const g = data.game;
            isMyTurn = g.turn === state.nickname;
            gameEnded = data.status === 'finished';

            state.currentAction = gameEnded ? 'Ожидает новую игру' : 'Играет в Морской бой';
            pingActivity(state.currentAction);

            statusBadge.textContent = gameEnded ? 'Игра окончена' : (isMyTurn ? 'Ваш ход' : 'Ход врага');

            renderGrid('myGrid', g.myShips.flat(), g.myShots, false);
            renderGrid('enemyGrid', g.myShips.flat(), g.enemyShots, true);

            if (gameEnded) {
                clearInterval(gameInterval);
                gameInterval = null;
                document.getElementById('battleMsg').textContent =
                    g.winner === state.nickname ? 'Вы победили! 🎉' : 'Вы проиграли 😢';
            } else {
                document.getElementById('battleMsg').textContent =
                    isMyTurn ? 'Стреляйте по врагу!' : 'Ждем выстрела врага...';
            }
        }
    } catch (e) {
        console.error(e);
    }
}

function leaveGame() {
    clearInterval(gameInterval);
    gameInterval = null;
    apiFetch('/api/game/leave', { method: 'POST' });

    state.currentAction = 'Просто смотрит';
    pingActivity(state.currentAction);

    lobby.classList.remove('hidden');
    placementArea.classList.add('hidden');
    battleArea.classList.add('hidden');
    playBtn.disabled = false;
    playBtn.textContent = '🚀 Найти игру';

    clearBoard();
}

leaveBtn.addEventListener('click', leaveGame);

// Инициализация кнопок при старте
updateShipButtons();