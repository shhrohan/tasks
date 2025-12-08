/**
 * ============================================================================
 * APP.JS - Main Alpine.js Application Entry Point
 * ============================================================================
 * 
 * Purpose: Initializes the Alpine.js TodoApp component, manages state, 
 *          orchestrates data loading, and coordinates drag-and-drop initialization.
 * 
 * CRITICAL INITIALIZATION ORDER:
 * ------------------------------
 * 1. Alpine component is defined
 * 2. Alpine.start() is called
 * 3. init() fires (Alpine lifecycle)
 * 4. Lanes are loaded from API
 * 5. HTML for lanes/columns is rendered (x-init fires on columns - but EMPTY!)
 * 6. Tasks are loaded ASYNCHRONOUSLY per lane
 * 7. Task cards are rendered in DOM
 * 8. reinitSortableForLane() MUST be called to reinitialize Sortable with new cards
 * 
 * WHY reinitSortableForLane() IS CRITICAL:
 * ----------------------------------------
 * - Alpine's x-init fires when element is CREATED, not when children are rendered
 * - Tasks are loaded async, so columns are empty when x-init fires
 * - Sortable initialized on empty column = no draggable items!
 * - Solution: Destroy and recreate Sortable AFTER tasks are loaded
 * 
 * DEBUGGING TIPS:
 * ---------------
 * - Check console for "[Drag] Column has X task-card children" 
 *   If 0, Sortable won't work until reinit
 * - Check for "[App] reinitSortableForLane called" after task fetch
 * - Ensure no CSS 3D transforms on parents (see style.css comments)
 * 
 * ============================================================================
 */

import Alpine from 'https://esm.sh/alpinejs@3.12.0';
import { Store } from './modules/store.js';
import { Api } from './modules/api.js';
import { Drag } from './modules/drag.js';

console.log('[App] ====== Application Loading ======');
console.log('[App] Alpine.js ESM Module System');
console.log('[App] Modules imported: Store, Api, Drag');

// =============================================================================
// ALPINE.JS COMPONENT DEFINITION
// =============================================================================
Alpine.data('todoApp', () => ({
    // =========================================================================
    // REACTIVE STATE
    // These properties are watched by Alpine for changes
    // =========================================================================
    lanes: [],      // Array of swimlane objects
    tasks: [],      // Array of task objects (loaded per-lane)
    showSaved: false,
    columns: ['TODO', 'IN_PROGRESS', 'DONE', 'BLOCKED', 'DEFERRED'],

    // Import Store methods (modal management, API wrappers, etc.)
    ...Store,

    // =========================================================================
    // LIFECYCLE: init()
    // Called automatically by Alpine when component is mounted
    // =========================================================================
    async init() {
        console.log('[App] ====== Component Initializing ======');

        // ---------------------------------------------------------------------
        // DEBUG: Global click listener for task cards
        // Helps verify clicks are reaching task cards (not blocked by CSS)
        // ---------------------------------------------------------------------
        document.addEventListener('click', (e) => {
            if (e.target.closest('.task-card')) {
                console.log('[App] Task Card clicked:', {
                    taskId: e.target.closest('.task-card').getAttribute('data-task-id'),
                    target: e.target.tagName
                });
            }
        }, true); // Capture phase

        // ---------------------------------------------------------------------
        // STEP 1: Load Lanes
        // Lanes must load first so we know which tasks to fetch
        // ---------------------------------------------------------------------
        try {
            console.log('[App] Step 1: Loading lanes from API...');
            const data = await Store.loadData();
            this.lanes = data.lanes;
            console.log(`[App] Loaded ${this.lanes.length} lanes:`, this.lanes.map(l => l.name));

            // -----------------------------------------------------------------
            // STEP 2: Load Tasks Per Lane (ASYNC - runs in parallel)
            // IMPORTANT: Tasks are fetched asynchronously per lane.
            // The HTML columns are already rendered (x-init already fired!)
            // So we MUST reinitialize Sortable after each lane's tasks load.
            // -----------------------------------------------------------------
            console.log('[App] Step 2: Starting incremental task fetch...');

            this.lanes.forEach(async (lane) => {
                try {
                    console.log(`[App] Fetching tasks for lane ${lane.id} "${lane.name}"...`);
                    const newTasks = await Store.fetchLaneTasks(lane.id);
                    console.log(`[App] Lane ${lane.id} returned ${newTasks.length} tasks`);

                    // Update reactive array (Alpine detects this change)
                    const existingIds = new Set(newTasks.map(t => t.id));
                    this.tasks = this.tasks.filter(t =>
                        !((t.swimLane && t.swimLane.id === lane.id) || existingIds.has(t.id))
                    );
                    this.tasks.push(...newTasks);

                    // Mark lane as loaded and auto-expand if tasks exist
                    const localLane = this.lanes.find(l => l.id === lane.id);
                    if (localLane) {
                        localLane.loading = false;
                        if (newTasks.length > 0) {
                            localLane.collapsed = false;
                        }
                    }

                    // ---------------------------------------------------------
                    // CRITICAL: Reinitialize Sortable AFTER tasks are rendered
                    // $nextTick waits for Alpine to update the DOM
                    // ---------------------------------------------------------
                    this.$nextTick(() => {
                        console.log(`[App] DOM updated for lane ${lane.id}, reinitializing Sortable...`);
                        this.reinitSortableForLane(lane.id);
                    });

                } catch (e) {
                    console.error(`[App] Error fetching tasks for lane ${lane.id}:`, e);
                }
            });

        } catch (e) {
            console.error('[App] Failed to initialize:', e);
        }

        // ---------------------------------------------------------------------
        // STEP 3: Setup LANE-level Drag (for reordering swimlanes)
        // This only needs to run once after initial render
        // ---------------------------------------------------------------------
        this.$nextTick(() => {
            console.log('[App] Step 3: Setting up lane-level drag...');
            this.setupDrag();
        });

        // ---------------------------------------------------------------------
        // STEP 4: Setup Server-Sent Events for real-time updates
        // ---------------------------------------------------------------------
        console.log('[App] Step 4: Initializing SSE connection...');
        Api.initSSE(
            (t) => this.onServerTaskUpdate(t),
            (id) => this.onServerTaskDelete(id),
            (l) => console.log('[App] SSE Lane update received:', l)
        );

        console.log('[App] ====== Initialization Complete ======');
    },

    // =========================================================================
    // DRAG-AND-DROP SETUP
    // =========================================================================

    /**
     * Setup drag-and-drop for the board container (lane reordering only)
     * 
     * NOTE: Column-level Sortable is initialized via x-init="initColumn($el)"
     * in the HTML template, NOT here. This prevents premature initialization.
     */
    setupDrag() {
        console.log('[App] setupDrag: Initializing board-level drag (swimlanes)');

        const container = document.querySelector('.board-container');
        if (container) {
            Drag.initLaneSortable(container, Store, () => {
                console.log('[App] Lane reorder complete, callback fired');
            });
        } else {
            console.error('[App] ERROR: .board-container not found!');
        }

        // IMPORTANT: Do NOT call setupTaskSortables() here!
        // Column Sortable is handled by x-init and reinitSortableForLane()
    },

    /**
     * Initialize Sortable on all columns at once (emergency fallback only)
     * 
     * WARNING: This may initialize on empty columns if called too early.
     * Prefer using reinitSortableForLane() after async task loading.
     */
    setupTaskSortables() {
        console.warn('[App] setupTaskSortables called - this may init empty columns!');
        const columns = document.querySelectorAll('.lane-column');
        console.log(`[App] Found ${columns.length} columns to initialize`);
        columns.forEach(col => {
            Drag.initOneColumn(col, this);
        });
    },

    /**
     * Per-column initialization (called via x-init in HTML template)
     * 
     * NOTE: This fires when the column element is created, which is BEFORE
     * tasks are loaded. Sortable will be reinitialized by reinitSortableForLane().
     * 
     * @param {HTMLElement} el - The .lane-column element
     */
    initColumn(el) {
        const laneId = el.getAttribute('data-lane-id');
        const status = el.getAttribute('data-status');
        console.log(`[App] initColumn (x-init): Lane ${laneId}, Status ${status}`);

        // Initialize Sortable (may be empty at this point)
        Drag.initOneColumn(el, this);
    },

    /**
     * CRITICAL: Re-initialize Sortable for a lane AFTER tasks are loaded
     * 
     * This destroys existing Sortable instances and creates new ones with
     * the actual task cards now present in the DOM.
     * 
     * @param {number|string} laneId - The lane ID to reinitialize
     */
    reinitSortableForLane(laneId) {
        console.log(`[App] ====== Reinit Sortable for Lane ${laneId} ======`);

        // Find all columns for this specific lane
        const columns = document.querySelectorAll(`.lane-column[data-lane-id="${laneId}"]`);
        console.log(`[App] Found ${columns.length} columns for lane ${laneId}`);

        columns.forEach(col => {
            const status = col.getAttribute('data-status');
            const cardCount = col.querySelectorAll('.task-card').length;

            console.log(`[App] Column ${status}: ${cardCount} task cards`);

            // Destroy existing Sortable instance
            if (col.sortableInstance) {
                console.log(`[App] Destroying old Sortable for ${col.id}`);
                col.sortableInstance.destroy();
                col.sortableInstance = null;
            }

            // Create fresh Sortable with current DOM state
            Drag.initOneColumn(col, this);
        });

        console.log(`[App] ====== Reinit Complete for Lane ${laneId} ======`);
    },

    // =========================================================================
    // HELPER METHODS FOR TEMPLATES
    // =========================================================================

    /**
     * Get tasks filtered by lane and status
     * Used in x-for loops in the HTML template
     * 
     * @param {number} laneId - The swimlane ID
     * @param {string} status - The task status (TODO, IN_PROGRESS, etc.)
     * @returns {Array} Filtered and sorted tasks
     */
    getTasks(laneId, status) {
        const tasks = this.tasks.filter(t =>
            t.swimLane && t.swimLane.id === laneId && t.status === status
        ).sort((a, b) => {
            const posA = a.position !== null ? a.position : 999999;
            const posB = b.position !== null ? b.position : 999999;
            return posA - posB;
        });
        return tasks;
    },

    /**
     * Parse tags from JSON string or return existing array
     * 
     * @param {string|Array} tagsRaw - Tags as JSON string or array
     * @returns {Array} Parsed tags array
     */
    getTags(tagsRaw) {
        if (Array.isArray(tagsRaw)) return tagsRaw;
        try {
            return JSON.parse(tagsRaw || '[]');
        } catch {
            return [];
        }
    }
}));

// =============================================================================
// START ALPINE
// =============================================================================
window.Alpine = Alpine; // Expose globally for debugging in console
Alpine.start();
console.log('[App] ====== Alpine Started ======');
