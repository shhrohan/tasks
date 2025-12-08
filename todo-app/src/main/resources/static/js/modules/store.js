/*
 * store.js
 * centralized State Management for Alpine.js.
 * Imports Api to handle data persistence.
 */
import { Api } from './api.js';

export const Store = {
    // State
    lanes: [],
    tasks: [],
    loading: false,

    // UI State
    showSaved: false,
    saveTimeout: null,
    modal: {
        open: false,
        title: '',
        message: '',
        type: 'info',
        confirmText: 'Confirm',
        action: null, // Stores the function to call on confirm
        payload: null // Stores arguments
    },
    inputModal: {
        open: false,
        title: '',
        value: '',
        status: 'TODO', // For task creation
        tags: [],       // Array of tag strings
        tagInput: '',   // Current tag being typed
        laneName: '',   // Swimlane name for display
        mode: '', // 'TASK' or 'SWIMLANE'
        payload: null
    },
    columns: ['TODO', 'IN_PROGRESS', 'DONE', 'BLOCKED', 'DEFERRED'],

    // Init
    async init() {
        console.log('[Store] Initializing...');
        this.loading = true;
        // Reset Modal State
        this.closeModal();
        this.closeInputModal();

        try {
            await this.loadData();
            console.log('[Store] Data loaded');
        } catch (e) {
            console.error('[Store] Failed to load data', e);
        } finally {
            this.loading = false;
        }
    },

    async loadData() {
        // 1. Fetch Lanes First
        const lanes = await Api.fetchSwimLanes();

        // Initialize UI state for lanes (collapsed, etc)
        const activeLanes = lanes.map(l => ({ ...l, collapsed: true, loading: true }));

        return { lanes: activeLanes };
    },

    getLaneStats(laneId) {
        // Since we don't have this.tasks populated in Store (it's in App now mainly),
        // we might rely on the App spreading Store methods.
        // HOWEVER, 'this' context inside Store methods when called via Alpine component
        // will be the Alpine component instance.
        // So 'this.tasks' refers to the component's reactive tasks array.

        const tasks = this.tasks || [];
        const laneTasks = tasks.filter(t => t.swimLane && t.swimLane.id === laneId);
        const total = laneTasks.length;

        // Count tasks per status
        const todo = laneTasks.filter(t => t.status === 'TODO').length;
        const inProgress = laneTasks.filter(t => t.status === 'IN_PROGRESS').length;
        const done = laneTasks.filter(t => t.status === 'DONE').length;
        const blocked = laneTasks.filter(t => t.status === 'BLOCKED').length;
        const deferred = laneTasks.filter(t => t.status === 'DEFERRED').length;

        // Calculate percentages (avoid division by zero)
        const pct = (count) => total > 0 ? Math.round((count / total) * 100) : 0;

        console.log(`[Store] getLaneStats(${laneId}):`, { total, todo, inProgress, done, blocked, deferred });

        return {
            total,
            todo,
            inProgress,
            done,
            blocked,
            deferred,
            todoPct: pct(todo),
            inProgressPct: pct(inProgress),
            donePct: pct(done),
            blockedPct: pct(blocked),
            deferredPct: pct(deferred),
            completionPct: pct(done) // Overall completion = done tasks
        };
    },

    async fetchLaneTasks(laneId) {
        console.log('[Store] fetchLaneTasks - Calling API with:', { laneId });
        try {
            const newTasks = await Api.fetchTasksByLane(laneId);
            console.log(`[Store] fetchLaneTasks - Received ${newTasks.length} tasks for lane ${laneId}`);

            // Remove old tasks for this lane (to avoid duplicates on re-fetch)
            this.tasks = this.tasks.filter(t => !t.swimLane || t.swimLane.id !== laneId);
            this.tasks.push(...newTasks);

            return newTasks;
        } catch (e) {
            console.error(`[Store] fetchLaneTasks - FAILED for lane ${laneId}:`, e);
            throw e;
        }
    },

    toggleLaneCollapse(laneId) {
        const lane = this.lanes.find(l => l.id === laneId);
        if (lane) {
            lane.collapsed = !lane.collapsed;
        }
    },

    toggleAllLanes() {
        // Check if any lane is currently expanded (not collapsed)
        const anyExpanded = this.lanes.some(l => !l.collapsed);

        // If any are expanded, we want to collapse all (newState = true).
        // If all are already collapsed, we want to expand all (newState = false).
        const newState = anyExpanded ? true : false;

        this.lanes.forEach(l => l.collapsed = newState);
    },

    areAllLanesCollapsed() {
        return this.lanes.length > 0 && this.lanes.every(l => l.collapsed);
    },

    // --- Modal Management ---

    // 1. Confirm Modal
    confirmAction(actionName, payload) {
        console.log(`[Store] Opening Confirm Modal for ${actionName}`, payload);
        this.modal.payload = payload;
        this.modal.action = actionName;
        this.modal.open = true;
        document.body.style.overflow = 'hidden'; // Disable page scroll

        if (actionName === 'deleteLane') {
            this.modal.title = 'Delete Swimlane?';
            this.modal.message = 'This will archive the swimlane. You can restore it later from database/admin.';
            this.modal.type = 'danger';
            this.modal.confirmText = 'Delete';
        } else if (actionName === 'completeLane') {
            this.modal.title = 'Complete Swimlane?';
            this.modal.message = 'This will mark all tasks as done and hide the lane.';
            this.modal.type = 'success';
            this.modal.confirmText = 'Complete';
        }
    },

    closeModal() {
        console.log('[Store] Closing Confirm Modal');
        this.modal.open = false;
        document.body.style.overflow = ''; // Re-enable page scroll
        setTimeout(() => {
            this.modal.action = null;
            this.modal.payload = null;
        }, 300);
    },

    confirmModalAction() {
        console.log('[Store] Generic Confirm Action Triggered');
        const { action, payload } = this.modal;
        if (action === 'deleteLane') this.deleteLaneRecursive(payload);
        if (action === 'completeLane') this.completeLaneRecursive(payload);
        this.closeModal();
    },

    // 2. Input Modal
    openSwimlaneModal() {
        console.log('[Store] Opening Swimlane Input Modal');
        this.inputModal.mode = 'SWIMLANE';
        this.inputModal.title = 'New Board (Swimlane)';
        this.inputModal.value = '';
        this.inputModal.open = true;
        document.body.style.overflow = 'hidden'; // Disable page scroll
        // Focus Hack
        setTimeout(() => {
            const input = document.querySelector('[x-ref="inputField"]');
            if (input) input.focus();
        }, 100);
    },

    openTaskModal(laneId) {
        console.log(`[Store] Opening Task Input Modal for Lane ${laneId}`);
        const lane = this.lanes.find(l => l.id === laneId);
        const laneName = lane ? lane.name : 'Unknown';

        this.inputModal.mode = 'TASK';
        this.inputModal.title = 'New Task';
        this.inputModal.laneName = laneName;
        this.inputModal.value = '';
        this.inputModal.status = 'TODO';
        this.inputModal.tags = [];
        this.inputModal.tagInput = '';
        this.inputModal.payload = { laneId }; // Store context
        this.inputModal.open = true;
        document.body.style.overflow = 'hidden'; // Disable page scroll
        // Focus Hack
        setTimeout(() => {
            const input = document.querySelector('[x-ref="inputField"]');
            if (input) input.focus();
        }, 100);
    },



    closeInputModal() {
        console.log('[Store] Closing Input Modal');
        this.inputModal.open = false;
        document.body.style.overflow = ''; // Re-enable page scroll
        setTimeout(() => {
            this.inputModal.value = '';
            this.inputModal.status = 'TODO';
            this.inputModal.tags = [];
            this.inputModal.tagInput = '';
        }, 300);
    },

    // Tag management for input modal
    addTag() {
        const tag = this.inputModal.tagInput.trim();
        if (tag && !this.inputModal.tags.includes(tag)) {
            this.inputModal.tags.push(tag);
            console.log('[Store] Added tag:', tag, 'All tags:', this.inputModal.tags);
        }
        this.inputModal.tagInput = '';
    },

    removeTag(index) {
        const removed = this.inputModal.tags.splice(index, 1);
        console.log('[Store] Removed tag:', removed, 'Remaining tags:', this.inputModal.tags);
    },


    async submitInputModal() {
        console.log('[Store] Submitting Input Modal', this.inputModal);
        const val = this.inputModal.value.trim();
        if (!val) {
            console.warn('[Store] Input empty, ignoring');
            return;
        }

        if (this.inputModal.mode === 'SWIMLANE') {
            await this.createSwimLane(val);
        } else if (this.inputModal.mode === 'TASK') {
            const laneId = this.inputModal.payload.laneId;
            const status = this.inputModal.status || 'TODO';
            const tags = this.inputModal.tags || [];
            await this.createTask(val, laneId, status, tags);
        }
        this.closeInputModal();
    },

    // --- Create Actions ---

    async createSwimLane(name) {
        console.log('[Store] createSwimLane - Calling API with:', { name });
        try {
            const newLane = await Api.createSwimLane(name);
            console.log('[Store] createSwimLane - Created successfully:', newLane);
            newLane.collapsed = false;
            newLane.loading = false;
            this.lanes.push(newLane);
            this.triggerSave();
        } catch (e) {
            console.error('[Store] createSwimLane - FAILED:', e);
            alert('Error creating lane');
        }
    },

    async createTask(name, laneId, status = 'TODO', tags = []) {
        console.log('[Store] createTask - Calling API with:', { name, laneId, status, tags });
        try {
            const lane = this.lanes.find(l => l.id === laneId);
            if (!lane) {
                console.error('[Store] createTask - Lane not found:', laneId);
                return;
            }

            // Convert tags array to JSON string
            const tagsJson = tags.length > 0 ? JSON.stringify(tags) : null;

            const taskPayload = {
                name: name,
                status: status,
                tags: tagsJson,
                swimLane: { id: laneId }
            };
            console.log('[Store] createTask - Full payload:', taskPayload);

            const newTask = await Api.createTask(taskPayload);
            console.log('[Store] createTask - Created successfully:', newTask);

            this.tasks.push(newTask);
            this.triggerSave();
        } catch (e) {
            console.error('[Store] createTask - FAILED:', e);
            alert('Error creating task');
        }
    },

    // --- Actions ---

    async moveTaskOptimistic(taskId, newStatus, newLaneId, newIndex) {
        console.log('[Store] moveTaskOptimistic - Params:', { taskId, newStatus, newLaneId, newIndex });

        // 1. Find Task
        const task = this.tasks.find(t => t.id == taskId);
        if (!task) {
            console.error('[Store] moveTaskOptimistic - Task not found:', taskId);
            return;
        }

        // 2. Snapshot for rollback
        const originalStatus = task.status;
        const originalLaneId = task.swimLane?.id;
        const originalPosition = task.position;
        console.log('[Store] moveTaskOptimistic - Original state:', { originalStatus, originalLaneId, originalPosition });

        // 3. Update Local State (Optimistic)
        task.status = newStatus;
        if (newLaneId) {
            const lane = this.lanes.find(l => l.id == newLaneId);
            if (lane) task.swimLane = lane;
        }

        if (newIndex !== undefined && newIndex !== null) {
            task.position = newIndex;
        }

        // 4. Send to API
        try {
            this.triggerSave();
            console.log('[Store] moveTaskOptimistic - Calling API.moveTask with:', { taskId, newStatus, newLaneId, newIndex });
            await Api.moveTask(taskId, newStatus, newLaneId, newIndex);
            console.log('[Store] moveTaskOptimistic - API call successful');
        } catch (e) {
            console.error('[Store] moveTaskOptimistic - FAILED, rolling back:', e);
            task.status = originalStatus;
            const originalLane = this.lanes.find(l => l.id == originalLaneId);
            if (originalLane) task.swimLane = originalLane;
            task.position = originalPosition;
            alert('Failed to move task');
        }
    },

    async reorderLanesOptimistic(newIds) {
        console.log('[Store] reorderLanesOptimistic - Calling API with:', { newIds });
        this.triggerSave();
        try {
            await Api.reorderSwimlanes(newIds);
            console.log('[Store] reorderLanesOptimistic - Reorder successful');
        } catch (e) {
            console.error('[Store] reorderLanesOptimistic - FAILED:', e);
            this.loadData();
        }
    },

    async deleteLaneRecursive(laneId) {
        console.log('[Store] deleteLaneRecursive - Calling API with:', { laneId });
        const originalLanes = [...this.lanes];
        this.lanes = this.lanes.filter(l => l.id !== laneId);

        try {
            await Api.deleteSwimLane(laneId);
            console.log('[Store] deleteLaneRecursive - Deleted successfully');
            this.triggerSave();
        } catch (e) {
            console.error('[Store] deleteLaneRecursive - FAILED:', e);
            this.lanes = originalLanes;
            alert('Failed to delete lane');
        }
    },

    async completeLaneRecursive(laneId) {
        console.log('[Store] completeLaneRecursive - Calling API with:', { laneId });
        const lane = this.lanes.find(l => l.id === laneId);
        if (lane) lane.isCompleted = true;

        this.lanes = this.lanes.filter(l => l.id !== laneId);

        try {
            await Api.completeSwimLane(laneId);
            console.log('[Store] completeLaneRecursive - Completed successfully');
            this.triggerSave();
        } catch (e) {
            console.error('[Store] completeLaneRecursive - FAILED:', e);
            if (lane) {
                lane.isCompleted = false;
                this.lanes.push(lane);
                this.loadData();
            }
        }
    },

    triggerSave() {
        this.showSaved = true;
        if (this.saveTimeout) clearTimeout(this.saveTimeout);
        this.saveTimeout = setTimeout(() => this.showSaved = false, 1500);
    },

    // --- SSE Handlers ---
    onServerTaskUpdate(updatedTask) {
        const index = this.tasks.findIndex(t => t.id === updatedTask.id);
        if (index !== -1) {
            // Update in place (safer for reactivity)
            this.tasks[index] = updatedTask;
        } else {
            this.tasks.push(updatedTask);
        }
        this.triggerSave();
    },

    onServerTaskDelete(id) {
        this.tasks = this.tasks.filter(t => t.id !== id);
    }
};
