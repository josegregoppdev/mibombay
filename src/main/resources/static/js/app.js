// ============================================
// Mi Bombay - Sistema de Gestión para Restaurantes
// JavaScript global
// ============================================

document.addEventListener('DOMContentLoaded', function () {
    // Auto-cerrar alertas después de 5 segundos
    const alerts = document.querySelectorAll('.alert-dismissible');
    alerts.forEach(function (alert) {
        setTimeout(function () {
            const bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        }, 5000);
    });

    // Sidebar toggle con backdrop
    const toggle = document.getElementById('sdSidebarToggle');
    const sidebar = document.querySelector('.sd-sidebar');
    const backdrop = document.getElementById('sdSidebarBackdrop');

    function isMobile() {
        return window.innerWidth <= 768;
    }

    function openSidebar() {
        if (isMobile()) {
            sidebar.classList.add('sd-sidebar--open');
            backdrop.classList.add('sd-sidebar-backdrop--visible');
            document.body.style.overflow = 'hidden';
        }
    }

    function closeSidebar() {
        sidebar.classList.remove('sd-sidebar--open');
        backdrop.classList.remove('sd-sidebar-backdrop--visible');
        document.body.style.overflow = '';
    }

    toggle?.addEventListener('click', function () {
        if (isMobile()) {
            if (sidebar.classList.contains('sd-sidebar--open')) {
                closeSidebar();
            } else {
                openSidebar();
            }
        }
    });

    backdrop?.addEventListener('click', closeSidebar);

    // Cerrar sidebar al hacer click en un link (mobile)
    sidebar?.querySelectorAll('.sd-link').forEach(function (link) {
        link.addEventListener('click', function () {
            if (isMobile()) {
                closeSidebar();
            }
        });
    });

    // Cerrar sidebar al redimensionar a desktop
    window.addEventListener('resize', function () {
        if (!isMobile()) {
            closeSidebar();
        }
    });

    // POS Cart drawer (mobile)
    const posCart = document.querySelector('.sd-pos-cart');
    const cartToggle = document.querySelector('.sd-pos-cart-header');
    const cartBackdrop = document.getElementById('sdPosCartBackdrop');

    function openCart() {
        if (posCart && isMobile()) {
            posCart.classList.add('sd-pos-cart--open');
            if (cartBackdrop) cartBackdrop.classList.add('sd-pos-cart-backdrop--visible');
        }
    }

    function closeCart() {
        if (posCart) {
            posCart.classList.remove('sd-pos-cart--open');
            if (cartBackdrop) cartBackdrop.classList.remove('sd-pos-cart-backdrop--visible');
        }
    }

    // Toggle cart al hacer click en el header (solo mobile)
    cartToggle?.addEventListener('click', function () {
        if (isMobile()) {
            if (posCart.classList.contains('sd-pos-cart--open')) {
                closeCart();
            } else {
                openCart();
            }
        }
    });

    // Cerrar cart al hacer click en backdrop
    cartBackdrop?.addEventListener('click', closeCart);

    // Global fade-in animation for .sd-fade elements
    document.querySelectorAll('.sd-fade').forEach(function (el, i) {
        setTimeout(function () {
            el.style.opacity = '1';
            el.style.transform = 'translateY(0)';
        }, 100 * i);
    });
});
