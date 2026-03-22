/**
 * Business Tracer - Sidebar Navigation Component
 * 动态注入侧边导航栏到页面
 */

(function() {
    'use strict';

    // Navigation configuration
    const NAV_ITEMS = [
        {
            id: 'flow-logs',
            icon: '📊',
            text: '流程日志',
            href: '/business-tracer/index.html',
            match: (path, params) => path.includes('index.html') && !params.has('view')
        },
        {
            id: 'trace',
            icon: '🔍',
            text: '链路追踪',
            href: '/business-tracer/index.html?view=trace',
            match: (path, params) => path.includes('index.html') && params.get('view') === 'trace'
        },
        {
            id: 'dsl',
            icon: '⚙️',
            text: 'DSL管理',
            href: '/business-tracer/dsl.html',
            match: (path) => path.includes('dsl.html')
        },
        {
            id: 'editor',
            icon: '🎨',
            text: '可视化编辑',
            href: '/business-tracer/editor.html',
            match: (path) => path.includes('editor.html')
        },
        {
            id: 'alerts',
            icon: '🚨',
            text: '告警中心',
            href: '/business-tracer/alerts.html',
            match: (path) => path.includes('alerts.html')
        }
    ];

    // Storage key for sidebar state
    const SIDEBAR_STATE_KEY = 'bt_sidebar_collapsed';

    /**
     * Initialize sidebar
     */
    function initSidebar() {
        // Create and inject sidebar HTML
        injectSidebar();
        
        // Create mobile header if needed
        injectMobileHeader();
        
        // Set up event listeners
        setupEventListeners();
        
        // Restore sidebar state
        restoreSidebarState();
        
        // Set up keyboard shortcuts
        setupKeyboardShortcuts();
    }

    /**
     * Inject sidebar HTML into the page
     */
    function injectSidebar() {
        const currentPath = window.location.pathname;
        const params = new URLSearchParams(window.location.search);
        
        const navItemsHtml = NAV_ITEMS.map(item => {
            const isActive = item.match(currentPath, params);
            return `
                <a href="${item.href}" class="nav-item ${isActive ? 'active' : ''}" data-nav-id="${item.id}">
                    <span class="nav-icon">${item.icon}</span>
                    <span class="nav-text">${item.text}</span>
                </a>
            `;
        }).join('');

        const sidebarHtml = `
            <aside class="sidebar" id="sidebar">
                <div class="sidebar-header">
                    <span class="sidebar-logo">🔍</span>
                    <span class="sidebar-title">Business Tracer</span>
                </div>
                <nav class="sidebar-nav">
                    ${navItemsHtml}
                </nav>
                <div class="sidebar-footer">
                    <button class="sidebar-toggle" id="sidebarToggle" title="折叠/展开侧边栏 (← →)">
                        <span class="toggle-icon">◀</span>
                        <span class="toggle-text">收起</span>
                    </button>
                </div>
            </aside>
        `;

        // Insert at the beginning of body
        document.body.insertAdjacentHTML('afterbegin', sidebarHtml);
    }

    /**
     * Inject mobile header for responsive layout
     */
    function injectMobileHeader() {
        const mobileHeaderHtml = `
            <header class="mobile-header" id="mobileHeader">
                <button class="mobile-menu-btn" id="mobileMenuBtn" aria-label="Menu">☰</button>
                <span class="mobile-title">Business Tracer</span>
                <span style="width: 40px;"></span>
            </header>
            <div class="mobile-overlay" id="mobileOverlay"></div>
        `;
        
        document.body.insertAdjacentHTML('afterbegin', mobileHeaderHtml);
        document.body.classList.add('has-mobile-header');
    }

    /**
     * Set up event listeners
     */
    function setupEventListeners() {
        // Sidebar toggle button
        const toggleBtn = document.getElementById('sidebarToggle');
        if (toggleBtn) {
            toggleBtn.addEventListener('click', toggleSidebar);
        }

        // Mobile menu button
        const mobileMenuBtn = document.getElementById('mobileMenuBtn');
        if (mobileMenuBtn) {
            mobileMenuBtn.addEventListener('click', toggleMobileSidebar);
        }

        // Mobile overlay click to close
        const mobileOverlay = document.getElementById('mobileOverlay');
        if (mobileOverlay) {
            mobileOverlay.addEventListener('click', closeMobileSidebar);
        }

        // Window resize handler
        window.addEventListener('resize', handleResize);
    }

    /**
     * Toggle sidebar collapsed state
     */
    function toggleSidebar() {
        const sidebar = document.getElementById('sidebar');
        if (!sidebar) return;

        const isCollapsed = sidebar.classList.toggle('collapsed');
        document.body.classList.toggle('sidebar-collapsed', isCollapsed);
        
        // Update toggle button
        const toggleIcon = sidebar.querySelector('.toggle-icon');
        const toggleText = sidebar.querySelector('.toggle-text');
        if (toggleIcon) toggleIcon.textContent = isCollapsed ? '▶' : '◀';
        if (toggleText) toggleText.textContent = isCollapsed ? '展开' : '收起';

        // Save state
        localStorage.setItem(SIDEBAR_STATE_KEY, isCollapsed ? 'true' : 'false');
    }

    /**
     * Toggle mobile sidebar
     */
    function toggleMobileSidebar() {
        const sidebar = document.getElementById('sidebar');
        const overlay = document.getElementById('mobileOverlay');
        
        sidebar?.classList.toggle('mobile-open');
        overlay?.classList.toggle('show');
    }

    /**
     * Close mobile sidebar
     */
    function closeMobileSidebar() {
        const sidebar = document.getElementById('sidebar');
        const overlay = document.getElementById('mobileOverlay');
        
        sidebar?.classList.remove('mobile-open');
        overlay?.classList.remove('show');
    }

    /**
     * Handle window resize
     */
    function handleResize() {
        // Close mobile sidebar on resize to desktop
        if (window.innerWidth >= 768) {
            closeMobileSidebar();
        }
    }

    /**
     * Restore sidebar state from localStorage
     */
    function restoreSidebarState() {
        const savedState = localStorage.getItem(SIDEBAR_STATE_KEY);
        if (savedState === 'true') {
            const sidebar = document.getElementById('sidebar');
            if (sidebar) {
                sidebar.classList.add('collapsed');
                document.body.classList.add('sidebar-collapsed');
                
                const toggleIcon = sidebar.querySelector('.toggle-icon');
                const toggleText = sidebar.querySelector('.toggle-text');
                if (toggleIcon) toggleIcon.textContent = '▶';
                if (toggleText) toggleText.textContent = '展开';
            }
        }
    }

    /**
     * Set up keyboard shortcuts
     */
    function setupKeyboardShortcuts() {
        document.addEventListener('keydown', (e) => {
            // Ignore if typing in input
            if (['INPUT', 'TEXTAREA', 'SELECT'].includes(document.activeElement?.tagName)) {
                // Allow Escape to blur input
                if (e.key === 'Escape') {
                    document.activeElement.blur();
                }
                return;
            }

            switch (e.key) {
                case '/':
                    // Focus search input
                    e.preventDefault();
                    const searchInput = document.querySelector('.search-input, #listBusinessId, #traceBusinessId, input[type="text"]');
                    if (searchInput) searchInput.focus();
                    break;
                    
                case 'ArrowLeft':
                    // Collapse sidebar
                    if (!document.getElementById('sidebar')?.classList.contains('collapsed')) {
                        toggleSidebar();
                    }
                    break;
                    
                case 'ArrowRight':
                    // Expand sidebar
                    if (document.getElementById('sidebar')?.classList.contains('collapsed')) {
                        toggleSidebar();
                    }
                    break;
                    
                case 'Escape':
                    // Close any open modals
                    document.querySelectorAll('.modal-overlay.show').forEach(modal => {
                        modal.classList.remove('show');
                    });
                    closeMobileSidebar();
                    break;
            }
        });

        // Ctrl+K for search (common convention)
        document.addEventListener('keydown', (e) => {
            if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
                e.preventDefault();
                const searchInput = document.querySelector('.search-input, #listBusinessId, #traceBusinessId, input[type="text"]');
                if (searchInput) searchInput.focus();
            }
        });
    }

    /**
     * Show toast notification
     */
    window.showToast = function(message, type = 'info') {
        // Remove existing toast
        const existing = document.querySelector('.toast');
        if (existing) existing.remove();

        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        toast.textContent = message;
        document.body.appendChild(toast);

        setTimeout(() => toast.remove(), 3000);
    };

    // Initialize when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initSidebar);
    } else {
        initSidebar();
    }

})();
