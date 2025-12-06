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
        const activeLanes = lanes.map(l => ({ ...l, collapsed: false, loading: true }));

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

        return {
            total: laneTasks.length,
            // Add other stats if needed in future
        };
    },

    async fetchLaneTasks(laneId) {
        try {
            const newTasks = await Api.fetchTasksByLane(laneId);

            // Remove old tasks for this lane (to avoid duplicates on re-fetch)
            // (Simpler: Filter out, then push new)
            this.tasks = this.tasks.filter(t => !t.swimLane || t.swimLane.id !== laneId);
            this.tasks.push(...newTasks);

            return newTasks;
        } catch (e) {
            console.error(`[Store] Failed to load tasks for lane ${laneId}`, e);
            throw e;
        }
    },

    toggleLaneCollapse(laneId) {
        const lane = this.lanes.find(l => l.id === laneId);
        if (lane) {
            lane.collapsed = !lane.collapsed;
        }
    },

    // --- Modal Management ---

    // 1. Confirm Modal
    confirmAction(actionName, payload) {
        console.log(`[Store] Opening Confirm Modal for ${actionName}`, payload);
        this.modal.payload = payload;
        this.modal.action = actionName;
        this.modal.open = true;

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
        // Focus Hack
        setTimeout(() => {
            const input = document.querySelector('[x-ref="inputField"]');
            if (input) input.focus();
        }, 100);
    },

    openTaskModal(laneId) {
        console.log(`[Store] Opening Task Input Modal for Lane ${laneId}`);
        this.inputModal.mode = 'TASK';
        this.inputModal.title = 'New Task';
        this.inputModal.value = '';
        this.inputModal.payload = { laneId }; // Store context
        this.inputModal.open = true;
        // Focus Hack
        setTimeout(() => {
            const input = document.querySelector('[x-ref="inputField"]');
            if (input) input.focus();
        }, 100);
    },

    closeInputModal() {
        console.log('[Store] Closing Input Modal');
        this.inputModal.open = false;
        setTimeout(() => {
            this.inputModal.value = '';
        }, 300);
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
            await this.createTask(val, laneId);
        }
        this.closeInputModal();
    },

    // --- Create Actions ---

    async createSwimLane(name) {
        try {
            // Optimistic? No, let's wait for ID from server for correctness
            const newLane = await Api.createSwimLane(name);
            newLane.collapsed = false;
            newLane.loading = false;
            this.lanes.push(newLane);
            this.triggerSave();
        } catch (e) {
            console.error('[Store] Failed to create lane', e);
            alert('Error creating lane');
        }
    },

    async createTask(name, laneId) {
        try {
            // Construct Payload
            const lane = this.lanes.find(l => l.id === laneId);
            if (!lane) return;

            const taskPayload = {
                name: name,
                status: 'TODO',
                swimLane: lane, // Backend expects object or ID? Service uses Entity.
                // Note: If backend expects just ID in DTO, this might fail if using raw Entity in controller.
                // Controller accepts 'Task' entity. It has 'swimLane' field.
                // Let's send the full object structure cleanly.
                swimLane: { id: laneId }
            };

            const newTask = await Api.createTask(taskPayload);

            // Add to local state (if not using SSE or strictly relying on it)
            // We use SSE for updates, but immediate feedback is nice.
            // Check if already covered by SSE?
            // If SSE is fast, we might duplicate.
            // Safe bet: Add it, and let deduplication logic in 'fetchLaneTasks' or 'onServerTaskUpdate' handle it.
            // Our 'onServerTaskUpdate' updates by ID, so it's safe.
            this.tasks.push(newTask);
            this.triggerSave();
        } catch (e) {
            console.error('[Store] Failed to create task', e);
            alert('Error creating task');
        }
    },

    // --- Actions ---

    async moveTaskOptimistic(taskId, newStatus, newLaneId) {
        // 1. Find Task
        const task = this.tasks.find(t => t.id == taskId);
        if (!task) return;

        // 2. Snapshot for rollback
        const originalStatus = task.status;
        const originalLaneId = task.swimLane?.id;

        // 3. Update Local State (Optimistic)
        task.status = newStatus;
        if (newLaneId) {
            // We need the full lane object for the relationship
            const lane = this.lanes.find(l => l.id == newLaneId);
            if (lane) task.swimLane = lane;
        }

        // 4. Send to API
        try {
            this.triggerSave();
            await Api.moveTask(taskId, newStatus, newLaneId);
            // Success - do nothing (state is already correct)
        } catch (e) {
            console.error('[Store] Move Failed, Rolling back');
            // Rollback
            task.status = originalStatus;
            const originalLane = this.lanes.find(l => l.id == originalLaneId);
            if (originalLane) task.swimLane = originalLane;
            alert('Failed to move task');
        }
    },

    async reorderLanesOptimistic(newIds) {
        // Sort local array based on new IDs Order
        this.triggerSave();
        try {
            await Api.reorderSwimlanes(newIds);
        } catch (e) {
            console.error('[Store] Lane Reorder Failed');
            // Re-fetch to sync
            this.loadData();
        }
    },

    async deleteLaneRecursive(laneId) {
        // Optimistic Remove
        const originalLanes = [...this.lanes];
        this.lanes = this.lanes.filter(l => l.id !== laneId);

        try {
            console.log(`[Store] Deleting lane ${laneId}`);
            await Api.deleteSwimLane(laneId);
            this.triggerSave();
        } catch (e) {
            console.error('[Store] Delete Failed', e);
            // Rollback
            this.lanes = originalLanes;
            alert('Failed to delete lane');
        }
    },

    async completeLaneRecursive(laneId) {
        // Optimistic Update
        const lane = this.lanes.find(l => l.id === laneId);
        if (lane) lane.isCompleted = true; // In active view, this might hide it if we filter

        // If we want it to vanish from "Active" view:
        this.lanes = this.lanes.filter(l => l.id !== laneId);

        try {
            console.log(`[Store] Completing lane ${laneId}`);
            await Api.completeSwimLane(laneId);
            this.triggerSave();
        } catch (e) {
            console.error('[Store] Complete Failed', e);
            if (lane) {
                // Restore
                lane.isCompleted = false;
                this.lanes.push(lane); // Hacky restore position??
                this.loadData(); // Safer
            }
        }
    },

    triggerSave() {
        this.showSaved = true;
        setTimeout(() => this.showSaved = false, 2000);
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
