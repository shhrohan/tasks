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
        onConfirm: null
    },
    columns: ['TODO', 'IN_PROGRESS', 'DONE', 'BLOCKED', 'DEFERRED'],

    // Init
    async init() {
        console.log('[Store] Initializing...');
        this.loading = true;
        // Reset Modal State to be safe
        this.modal.open = false;
        this.modal.type = 'info';

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

    // --- Modal Actions ---
    openModal({ title, message, type = 'info', confirmText = 'Confirm', onConfirm }) {
        this.modal.title = title;
        this.modal.message = message;
        this.modal.type = type;
        this.modal.confirmText = confirmText;
        this.modal.onConfirm = onConfirm;
        this.modal.open = true;
    },

    closeModal() {
        this.modal.open = false;
        setTimeout(() => {
            this.modal.onConfirm = null; // Cleanup
        }, 300);
    },

    confirmAction() {
        if (this.modal.onConfirm) {
            this.modal.onConfirm();
        }
        this.closeModal();
    },

    // --- Getters (Alpine works best with functions for computed data) ---

    getTasksByLaneAndStatus(laneId, status) {
        // Safe access to avoid "undefined" errors
        return this.tasks.filter(t =>
            t.swimLane &&
            t.swimLane.id === laneId &&
            t.status === status
        );
    },

    getLaneStats(laneId) {
        const tasks = this.tasks.filter(t => t.swimLane && t.swimLane.id === laneId);
        const total = tasks.length;
        const done = tasks.filter(t => t.status === 'DONE').length;
        return {
            total,
            done,
            percent: total === 0 ? 0 : Math.round((done / total) * 100)
        };
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

    async deleteLane(laneId) {
        if (!confirm('Are you sure you want to delete this swimlane?')) return;

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

    async completeLane(laneId) {
        if (!confirm('Mark this swimlane as complete?')) return;

        // Optimistic Update
        const lane = this.lanes.find(l => l.id === laneId);
        if (lane) lane.isCompleted = true;

        try {
            console.log(`[Store] Completing lane ${laneId}`);
            await Api.completeSwimLane(laneId);
            this.triggerSave();
        } catch (e) {
            console.error('[Store] Complete Failed', e);
            if (lane) lane.isCompleted = false; // Rollback
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
