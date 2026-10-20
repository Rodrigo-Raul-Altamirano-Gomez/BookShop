// Accesibilidad de diálogos: foco inicial, Escape, tabulación y retorno al disparador.
(() => {
    const priorFocus = new Map();
    const selector = 'button:not([disabled]), a[href], input:not([disabled]), '
        + 'select:not([disabled]), textarea:not([disabled]), [tabindex="0"]';
    const visible = dialog => [...dialog.querySelectorAll(selector)].filter(el => el.getClientRects().length);
    new MutationObserver(() => {
        const dialogs = [...document.querySelectorAll('[role="dialog"]')];
        for (const dialog of dialogs) if (!priorFocus.has(dialog)) {
            priorFocus.set(dialog, document.activeElement);
            requestAnimationFrame(() => {
                if (dialog.isConnected)
                    (dialog.querySelector('input') || visible(dialog)[0])?.focus();
            });
        }
        for (const [dialog, previous] of priorFocus) if (!dialog.isConnected) {
            priorFocus.delete(dialog);
            if (previous?.isConnected) previous.focus();
        }
        document.body.classList.toggle('dialog-open', dialogs.length > 0);
    }).observe(document.body, { childList: true, subtree: true });
    document.addEventListener('keydown', event => {
        const dialog = [...document.querySelectorAll('[role="dialog"]')].at(-1);
        if (!dialog) return;
        if (event.key === 'Escape') {
            event.preventDefault();
            dialog.querySelector('.modal-close')?.click();
        }
        if (event.key === 'Tab') {
            const controls = visible(dialog), first = controls[0], last = controls.at(-1);
            if (event.shiftKey && document.activeElement === first) {
                event.preventDefault();
                last?.focus();
            }
            else if (!event.shiftKey && document.activeElement === last) {
                event.preventDefault();
                first?.focus();
            }
        }
    });
})();
