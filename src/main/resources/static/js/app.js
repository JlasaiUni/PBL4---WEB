/* ============================================================
 *  app.js — utilidades comunes (CSRF, AJAX helpers, UI helpers)
 * ============================================================ */

(function () {
    'use strict';

    // ─── CSRF helpers ─────────────────────────────────────────
    const csrfToken  = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    /**
     * Wrapper de fetch con CSRF + JSON automático.
     */
    window.api = async function (url, options = {}) {
        const headers = {
            'Content-Type': 'application/json',
            'Accept':       'application/json',
            ...(csrfToken && csrfHeader ? { [csrfHeader]: csrfToken } : {}),
            ...options.headers
        };

        const res = await fetch(url, { ...options, headers });

        if (!res.ok) {
            const error = await res.json().catch(() => ({ message: res.statusText }));
            throw error;
        }

        if (res.status === 204) return null;
        return res.json();
    };

    // ─── Loading state para botones ───────────────────────────
    /**
     * Activa/desactiva el estado de carga en un boton.
     * @param {HTMLButtonElement} btn
     * @param {boolean} loading
     */
    window.setLoading = function (btn, loading) {
        if (!btn) return;
        if (loading) {
            btn.dataset.originalHtml = btn.innerHTML;
            btn.innerHTML =
                '<span class="spinner-border spinner-border-sm me-2" ' +
                'role="status" aria-hidden="true"></span>Cargando...';
            btn.disabled = true;
            btn.setAttribute('aria-busy', 'true');
        } else {
            btn.innerHTML  = btn.dataset.originalHtml ?? btn.innerHTML;
            btn.disabled   = false;
            btn.removeAttribute('aria-busy');
        }
    };

    // ─── Toast helper ─────────────────────────────────────────
    window.showToast = function (msg, type) {
        type = type || 'info';
        const colorMap = {
            info:    'primary',
            success: 'success',
            error:   'danger',
            warning: 'warning'
        };
        const cls = colorMap[type] || 'primary';

        const wrap = document.createElement('div');
        wrap.className = 'position-fixed bottom-0 end-0 p-3';
        wrap.style.zIndex = 1080;
        wrap.setAttribute('aria-live', 'polite');
        wrap.setAttribute('aria-atomic', 'true');
        wrap.innerHTML =
            '<div class="toast align-items-center text-white bg-' + cls + ' border-0 show" ' +
                 'role="alert" aria-live="assertive" aria-atomic="true">' +
              '<div class="d-flex">' +
                '<div class="toast-body">' + msg + '</div>' +
                '<button type="button" class="btn-close btn-close-white me-2 m-auto" ' +
                        'data-bs-dismiss="toast" aria-label="Cerrar"></button>' +
              '</div>' +
            '</div>';
        document.body.appendChild(wrap);
        setTimeout(function () { wrap.remove(); }, 4500);
    };

    // ─── Modal de confirmacion de borrado ────────────────────
    var MODAL_ID = 'app-confirm-modal';

    function getOrCreateModal() {
        var el = document.getElementById(MODAL_ID);
        if (!el) {
            el = document.createElement('div');
            el.id = MODAL_ID;
            el.className = 'modal fade';
            el.setAttribute('tabindex', '-1');
            el.setAttribute('aria-modal', 'true');
            el.setAttribute('role', 'dialog');
            el.innerHTML =
                '<div class="modal-dialog modal-dialog-centered">' +
                  '<div class="modal-content">' +
                    '<div class="modal-header">' +
                      '<h5 class="modal-title">' +
                        '<i class="bi bi-exclamation-triangle-fill text-warning me-2" aria-hidden="true"></i>' +
                        'Confirmar accion' +
                      '</h5>' +
                      '<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Cerrar"></button>' +
                    '</div>' +
                    '<div class="modal-body" id="' + MODAL_ID + '-body">Estas seguro?</div>' +
                    '<div class="modal-footer">' +
                      '<button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>' +
                      '<button type="button" class="btn btn-danger" id="' + MODAL_ID + '-ok">Confirmar</button>' +
                    '</div>' +
                  '</div>' +
                '</div>';
            document.body.appendChild(el);
        }
        return el;
    }

    window.confirmAction = function (opts) {
        var message   = opts.message   || 'Estas seguro?';
        var onConfirm = opts.onConfirm;

        var el    = getOrCreateModal();
        var modal = bootstrap.Modal.getOrCreateInstance(el);
        el.querySelector('#' + MODAL_ID + '-body').textContent = message;

        var okBtn = el.querySelector('#' + MODAL_ID + '-ok');
        var newOk = okBtn.cloneNode(true);
        okBtn.parentNode.replaceChild(newOk, okBtn);
        newOk.addEventListener('click', function () {
            modal.hide();
            onConfirm();
        });

        modal.show();
    };

    // Delegacion de eventos para botones data-confirm
    document.addEventListener('click', function (e) {
        var btn = e.target.closest('[data-confirm]');
        if (!btn) return;
        e.preventDefault();

        var message  = btn.dataset.confirm || 'Estas seguro?';
        var action   = btn.dataset.confirmAction;
        var method   = (btn.dataset.confirmMethod || 'DELETE').toUpperCase();
        var redirect = btn.dataset.confirmRedirect || null;

        window.confirmAction({
            message: message,
            onConfirm: function () {
                window.setLoading(btn, true);
                window.api(action, { method: method })
                    .then(function () {
                        window.showToast('Operacion completada.', 'success');
                        if (redirect) {
                            window.location.href = redirect;
                        } else {
                            var card = btn.closest('.card, [data-post-id]');
                            if (card) {
                                card.style.transition = 'opacity 0.3s';
                                card.style.opacity = '0';
                                setTimeout(function () { card.remove(); }, 300);
                            }
                        }
                    })
                    .catch(function (err) {
                        window.showToast((err && err.message) || 'Error al realizar la operacion.', 'error');
                        window.setLoading(btn, false);
                    });
            }
        });
    });

    // ─── Smooth-scroll para anclas internas ───────────────────
    document.querySelectorAll('a[href^="#"]').forEach(function (a) {
        a.addEventListener('click', function (e) {
            var id = this.getAttribute('href').slice(1);
            var target = document.getElementById(id);
            if (target) {
                e.preventDefault();
                target.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }
        });
    });

    // ─── Spinner automatico en submit ─────────────────────────
    // Los formularios marcados con data-no-loading (p.ej. chat AJAX que
    // hace preventDefault) quedan excluidos para no dejar el boton
    // atascado en "Cargando...".
    document.querySelectorAll('form [type=submit]').forEach(function (btn) {
        var form = btn.closest('form');
        if (!form || form.hasAttribute('data-no-loading')) return;
        form.addEventListener('submit', function () {
            window.setLoading(btn, true);
        });
    });

    console.debug('[app.js] inicializado');
})();
