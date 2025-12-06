/*
 * app.v11.js
 * Main Entry Point.
 * Assembles Modules and Initializes Alpine.
 */
import { Store } from './modules/store.js';
import { Api } from './modules/api.js';
import { Drag } from './modules/drag.js';

console.log('[App] Loading v11 Modular...');

document.addEventListener('alpine:init', () => {

    // Define the Main Component
    Alpine.data('todoApp', () => ({
        ...Store, // Merge Store state/methods directly into component scope

        async init() {
            console.log('[App] Alpine Init');

            // 1. Data Load
            await Store.init();

            // 2. Setup Drag & Drop (After DOM render)
            this.$nextTick(() => {
                this.setupDrag();
            });

            // 3. Setup SSE
            Api.initSSE(
                (t) => Store.onServerTaskUpdate(t),
                (id) => Store.onServerTaskDelete(id),
                (l) => console.log('Lane update', l) // TODO: Handle lane updates if needed
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
                    // For now, simpler is better.
                });
            }
        },

        // Helper wrappers for template access
        // (Alpine proxies existing Store methods, but we ensures helpers exist)
        getTasks(laneId, status) {
            return Store.getTasksByLaneAndStatus(laneId, status);
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
});
