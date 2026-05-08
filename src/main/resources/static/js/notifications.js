/* ============================================================
 *  notifications.js — cliente WebSocket (SockJS + STOMP)
 * ============================================================ */

(function () {
    'use strict';

    // Sólo conectamos WS si hay un panel de notificaciones en la página.
    // (en login/register/landing pública no es necesario)
    const liveBox = document.getElementById('live-notifications');
    const empty   = document.getElementById('ws-empty');
    const counter = document.getElementById('notif-count');

    if (!liveBox && !document.body.dataset.wsForce) return;

    let stompClient = null;
    let received = 0;

    function connect() {
        const socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);
        stompClient.debug = null;   // silencia logs

        stompClient.connect({}, frame => {
            console.info('[WS] conectado:', frame);

            // Notificaciones globales
            stompClient.subscribe('/topic/notifications', msg => {
                const data = JSON.parse(msg.body);
                renderNotification(data);
            });

            // Mensajes privados (al usuario actual)
            stompClient.subscribe('/user/queue/messages', msg => {
                const data = JSON.parse(msg.body);
                renderNotification(data, true);
            });

        }, err => {
            console.warn('[WS] error de conexión, reintentando en 5s...', err);
            setTimeout(connect, 5000);
        });
    }

    function renderNotification(data, privateMsg = false) {
        if (!liveBox) return;

        if (empty) empty.style.display = 'none';

        const item = document.createElement('div');
        item.className = 'notification-item';
        item.innerHTML = `
            <div class="d-flex justify-content-between">
                <strong>${privateMsg ? '📩 ' : ''}${escape(data.type || 'INFO')}</strong>
                <small class="text-muted">${new Date().toLocaleTimeString()}</small>
            </div>
            <div>${escape(data.message || '')}</div>
        `;
        liveBox.prepend(item);

        received += 1;
        if (counter) counter.textContent = received;

        if (window.showToast) window.showToast(data.message || 'Nueva notificación', 'info');
    }

    function escape(s) {
        return String(s).replace(/[&<>"']/g, c => ({
            '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
        }[c]));
    }

    // ── Iniciar ───────────────────────────────────────────────
    if (typeof SockJS !== 'undefined' && typeof Stomp !== 'undefined') {
        connect();
    } else {
        console.warn('[WS] SockJS/Stomp no cargados');
    }

    // Limpieza al cerrar la página
    window.addEventListener('beforeunload', () => {
        if (stompClient && stompClient.connected) stompClient.disconnect();
    });
})();
