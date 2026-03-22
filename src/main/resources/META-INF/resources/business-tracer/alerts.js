(function () {
    'use strict';

    const api = {
        getRules: '/business-tracer/api/alerts/rules',
        upsertRule: (scopeType, scopeRef) => `/business-tracer/api/alerts/rules/${encodeURIComponent(scopeType)}/${encodeURIComponent(scopeRef)}`,
        getChannels: '/business-tracer/api/alerts/channels',
        createChannel: '/business-tracer/api/alerts/channels',
        updateChannel: (id) => `/business-tracer/api/alerts/channels/${id}`,
        testSend: (id) => `/business-tracer/api/alerts/channels/${id}/test-send`,
        queryEvents: '/business-tracer/api/alerts/events',
        eventDispatchLogs: (id) => `/business-tracer/api/alerts/events/${id}/dispatch-logs`
    };

    const state = {
        rules: [],
        channels: [],
        selectedScope: { scopeType: 'GLOBAL', scopeRef: 'GLOBAL', flowCode: '' },
        history: {
            pageNum: 1,
            pageSize: 10,
            total: 0,
            list: []
        }
    };

    const silenceStorageKey = 'bt_alert_silence_window';

    document.addEventListener('DOMContentLoaded', () => {
        bindTabs();
        bindRuleActions();
        bindChannelActions();
        bindSilenceActions();
        bindHistoryActions();

        loadRules();
        loadChannels();
        loadSilenceConfig();
        loadHistory(1);
    });

    function bindTabs() {
        const tabs = document.querySelectorAll('.alert-tab');
        tabs.forEach(btn => {
            btn.addEventListener('click', () => {
                tabs.forEach(t => t.classList.remove('active'));
                document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
                btn.classList.add('active');
                const id = `tab-${btn.dataset.tab}`;
                const panel = document.getElementById(id);
                if (panel) panel.classList.add('active');
            });
        });
    }

    function bindRuleActions() {
        const form = document.getElementById('ruleForm');
        const reloadBtn = document.getElementById('ruleReloadBtn');
        if (reloadBtn) reloadBtn.addEventListener('click', loadRules);
        if (form) {
            form.addEventListener('submit', async (e) => {
                e.preventDefault();
                await saveRule();
            });
        }
    }

    async function loadRules() {
        try {
            const res = await fetch(api.getRules);
            const result = await res.json();
            if (result.code !== 200) {
                showToast(result.message || '加载规则失败', 'error');
                return;
            }
            state.rules = result.data || [];
            renderRuleTree();
            ensureRuleSelection();
            fillRuleFormFromSelection();
        } catch (e) {
            showToast(`加载规则异常: ${e.message}`, 'error');
        }
    }

    function renderRuleTree() {
        const el = document.getElementById('ruleScopeTree');
        if (!el) return;

        const flowSet = new Set();
        const nodeMap = new Map();
        state.rules.forEach(rule => {
            if (rule.flowCode) flowSet.add(rule.flowCode);
            if (rule.scopeType === 'NODE' && rule.flowCode && rule.scopeRef) {
                const list = nodeMap.get(rule.flowCode) || [];
                if (!list.includes(rule.scopeRef)) list.push(rule.scopeRef);
                nodeMap.set(rule.flowCode, list);
            }
            if (rule.scopeType === 'FLOW' && rule.scopeRef) flowSet.add(rule.scopeRef);
        });

        const flowCodes = Array.from(flowSet).sort();

        let html = '';
        html += renderScopeItem({ scopeType: 'GLOBAL', scopeRef: 'GLOBAL', flowCode: '' }, 'GLOBAL', 0);
        flowCodes.forEach(flowCode => {
            html += renderScopeItem({ scopeType: 'FLOW', scopeRef: flowCode, flowCode }, `FLOW: ${flowCode}`, 1);
            const nodeCodes = (nodeMap.get(flowCode) || []).sort();
            nodeCodes.forEach(nodeCode => {
                html += renderScopeItem({ scopeType: 'NODE', scopeRef: nodeCode, flowCode }, `NODE: ${nodeCode} (${flowCode})`, 2);
            });
        });

        el.innerHTML = html || '<div class="text-muted">暂无规则数据</div>';

        el.querySelectorAll('.scope-item').forEach(item => {
            item.addEventListener('click', () => {
                state.selectedScope = {
                    scopeType: item.dataset.scopeType,
                    scopeRef: item.dataset.scopeRef,
                    flowCode: item.dataset.flowCode || ''
                };
                markActiveScope();
                fillRuleFormFromSelection();
            });
        });

        markActiveScope();
    }

    function renderScopeItem(scope, text, level) {
        const selected = isScopeSelected(scope);
        return `
            <div class="scope-item scope-item-level-${level} ${selected ? 'active' : ''}"
                 data-scope-type="${escapeAttr(scope.scopeType)}"
                 data-scope-ref="${escapeAttr(scope.scopeRef)}"
                 data-flow-code="${escapeAttr(scope.flowCode || '')}">
                ${escapeHtml(text)}
            </div>
        `;
    }

    function ensureRuleSelection() {
        if (state.selectedScope) return;
        state.selectedScope = { scopeType: 'GLOBAL', scopeRef: 'GLOBAL', flowCode: '' };
    }

    function markActiveScope() {
        const tree = document.getElementById('ruleScopeTree');
        if (!tree) return;
        tree.querySelectorAll('.scope-item').forEach(item => {
            const scope = {
                scopeType: item.dataset.scopeType,
                scopeRef: item.dataset.scopeRef,
                flowCode: item.dataset.flowCode || ''
            };
            item.classList.toggle('active', isScopeSelected(scope));
        });
    }

    function isScopeSelected(scope) {
        const current = state.selectedScope;
        if (!current) return false;
        return current.scopeType === scope.scopeType
            && current.scopeRef === scope.scopeRef
            && (current.flowCode || '') === (scope.flowCode || '');
    }

    function fillRuleFormFromSelection() {
        const selected = state.selectedScope || { scopeType: 'GLOBAL', scopeRef: 'GLOBAL', flowCode: '' };
        const title = document.getElementById('ruleFormTitle');
        const scopeType = document.getElementById('ruleScopeType');
        const scopeRef = document.getElementById('ruleScopeRef');
        const flowCodeInput = document.getElementById('ruleFlowCode');
        const nameInput = document.getElementById('ruleName');
        const typeInput = document.getElementById('ruleAlertType');
        const enabledInput = document.getElementById('ruleEnabled');

        if (!scopeType || !scopeRef || !flowCodeInput || !nameInput || !typeInput || !enabledInput) return;

        scopeType.value = selected.scopeType;
        scopeRef.value = selected.scopeRef;
        if (title) title.textContent = `编辑规则 - ${selected.scopeType}:${selected.scopeRef}`;

        const existing = state.rules.find(r =>
            r.scopeType === selected.scopeType
            && r.scopeRef === selected.scopeRef
            && ((r.flowCode || '') === (selected.flowCode || selected.scopeType === 'FLOW' ? selected.scopeRef : ''))
        );

        if (existing) {
            nameInput.value = existing.name || '';
            typeInput.value = existing.alertType || 'NODE_FAILED';
            enabledInput.value = String(existing.enabled !== false);
            if (selected.scopeType === 'GLOBAL') flowCodeInput.value = '';
            else if (selected.scopeType === 'FLOW') flowCodeInput.value = selected.scopeRef;
            else flowCodeInput.value = existing.flowCode || selected.flowCode || '';
        } else {
            nameInput.value = '';
            typeInput.value = 'NODE_FAILED';
            enabledInput.value = 'true';
            if (selected.scopeType === 'GLOBAL') flowCodeInput.value = '';
            else if (selected.scopeType === 'FLOW') flowCodeInput.value = selected.scopeRef;
            else flowCodeInput.value = selected.flowCode || '';
        }

        if (selected.scopeType === 'FLOW') {
            flowCodeInput.readOnly = true;
        } else {
            flowCodeInput.readOnly = false;
        }
    }

    async function saveRule() {
        const scopeType = valueOf('ruleScopeType');
        const scopeRef = valueOf('ruleScopeRef');
        const name = valueOf('ruleName').trim();
        const alertType = valueOf('ruleAlertType');
        const flowCode = valueOf('ruleFlowCode').trim();
        const enabled = valueOf('ruleEnabled') === 'true';

        if (!scopeType || !scopeRef || !name || !alertType) {
            showToast('请完整填写规则信息', 'error');
            return;
        }
        if (scopeType === 'NODE' && !flowCode) {
            showToast('NODE scope 必须填写 flowCode', 'error');
            return;
        }

        const payload = {
            name,
            alertType,
            flowCode: flowCode || null,
            enabled
        };

        try {
            const res = await fetch(api.upsertRule(scopeType, scopeRef), {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const result = await res.json();
            if (result.code !== 200) {
                showToast(result.message || '规则保存失败', 'error');
                return;
            }
            showToast(result.message || '规则已保存', 'success');
            await loadRules();
        } catch (e) {
            showToast(`规则保存异常: ${e.message}`, 'error');
        }
    }

    function bindChannelActions() {
        const form = document.getElementById('channelForm');
        const createBtn = document.getElementById('channelCreateBtn');

        if (createBtn) {
            createBtn.addEventListener('click', () => resetChannelForm());
        }

        if (form) {
            form.addEventListener('submit', async (e) => {
                e.preventDefault();
                await saveChannel();
            });
        }
    }

    async function loadChannels() {
        try {
            const res = await fetch(api.getChannels);
            const result = await res.json();
            if (result.code !== 200) {
                showToast(result.message || '加载通道失败', 'error');
                return;
            }
            state.channels = result.data || [];
            renderChannelTable();
            if (!valueOf('channelId')) resetChannelForm();
        } catch (e) {
            showToast(`加载通道异常: ${e.message}`, 'error');
        }
    }

    function renderChannelTable() {
        const tbody = document.getElementById('channelTableBody');
        if (!tbody) return;

        if (!state.channels.length) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted">暂无通道</td></tr>';
            return;
        }

        tbody.innerHTML = state.channels.map(channel => `
            <tr>
                <td>${escapeHtml(String(channel.id ?? '-'))}</td>
                <td>${escapeHtml(channel.name || '-')}</td>
                <td><span class="badge badge-info">${escapeHtml(channel.channelType || '-')}</span></td>
                <td><span class="mono">${escapeHtml(channel.target || '-')}</span></td>
                <td>${channel.enabled === false ? '<span class="badge badge-warning">禁用</span>' : '<span class="badge badge-success">启用</span>'}</td>
                <td>
                    <button class="btn btn-sm btn-secondary" data-action="edit" data-id="${channel.id}">编辑</button>
                    <button class="btn btn-sm btn-primary" data-action="test" data-id="${channel.id}">测试发送</button>
                </td>
            </tr>
        `).join('');

        tbody.querySelectorAll('button[data-action="edit"]').forEach(btn => {
            btn.addEventListener('click', () => editChannel(Number(btn.dataset.id)));
        });

        tbody.querySelectorAll('button[data-action="test"]').forEach(btn => {
            btn.addEventListener('click', () => testSendChannel(Number(btn.dataset.id)));
        });
    }

    function resetChannelForm() {
        setValue('channelId', '');
        setValue('channelName', '');
        setValue('channelType', 'WEBHOOK');
        setValue('channelTarget', '');
        setValue('channelEnabled', 'true');
        const title = document.getElementById('channelFormTitle');
        if (title) title.textContent = '新建通道';
    }

    function editChannel(id) {
        const channel = state.channels.find(c => Number(c.id) === Number(id));
        if (!channel) {
            showToast(`通道不存在: ${id}`, 'error');
            return;
        }
        setValue('channelId', String(channel.id));
        setValue('channelName', channel.name || '');
        setValue('channelType', channel.channelType || 'WEBHOOK');
        setValue('channelTarget', channel.target || '');
        setValue('channelEnabled', String(channel.enabled !== false));
        const title = document.getElementById('channelFormTitle');
        if (title) title.textContent = `编辑通道 #${channel.id}`;
    }

    async function saveChannel() {
        const id = valueOf('channelId').trim();
        const name = valueOf('channelName').trim();
        const channelType = valueOf('channelType');
        const target = valueOf('channelTarget').trim();
        const enabled = valueOf('channelEnabled') === 'true';

        if (!name || !channelType || !target) {
            showToast('请完整填写通道信息', 'error');
            return;
        }

        const payload = { name, channelType, target, enabled };
        const url = id ? api.updateChannel(id) : api.createChannel;
        const method = id ? 'PUT' : 'POST';

        try {
            const res = await fetch(url, {
                method,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const result = await res.json();
            if (result.code !== 200) {
                showToast(result.message || '通道保存失败', 'error');
                return;
            }
            showToast(result.message || '通道已保存', 'success');
            resetChannelForm();
            await loadChannels();
        } catch (e) {
            showToast(`通道保存异常: ${e.message}`, 'error');
        }
    }

    async function testSendChannel(id) {
        if (!id) return;
        try {
            const res = await fetch(api.testSend(id), { method: 'POST' });
            const result = await res.json();
            if (result.code !== 200) {
                showToast(result.message || '测试发送失败', 'error');
                return;
            }
            showToast(result.message || '测试发送成功', 'success');
        } catch (e) {
            showToast(`测试发送异常: ${e.message}`, 'error');
        }
    }

    function bindSilenceActions() {
        const form = document.getElementById('silenceForm');
        if (form) {
            form.addEventListener('submit', (e) => {
                e.preventDefault();
                saveSilenceConfig();
            });
        }
    }

    function loadSilenceConfig() {
        let cfg;
        try {
            cfg = JSON.parse(localStorage.getItem(silenceStorageKey) || '{}');
        } catch (_) {
            cfg = {};
        }
        const normalized = {
            enabled: cfg.enabled !== false,
            start: typeof cfg.start === 'string' && cfg.start ? cfg.start : '23:00',
            end: typeof cfg.end === 'string' && cfg.end ? cfg.end : '07:00',
            updatedAt: cfg.updatedAt || null
        };

        setValue('silenceEnabled', String(normalized.enabled));
        setValue('silenceStart', normalized.start);
        setValue('silenceEnd', normalized.end);
        renderSilencePreview(normalized);
    }

    function saveSilenceConfig() {
        const enabled = valueOf('silenceEnabled') === 'true';
        const start = valueOf('silenceStart');
        const end = valueOf('silenceEnd');

        if (!start || !end) {
            showToast('请填写静默开始与结束时间', 'error');
            return;
        }

        const cfg = {
            enabled,
            start,
            end,
            updatedAt: new Date().toISOString()
        };

        localStorage.setItem(silenceStorageKey, JSON.stringify(cfg));
        renderSilencePreview(cfg);
        showToast('静默窗口已保存（localStorage）', 'success');
    }

    function renderSilencePreview(cfg) {
        const preview = document.getElementById('silencePreview');
        if (!preview) return;
        preview.textContent = JSON.stringify(cfg, null, 2);
    }

    function bindHistoryActions() {
        const btn = document.getElementById('historySearchBtn');
        if (btn) btn.addEventListener('click', () => loadHistory(1));
    }

    async function loadHistory(pageNum) {
        const params = new URLSearchParams();
        params.set('pageNum', String(pageNum || 1));
        params.set('pageSize', String(state.history.pageSize));

        appendIfPresent(params, 'flowCode', valueOf('historyFlowCode'));
        appendIfPresent(params, 'nodeCode', valueOf('historyNodeCode'));
        appendIfPresent(params, 'businessId', valueOf('historyBusinessId'));
        appendIfPresent(params, 'alertType', valueOf('historyAlertType'));
        appendIfPresent(params, 'status', valueOf('historyStatus'));

        const startTime = datetimeLocalToIso(valueOf('historyStartTime'));
        const endTime = datetimeLocalToIso(valueOf('historyEndTime'));
        appendIfPresent(params, 'startTime', startTime);
        appendIfPresent(params, 'endTime', endTime);

        try {
            const res = await fetch(`${api.queryEvents}?${params.toString()}`);
            const result = await res.json();
            if (result.code !== 200) {
                showToast(result.message || '历史查询失败', 'error');
                return;
            }
            const page = result.data || {};
            state.history = {
                pageNum: Number(page.pageNum || pageNum || 1),
                pageSize: Number(page.pageSize || state.history.pageSize || 10),
                total: Number(page.total || 0),
                list: Array.isArray(page.list) ? page.list : []
            };
            renderHistoryTable();
            renderHistoryPagination();
        } catch (e) {
            showToast(`历史查询异常: ${e.message}`, 'error');
        }
    }

    function renderHistoryTable() {
        const tbody = document.getElementById('historyTableBody');
        if (!tbody) return;

        const list = state.history.list || [];
        if (!list.length) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">暂无告警事件</td></tr>';
            return;
        }

        tbody.innerHTML = list.map(event => `
            <tr>
                <td>${escapeHtml(String(event.id ?? '-'))}</td>
                <td><span class="badge badge-info">${escapeHtml(event.alertType || '-')}</span></td>
                <td>${renderStatusBadge(event.status)}</td>
                <td class="mono">${escapeHtml((event.flowCode || '-') + ' / ' + (event.nodeCode || '-'))}</td>
                <td>${escapeHtml(event.businessId || '-')}</td>
                <td>${formatTime(event.occurredAt)}</td>
                <td><button class="btn btn-sm btn-secondary" data-event-id="${event.id}">查看投递</button></td>
            </tr>
        `).join('');

        tbody.querySelectorAll('button[data-event-id]').forEach(btn => {
            btn.addEventListener('click', () => loadDispatchLogs(Number(btn.dataset.eventId)));
        });
    }

    function renderHistoryPagination() {
        const el = document.getElementById('historyPagination');
        if (!el) return;

        const totalPages = Math.ceil((state.history.total || 0) / (state.history.pageSize || 10));
        el.innerHTML = '';
        if (totalPages <= 1) return;

        const current = state.history.pageNum || 1;

        const prev = document.createElement('button');
        prev.className = 'page-btn';
        prev.textContent = '‹';
        prev.disabled = current <= 1;
        prev.addEventListener('click', () => loadHistory(current - 1));
        el.appendChild(prev);

        const info = document.createElement('span');
        info.style.padding = '8px';
        info.textContent = `${current} / ${totalPages}`;
        el.appendChild(info);

        const next = document.createElement('button');
        next.className = 'page-btn';
        next.textContent = '›';
        next.disabled = current >= totalPages;
        next.addEventListener('click', () => loadHistory(current + 1));
        el.appendChild(next);
    }

    async function loadDispatchLogs(eventId) {
        const panel = document.getElementById('dispatchLogPanel');
        if (!panel) return;

        panel.innerHTML = '<div class="text-muted">加载中...</div>';

        try {
            const res = await fetch(api.eventDispatchLogs(eventId));
            const result = await res.json();
            if (result.code !== 200) {
                panel.innerHTML = `<div style="color: var(--danger);">${escapeHtml(result.message || '加载失败')}</div>`;
                return;
            }
            const logs = result.data || [];
            if (!logs.length) {
                panel.innerHTML = '<div class="text-muted">无投递日志</div>';
                return;
            }
            panel.innerHTML = logs.map(log => `
                <div class="dispatch-item">
                    <div><strong>channelId:</strong> ${escapeHtml(String(log.channelId ?? '-'))}</div>
                    <div><strong>status:</strong> ${renderStatusBadge(log.status)}</div>
                    <div><strong>retry:</strong> ${escapeHtml(String(log.retryCount ?? '-'))}</div>
                    <div><strong>time:</strong> ${formatTime(log.dispatchTime)}</div>
                    <div class="mono" style="margin-top:6px; white-space: pre-wrap;">${escapeHtml(log.response || '')}</div>
                </div>
            `).join('');
        } catch (e) {
            panel.innerHTML = `<div style="color: var(--danger);">${escapeHtml(e.message)}</div>`;
        }
    }

    function renderStatusBadge(status) {
        if (status === 'SENT') return '<span class="badge badge-success">SENT</span>';
        if (status === 'FAILED') return '<span class="badge badge-danger">FAILED</span>';
        if (status === 'SUPPRESSED') return '<span class="badge badge-warning">SUPPRESSED</span>';
        return `<span class="badge badge-info">${escapeHtml(status || 'NEW')}</span>`;
    }

    function appendIfPresent(params, key, value) {
        const v = (value || '').trim();
        if (v) params.set(key, v);
    }

    function datetimeLocalToIso(v) {
        if (!v) return '';
        const d = new Date(v);
        if (Number.isNaN(d.getTime())) return '';
        return d.toISOString();
    }

    function formatTime(v) {
        if (!v) return '-';
        const d = new Date(v);
        if (Number.isNaN(d.getTime())) return String(v);
        return d.toLocaleString();
    }

    function valueOf(id) {
        const el = document.getElementById(id);
        return el ? (el.value || '') : '';
    }

    function setValue(id, value) {
        const el = document.getElementById(id);
        if (el) el.value = value;
    }

    function escapeHtml(str) {
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function escapeAttr(str) {
        return escapeHtml(str).replace(/`/g, '&#96;');
    }
})();
