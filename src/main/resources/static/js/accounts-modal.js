// accounts-modal.js - упрощенная версия
class AccountModalManager {
    constructor() {
        this.deleteModal = new bootstrap.Modal(document.getElementById('accountDeleteModal'));
        this.accountToDelete = null;

        this.initDeleteListeners();
    }

    initDeleteListeners() {
        // Кнопка подтверждения удаления
        document.getElementById('confirmDeleteBtn')?.addEventListener('click', () => {
            this.confirmDelete();
        });

        // Делегирование событий для кнопок удаления в таблице
        document.addEventListener('click', (e) => {
            if (e.target.closest('.delete-account-btn')) {
                const btn = e.target.closest('.delete-account-btn');
                const accountId = btn.getAttribute('data-account-id');
                const accountName = btn.getAttribute('data-account-name');
                const accountCurrency = btn.getAttribute('data-account-currency');

                this.showDeleteModal(accountId, accountName, accountCurrency);
            }
        });
    }

    // Показать модальное окно удаления
    showDeleteModal(accountId, accountName, accountCurrency) {
        this.accountToDelete = {
            id: accountId,
            name: accountName,
            currency: accountCurrency
        };

        // Заполняем данные в модальном окне
        document.getElementById('deleteAccountId').textContent = accountId;
        document.getElementById('deleteAccountName').textContent = accountName;
        document.getElementById('deleteAccountCurrency').textContent = accountCurrency;

        // Показываем модальное окно
        this.deleteModal.show();
    }

    // Подтверждение удаления
    async confirmDelete() {
        if (!this.accountToDelete) return;

        const accountId = this.accountToDelete.id;

        try {
            // Показываем индикатор загрузки
            const confirmBtn = document.getElementById('confirmDeleteBtn');
            const originalText = confirmBtn.innerHTML;
            confirmBtn.innerHTML = `
                <span class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>
                Удаление...
            `;
            confirmBtn.disabled = true;

            // AJAX запрос на удаление
            const response = await fetch(`/accounts/${accountId}/delete`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                }
            });

            const data = await response.json();

            if (!response.ok || !data.success) {
                throw new Error(data.error || 'Ошибка удаления');
            }

            // Успешное удаление
            this.deleteModal.hide();

            // Показываем уведомление
            this.showToast(data.message || 'Счет успешно удален!', 'success');

            // Удаляем строку из таблицы
            this.removeAccountFromTable(accountId);

        } catch (error) {
            // Восстанавливаем кнопку
            const confirmBtn = document.getElementById('confirmDeleteBtn');
            confirmBtn.innerHTML = originalText;
            confirmBtn.disabled = false;

            // Показываем ошибку
            this.showToast(`Ошибка: ${error.message}`, 'danger');
            console.error('Delete error:', error);
        }
    }

    // Удалить счет из таблицы
    removeAccountFromTable(accountId) {
        // Находим строку с нужным account-id
        const row = document.querySelector(`tr[data-account-id="${accountId}"]`);
        if (row) {
            // Плавное исчезновение
            row.style.transition = 'opacity 0.3s';
            row.style.opacity = '0';

            setTimeout(() => {
                row.remove();
                // Если таблица пустая, показываем сообщение
                this.checkEmptyTable();
            }, 300);
        }
    }

    // Проверить пустую таблицу
    checkEmptyTable() {
        const tbody = document.querySelector('.table tbody');
        if (tbody && tbody.children.length === 0) {
            tbody.innerHTML = `
                <tr class="text-center py-4">
                    <td colspan="4">
                        <div class="py-5">
                            <i class="bi bi-wallet2 display-4 text-muted mb-3"></i>
                            <h5 class="text-muted">Счетов нет</h5>
                            <a th:href="@{/accounts/create}" class="btn btn-primary mt-2">
                                <i class="bi bi-plus-circle me-1"></i>Создать счет
                            </a>
                        </div>
                    </td>
                </tr>
            `;
        }
    }

    // Показать уведомление
    showToast(message, type = 'info') {
        const toastHtml = `
            <div class="toast align-items-center text-bg-${type} border-0" role="alert">
                <div class="d-flex">
                    <div class="toast-body">
                        <i class="bi ${type === 'success' ? 'bi-check-circle' : 'bi-exclamation-circle'} me-2"></i>
                        ${message}
                    </div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
                </div>
            </div>
        `;

        const container = document.getElementById('toastContainer');
        container.innerHTML = toastHtml;

        const toastElement = container.querySelector('.toast');
        const toast = new bootstrap.Toast(toastElement, {
            autohide: true,
            delay: 3000
        });
        toast.show();
    }
}

// Инициализация
let accountModalManager;

document.addEventListener('DOMContentLoaded', () => {
    accountModalManager = new AccountModalManager();
    window.accountModalManager = accountModalManager;
});