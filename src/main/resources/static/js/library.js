/*
 * Клиент страницы /itline.
 * Вся информация хранится в БД. Клиент хранит копию и обновляет её только
 * по ответам сервера; при любой ошибке - перечитывает список целиком
 */
(() => {
  'use strict';

  // Константы и состояние
  const ICONS = {
    icon1: 'Книга',
    icon2: 'Учебник',
    icon3: 'Документ'
  };
  const state = {
    books: [],            // копия данных сервера, список книг
    selectedId: null,     // id выбранных для удаления строка
    pending: new Map(),   // Map со значениями (id, статус активности), это нужно, пока PATCH
    // еще выполняется
    deleting: false,      // показывает, выполняется ли сейчас удаление
  };
  const $ = (id) => document.getElementById(id);

  class ApiError extends Error {
  }

  // HTTP-слой
  async function request(method, url, body) {
    let res;
    try {
      res = await fetch(url, {
        method,
        headers: body === undefined ? {} : {'Content-Type': 'application/json'},
        body: body === undefined ? undefined : JSON.stringify(body),
      });
    } catch {
      throw new ApiError('Нет связи с сервером. Проверьте подключение и повторите.');
    }
    if (res.status === 204) {
      return null; // успешное удаление - тела нет
    }
    let data = null;
    try {
      data = await res.json();
    } catch {
      // тела нет или не JSON
    }
    if (!res.ok) {
      throw new ApiError((data && data.detail) || `Ошибка сервера (${res.status})`);
    }
    return data;
  }

  // Последовательная очередь. Все обращения к серверу идут строго одно за другим. Так ответы не
  // обгоняют друг друга и не затирают более свежее состояние
  let queue = Promise.resolve();

  function enqueue(task) {
    const result = queue.then(task);
    queue = result.catch(() => {
    }); // ошибка одной задачи не ломает очередь
    return result;
  }

  // Функция для уведомлений
  let toastTimer;

  function toast(message) {
    const el = $('toast');
    el.textContent = message;
    el.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => el.classList.remove('show'), 4500);
  }

  // Загрузка списка
  async function loadBooks() {
    try {
      state.books = await request('GET', '/api/books');
      // если выбранную строку удалили в другой вкладке, выбор снимается
      if (!state.books.some((i) => i.id === state.selectedId)) {
        state.selectedId = null;
      }
      render();
    } catch (e) {
      toast(e.message);
    }
  }

  // Отрисовка
  function render() {
    const tbody = $('rows');
    tbody.replaceChildren();
    for (const book of state.books) {
      const tr = document.createElement('tr');
      tr.dataset.id = book.id;
      tr.tabIndex = 0;
      const selected = book.id === state.selectedId;
      if (selected) {
        tr.classList.add('selected');
      }
      tr.setAttribute('aria-selected', String(selected));

      const tdActive = document.createElement('td');
      const cb = document.createElement('input');
      cb.type = 'checkbox';
      cb.setAttribute('aria-label', 'Активно');
      const isPending = state.pending.has(book.id);
      cb.checked = isPending ? state.pending.get(book.id) : book.active;
      cb.disabled = isPending; // пока запрос выполняется - повторно не кликнуть
      // клик по чекбоксу не выбирает строку
      cb.addEventListener('click', (e) => e.stopPropagation());
      cb.addEventListener('change', () => onToggle(book.id, cb.checked));
      tdActive.append(cb);

      const tdIcon = document.createElement('td');
      const img = document.createElement('img');
      img.src = `/icons/${book.icon}.svg`;
      img.width = 32;
      img.height = 32;
      img.alt = ICONS[book.icon] || 'Иконка';
      tdIcon.append(img);

      const tdName = document.createElement('td');
      tdName.textContent = book.name;
      const tdDesc = document.createElement('td');
      tdDesc.textContent = book.description;

      tr.append(tdActive, tdIcon, tdName, tdDesc);
      tbody.append(tr);
    }
    $('empty').hidden = state.books.length > 0;
    $('btn-delete').disabled = state.selectedId === null || state.deleting;
  }

  // Выбор строки
  function select(id) {
    state.selectedId = state.selectedId === id ? null : id;
    state.pending.has(book.id);
    render();
  }

  $('rows').addEventListener('click', (e) => {
    const tr = e.target.closest('tr');
    if (tr) select(Number(tr.dataset.id));
  });
  $('rows').addEventListener('keydown', (e) => {
    if ((e.key === 'Enter' || e.key === ' ') && e.target.tagName === 'TR') {
      e.preventDefault();
      select(Number(e.target.dataset.id));
    }
  });

  // Переключение статуса активности
  function onToggle(id, desired) {
    state.pending.set(id, desired);
    render();
    enqueue(async () => {
      try {
        // версию берем в момент выполнения
        const current = state.books.find((i) => i.id === id);
        if (!current) {
          throw new ApiError('Книга уже удалена в другом окне');
        }
        const updated = await request('PATCH', `/api/books/${id}/active`,
          {active: desired, version: current.version});
        state.books = state.books.map((i) => (i.id === id ? updated : i));
      } catch (e) {
        toast(e.message);
        await loadBooks();// синхронизируемся с БД при ошибке
      } finally {
        state.pending.delete(id);
        render();
      }
    }).then(r => {
    });
  }

  // Удаление
  const dlgConfirm = $('dlg-confirm');
  $('btn-delete').addEventListener('click', () => {
    const book = state.books.find((i) => i.id === state.selectedId);
    if (!book) return;
    $('confirm-text').textContent = `Вы точно хотите удалить ${book.name}?`;
    dlgConfirm.dataset.id = book.id;
    dlgConfirm.showModal();
  });
  $('btn-confirm-no').addEventListener('click', () => dlgConfirm.close());
  $('btn-confirm-yes').addEventListener('click', () => {
    const id = Number(dlgConfirm.dataset.id);
    dlgConfirm.close();
    state.deleting = true;
    render();
    enqueue(async () => {
      try {
        const current = state.books.find((i) => i.id === id);
        if (!current) {
          throw new ApiError('Книга уже удалена в другом окне');
        }
        await request('DELETE', `/api/books/${id}?version=${current.version}`);
        state.books = state.books.filter((i) => i.id !== id);
        if (state.selectedId === id) {
          state.selectedId = null;
        }
      } catch (e) {
        toast(e.message);
        await loadBooks();
      } finally {
        state.deleting = false;
        render();
      }
    });
  });

  // Модальное окно создания
  const dlgCreate = $('dlg-create');
  const form = $('create-form');
  const iconList = $('icon-list');
  const iconBtn = $('icon-btn');
  let currentIcon = 'icon1';
  let creating = false;

  // выпадающий список с картинками
  for (const [key, label] of Object.entries(ICONS)) {
    const li = document.createElement('li');
    li.setAttribute('role', 'option');
    li.dataset.icon = key;
    const img = document.createElement('img');
    img.src = `/icons/${key}.svg`;
    img.width = 32;
    img.height = 32;
    img.alt = '';
    const span = document.createElement('span');
    span.textContent = label;
    li.append(img, span);
    li.addEventListener('click', () => setIcon(key));
    iconList.append(li);
  }

  function setIcon(key) {
    currentIcon = key;
    $('icon-btn-img').src = `/icons/${key}.svg`;
    $('icon-btn-label').textContent = ICONS[key];
    for (const li of iconList.children) li.setAttribute('aria-selected', String(li.dataset.icon === key));
    iconList.hidden = true;
    iconBtn.setAttribute('aria-expanded', 'false');
  }

  iconBtn.addEventListener('click', () => {
    const willOpen = iconList.hidden;
    iconList.hidden = !willOpen;
    iconBtn.setAttribute('aria-expanded', String(willOpen));
  });

  function showFormError(msg) {
    $('create-error').textContent = msg;
  }

  $('btn-add').addEventListener('click', () => {
    form.reset();
    setIcon('icon1');
    showFormError('');
    dlgCreate.showModal();
    $('f-name').focus();
  });
  $('btn-cancel').addEventListener('click', () => dlgCreate.close());

  form.addEventListener('submit', (e) => {
    e.preventDefault();
    if (creating) {
      return; // защита от двойного клика/Enter
    }
    const name = $('f-name').value.trim();
    const description = $('f-desc').value.trim();
    if (!name || !description) {
      showFormError('Заполните название и описание.');
      return;
    }
    creating = true;
    $('btn-create').disabled = true;
    showFormError('');
    enqueue(async () => {
      try {
        await request('POST', '/api/books',
          {active: $('f-active').checked, icon: currentIcon, name, description});
        dlgCreate.close();
        await loadBooks(); // показываем то, что реально в БД
      } catch (err) {
        showFormError(err.message);
      } finally {
        creating = false;
        $('btn-create').disabled = false;
      }
    }).then(r => {
    });
  });

  // Старт и синхронизация
  enqueue(loadBooks).then(r => {
  });
  // По возвращении на вкладку, подтягивается актуальное состояние БД
  window.addEventListener('focus', () => enqueue(loadBooks));
})();
