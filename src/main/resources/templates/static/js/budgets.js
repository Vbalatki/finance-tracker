// Обработка модальных окон для бюджетов
document.addEventListener('DOMContentLoaded', function() {

    // Модальное окно создания бюджета для категории
    const createBudgetForCategoryModal = document.getElementById('createBudgetForCategoryModal');
    if (createBudgetForCategoryModal) {
        createBudgetForCategoryModal.addEventListener('show.bs.modal', function(event) {
            const button = event.relatedTarget;
            const categoryId = button.getAttribute('data-category-id');
            const categoryName = button.getAttribute('data-category-name');

            const modal = this;
            modal.querySelector('.modal-title').textContent = 'Бюджет для: ' + categoryName;
            modal.querySelector('input[name="categoryId"]').value = categoryId;
        });
    }

    // Модальное окно редактирования бюджета
    const editBudgetModal = document.getElementById('editBudgetModal');
    if (editBudgetModal) {
        editBudgetModal.addEventListener('show.bs.modal', function(event) {
            const button = event.relatedTarget;
            const budgetId = button.getAttribute('data-id');
            const currentLimit = button.getAttribute('data-limit');

            const form = document.getElementById('editBudgetForm');
            form.action = '/budgets/' + budgetId;

            document.getElementById('editBudgetLimit').value = currentLimit;
        });
    }

    // Валидация форм
    const forms = document.querySelectorAll('form[data-validate]');
    forms.forEach(form => {
        form.addEventListener('submit', function(event) {
            const amountInput = this.querySelector('input[name="monthlyLimit"]');
            if (amountInput && parseFloat(amountInput.value) <= 0) {
                event.preventDefault();
                alert('Лимит бюджета должен быть больше 0');
            }
        });
    });
});