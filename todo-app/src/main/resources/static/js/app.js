/*
 * app.v13.js
 * Main Entry Point.
 * Assembles Modules and Initializes Alpine via ESM.
 */
import Alpine from 'https://esm.sh/alpinejs@3.12.0';
import { Store } from './modules/store.js';
import { Api } from './modules/api.js';
import { Drag } from './modules/drag.js';

console.log('[App] Loading v13 ESM...');

// Define the Main Component
Alpine.data('todoApp', () => ({
    // Explicit Reactive State
    lanes: [],
    tasks: [],
    showSaved: false,
    columns: ['TODO', 'IN_PROGRESS', 'DONE', 'BLOCKED', 'DEFERRED'],

    ...Store, // Import methods

    async init() {
        console.log('[App] Alpine Init');

        // 1. Data Load (Lanes first)
        try {
            const data = await Store.loadData();
            this.lanes = data.lanes;
            // this.tasks = data.tasks; // No longer bulk loading

            console.log('[App] Lanes loaded. Starting incremental task fetch...');

            // 2. Incremental Task Loading
            console.log('[App] Starting incremental fetch for', this.lanes.length, 'lanes');

            this.lanes.forEach(async (lane) => {
                try {
                    console.log(`[App] Fetching tasks for lane ${lane.id} (${lane.name})...`);
                    // Store fetches and returns the tasks
                    const newTasks = await Store.fetchLaneTasks(lane.id);

                    console.log(`[App] Lane ${lane.id} returned ${newTasks.length} tasks.`);

                    // Reactivity: Update the component's reactive array
                    // We must ensure we trigger Alpine's reactivity system.
                    // Filter out any existing tasks for this lane to prevent dups
                    const existingIds = new Set(newTasks.map(t => t.id));
                    this.tasks = this.tasks.filter(t =>
                        !((t.swimLane && t.swimLane.id === lane.id) || existingIds.has(t.id))
                    );
                    // Push new ones
                    this.tasks.push(...newTasks);

                    // Update Lane Loading State
                    const localLane = this.lanes.find(l => l.id === lane.id);
                    if (localLane) localLane.loading = false;

                } catch (e) {
                    console.error(`[App] Error fetching lane ${lane.id}:`, e);
                }
            });

        } catch (e) {
            console.error('[App] Failed to init:', e);
        }

        // 3. Setup Drag & Drop (After DOM render)
        this.$nextTick(() => {
            this.setupDrag();
        });

        // 4. Setup SSE
        Api.initSSE(
            (t) => this.onServerTaskUpdate(t), // Bind to component method
            (id) => this.onServerTaskDelete(id),
            (l) => console.log('Lane update', l)
        );
    },

    setupDrag() {
        // Binds Sortable to Tasks
        Drag.initTaskSortables(Store);

        // Binds Sortable to Lanes
        const container = document.querySelector('.board-container');
        if (container) {
            Drag.initLaneSortable(container, Store, () => {
                // Flash Refresh Callback if needed
            });
        }
    },

    // Helper wrappers for template access
    getTasks(laneId, status) {
        // FIX: Use 'this.tasks' (Reactive Component State)
        // Do NOT delegate to Store (Non-reactive Module State)
        const tasks = this.tasks.filter(t =>
            t.swimLane && t.swimLane.id === laneId && t.status === status
        );
        return tasks;
    },

    // Helper to formatting tags correctly for display
    getTags(tagsRaw) {
        if (Array.isArray(tagsRaw)) return tagsRaw;
        try {
            return JSON.parse(tagsRaw || '[]');
        } catch {
            return [];
        }
    }
}));

// Start Alpine
window.Alpine = Alpine; // Optional: for debugging
Alpine.start();
console.log('[App] Alpine Started');
