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
    selectedTask: null,
    loading: false,

    // UI State
    showSaved: false,
    columns: ['TODO', 'IN_PROGRESS', 'DONE', 'BLOCKED', 'DEFERRED'],

    // Init
    async init() {
        console.log('[Store] Initializing...');
        this.loading = true;
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
        const [lanes, tasks] = await Promise.all([
            Api.fetchSwimLanes(),
            Api.fetchTasks()
        ]);
        this.lanes = lanes;
        this.tasks = tasks;
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

    selectTask(task) {
        console.log('[Store] Selecting task', task.name);
        // Clone to avoid direct mutation issues until ready
        this.selectedTask = { ...task };
        // Ensure tags/comments are consistent formats if needed
        // (Assuming backend sends them correctly now)
    },

    deselectTask() {
        this.selectedTask = null;
    },

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

    triggerSave() {
        this.showSaved = true;
        setTimeout(() => this.showSaved = false, 2000);
    },

    // --- SSE Handlers ---
    onServerTaskUpdate(updatedTask) {
        const index = this.tasks.findIndex(t => t.id === updatedTask.id);
        if (index !== -1) {
            // Update in place to preserve reference if possible, or replace
            // Replacing is safer for reactivity in Alpine v3
            this.tasks[index] = updatedTask;

            // If selected, update that too
            if (this.selectedTask && this.selectedTask.id === updatedTask.id) {
                this.selectedTask = { ...updatedTask };
            }
        } else {
            this.tasks.push(updatedTask);
        }
        this.triggerSave();
    },

    onServerTaskDelete(id) {
        this.tasks = this.tasks.filter(t => t.id !== id);
        if (this.selectedTask && this.selectedTask.id === id) {
            this.selectedTask = null;
        }
    }
};
