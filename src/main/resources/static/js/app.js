/* ============================================================
 *  app.js — utilidades comunes (CSRF, AJAX helpers)
 * ============================================================ */

(function () {
    'use strict';

    // ─── CSRF helpers ─────────────────────────────────────────
    const csrfToken  = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    /**
     * Wrapper de fetch que incluye CSRF + JSON automáticamente.
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

        // 204 No Content
        if (res.status === 204) return null;
        return res.json();
    };

    // ─── Toast helper ─────────────────────────────────────────
    window.showToast = function (msg, type = 'info') {
        const colorMap = { info: 'primary', success: 'success', error: 'danger', warning: 'warning' };
        const cls = colorMap[type] || 'primary';

        const wrap = document.createElement('div');
        wrap.className = `position-fixed bottom-0 end-0 p-3`;
        wrap.style.zIndex = 1080;
        wrap.innerHTML = `
            <div class="toast align-items-center text-white bg-${cls} border-0 show" role="alert">
                <div class="d-flex">
                    <div class="toast-body">${msg}</div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
                </div>
            </div>`;
        document.body.appendChild(wrap);
        setTimeout(() => wrap.remove(), 4500);
    };

    // ─── Smooth-scroll para anclas internas ───────────────────
    document.querySelectorAll('a[href^="#"]').forEach(a => {
        a.addEventListener('click', function (e) {
            const id = this.getAttribute('href').slice(1);
            const target = document.getElementById(id);
            if (target) {
                e.preventDefault();
                target.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }
        });
    });

    console.debug('[app.js] inicializado');
})();
