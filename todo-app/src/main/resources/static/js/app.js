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

    // Task filters
    hideDone: false,
    showOnlyBlocked: false,
    selectedTags: [], // Faceted filter state

    // Loading state for skeleton loaders
    isLoading: true,  // Shows skeletons until tasks load

    // Mobile sidebar navigation
    mobileSidebarOpen: false,  // Toggle sidebar drawer
    sidebarPinned: false,      // Pin sidebar open (don't auto-close on lane select)
    activeLaneId: null,        // Currently selected lane on mobile (null = show all)
    isMobile: false,           // Track if we're on mobile viewport

    // Task keyboard navigation (main view)
    selectedTaskId: null,      // Currently keyboard-selected task in main view

    // --- Drag Helpers (Event-Based Protection) ---

    prepareDrag(taskId) {
        // console.log('[App] prepareDrag - Adding x-ignore to task', taskId);
        const el = document.querySelector(`[data-task-id="${taskId}"]`);
        if (el) {
            el.setAttribute('x-ignore', '');
        }
    },

    cleanupDrag(taskId) {
        // console.log('[App] cleanupDrag - Removing x-ignore from task', taskId);
        const el = document.querySelector(`[data-task-id="${taskId}"]`);
        if (el) {
            el.removeAttribute('x-ignore');
        }
    },

    // --- Task Resize Feature ---State (Desktop Only)
    taskDetail: {
        open: false,
        task: null,
        newComment: '',
        isLoading: false,
        editingCommentId: null,
        editingCommentText: '',
        selectedCommentIndex: -1,  // For arrow key navigation
        tagInput: ''               // For adding new tags
    },

    // Import Store methods (modal management, API wrappers, etc.)
    ...Store,

    // =========================================================================
    // LIFECYCLE: init()
    // Called automatically by Alpine when component is mounted
    // =========================================================================
    async init() {
        console.log('[App] ====== Component Initializing ======');
        // Ensure taskDetail is fully initialized to prevent Alpine errors
        this.taskDetail = { ...this.taskDetail, tagInput: '' };

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
        // STEP 1: Load Lanes and Tasks
        // Use server-side initial data if available (reduces CLS)
        // Falls back to async API fetch if not available
        // ---------------------------------------------------------------------
        try {
            // Check for server-side initial data (embedded in script#initial-data)
            const dataScript = document.getElementById('initial-data');
            console.log('[App] DEBUG: Looking for #initial-data script tag...');
            console.log('[App] DEBUG: dataScript element:', dataScript);

            if (dataScript) {
                console.log('[App] Step 1: Found server-side initial data, parsing...');
                const rawJson = dataScript.textContent;
                console.log('[App] DEBUG: Raw JSON length:', rawJson ? rawJson.length : 0);
                console.log('[App] DEBUG: First 200 chars:', rawJson ? rawJson.substring(0, 200) : 'null');

                try {
                    const initialData = JSON.parse(rawJson);
                    console.log('[App] DEBUG: JSON parsed successfully');

                    this.lanes = initialData.lanes || [];
                    this.tasks = initialData.tasks || [];

                    console.log(`[App] Loaded ${this.lanes.length} lanes and ${this.tasks.length} tasks from initial data`);

                    // Initial Load: Force collapsed state on Desktop (Optimization requested)
                    if (!this.isMobile) {
                        console.log('[App] Desktop mode: clearing initial tasks for lazy loading');
                        this.tasks = [];
                    }

                    this.lanes.forEach(lane => {
                        lane.loading = false;
                        lane.tasksLoaded = false;

                        if (!this.isMobile) {
                            lane.collapsed = true;
                        } else {
                            // Mobile: Keep existing logic (expand if tasks present in initial data)
                            const laneTasks = this.tasks.filter(t => t.swimLane && t.swimLane.id === lane.id);
                            lane.collapsed = laneTasks.length === 0;
                            if (laneTasks.length > 0) lane.tasksLoaded = true;
                        }
                    });

                    // Initialize Sortable after DOM renders
                    this.$nextTick(() => {
                        this.lanes.forEach(lane => {
                            this.reinitSortableForLane(lane.id);
                        });
                    });

                    this.isLoading = false;

                } catch (parseError) {
                    console.error('[App] DEBUG: JSON parse failed:', parseError);
                    console.log('[App] Falling back to API fetch due to parse error...');
                    await this.loadViaApi();
                }

            } else {
                // Fallback: Async API fetch (slower, causes CLS)
                console.log('[App] Step 1: No initial data script found, falling back to API fetch...');
                await this.loadViaApi();
            }

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

        // Start proactive connection monitoring
        Api.startConnectionMonitor();

        console.log('[App] ====== Initialization Complete ======');

        // ---------------------------------------------------------------------
        // STEP 5: Setup mobile detection and set initial active lane
        // ---------------------------------------------------------------------
        this.checkMobile();
        window.addEventListener('resize', () => this.checkMobile());

        // Set first lane as active on mobile if we have lanes
        if (this.lanes.length > 0 && !this.activeLaneId) {
            this.activeLaneId = this.lanes[0].id;
        }

        // Watch for mobile viewport changes to enable/disable lane reordering
        this.$watch('isMobile', (isMobile) => {
            console.log('[App] isMobile changed:', isMobile);
            const shouldDisableLanes = isMobile;

            const boardContainer = document.querySelector('.board-container');
            if (boardContainer && boardContainer.sortableInstance) {
                console.log(`[App] Updating lane Sortable disabled state to: ${shouldDisableLanes}`);
                boardContainer.sortableInstance.option('disabled', shouldDisableLanes);
            }
        });

        // --- Auto-Expand Watchers ---
        this.$watch('viewMode', () => this.checkSingleLaneAutoExpand());
        this.$watch('hideDone', () => this.checkSingleLaneAutoExpand());
        this.$watch('showOnlyBlocked', () => this.checkSingleLaneAutoExpand());

        // ---------------------------------------------------------------------
        // STEP 7: Global Keyboard Navigation
        // ---------------------------------------------------------------------
        document.addEventListener('keydown', (e) => this.handleGlobalKeydown(e));

        // ---------------------------------------------------------------------
        // STEP 8: Initial Auto-Expand Check
        // Ensure single-visible lane expands on initial load/reload
        // ---------------------------------------------------------------------
        this.checkSingleLaneAutoExpand();
    },

    /**
     * Check if current viewport is mobile (<992px)
     * Updates isMobile state and resets sidebar if switching to desktop
     */
    checkMobile() {
        const wasMobile = this.isMobile;
        this.isMobile = window.innerWidth < 992;

        // If switching from mobile to desktop, close sidebar and show all lanes
        if (wasMobile && !this.isMobile) {
            this.mobileSidebarOpen = false;
        }


    },

    /**
     * Toggle mobile sidebar open/close
     */
    toggleMobileSidebar() {
        this.mobileSidebarOpen = !this.mobileSidebarOpen;
        // Always use push mode - content shrinks when sidebar is open
        this.sidebarPinned = this.mobileSidebarOpen;
        console.log('[App] toggleMobileSidebar:', this.mobileSidebarOpen, 'pinned:', this.sidebarPinned);
        this.updateSidebarBodyClasses();
    },

    /**
     * Select a swimlane on mobile (closes sidebar only if not pinned)
     * Now also triggers instant lazy loading of tasks
     */
    async selectLane(laneId) {
        console.log('[App] selectLane:', laneId);
        this.activeLaneId = laneId;

        // Auto-close sidebar on very small screens (iPhone SE etc)
        // since the sidebar covers the entire view
        if (window.innerWidth <= 480) {
            this.mobileSidebarOpen = false;
        }

        // Find the lane and trigger lazy load if tasks not loaded
        const lane = this.lanes.find(l => l.id === laneId);
        if (lane && !lane.tasksLoaded) {
            console.log(`[App] selectLane: Lazy loading tasks for lane ${laneId}`);
            lane.loading = true;
            lane.collapsed = false; // Expand the lane
            try {
                await this.fetchLaneTasks(laneId);
                lane.tasksLoaded = true;
                this.$nextTick(() => {
                    this.reinitSortableForLane(laneId);
                });
            } catch (e) {
                console.error(`[App] Failed to load tasks for lane ${laneId}:`, e);
                this.showError('Failed to load tasks');
            } finally {
                lane.loading = false;
            }
        } else if (lane) {
            lane.collapsed = false; // Just expand if already loaded
        }

        // Sidebar persistence: removed auto-close logic here.
        // User now explicitly closes sidebar via Lanes button or backdrop.

        // Scroll to top of content
        window.scrollTo({ top: 0, behavior: 'smooth' });
    },

    /**
     * Toggle sidebar pinned state
     */
    toggleSidebarPin() {
        this.sidebarPinned = !this.sidebarPinned;
        console.log('[App] toggleSidebarPin:', this.sidebarPinned);
        this.updateSidebarBodyClasses();
    },

    /**
     * Update body classes for sidebar state (used by CSS for push layout)
     */
    updateSidebarBodyClasses() {
        const body = document.body;
        if (this.mobileSidebarOpen) {
            body.classList.add('sidebar-open');
        } else {
            body.classList.remove('sidebar-open');
        }
        if (this.sidebarPinned) {
            body.classList.add('sidebar-pinned');
        } else {
            body.classList.remove('sidebar-pinned');
        }
    },

    /**
     * Check if a lane should be visible
     * - On desktop: always show lanes matching view mode
     * - On mobile: show only active lane
     */
    isLaneVisible(laneId) {
        const lane = this.lanes.find(l => l.id === laneId);
        if (!lane) return false;

        // --- View Mode Filter ---
        // 'Active' mode shows only non-completed lanes
        // 'Completed' mode shows only completed lanes
        const matchesMode = (this.viewMode === 'ACTIVE' && !lane.isCompleted) ||
            (this.viewMode === 'COMPLETED' && lane.isCompleted);

        if (!matchesMode) return false;

        // --- Device Filters ---
        // On desktop, show all lanes matching view mode
        if (!this.isMobile) {
            return true;
        }

        // On mobile: show only active lane
        return laneId === this.activeLaneId;
    },

    isSidebarLaneVisible(laneId) {
        const lane = this.lanes.find(l => l.id === laneId);
        if (!lane) return false;

        // Sidebar should only filter by Active/Completed view mode
        // Tag filters should NOT affect sidebar visibility
        const matchesMode = (this.viewMode === 'ACTIVE' && !lane.isCompleted) ||
            (this.viewMode === 'COMPLETED' && lane.isCompleted);

        return matchesMode;
    },

    /**
     * Override Store.toggleAllLanes to support lazy-loading
     */
    async toggleAllLanes() {
        const anyExpanded = this.lanes.some(l => !l.collapsed);
        const targetCollapsedState = anyExpanded ? true : false;

        console.log(`[App] toggleAllLanes - Setting collapsed state to: ${targetCollapsedState}`);

        this.lanes.forEach(lane => {
            lane.collapsed = targetCollapsedState;
        });

        // If we just expanded all, trigger lazy load for all visible lanes
        if (!targetCollapsedState) {
            const lanesToLoad = this.lanes.filter(l =>
                !l.tasksLoaded && this.isLaneVisible(l.id)
            );

            const loadPromises = lanesToLoad.map(async (lane) => {
                lane.loading = true;
                try {
                    await this.fetchLaneTasks(lane.id);
                    lane.tasksLoaded = true;
                    this.$nextTick(() => {
                        this.reinitSortableForLane(lane.id);
                    });
                } catch (e) {
                    console.error(`[App] Failed to lazy load lane ${lane.id}:`, e);
                } finally {
                    lane.loading = false;
                }
            });
            await Promise.all(loadPromises);
        }
    },

    /**
     * Override Store.toggleLaneCollapse to add lazy-loading for desktop
     */
    async toggleLaneCollapse(laneId) {
        const lane = this.lanes.find(l => l.id === laneId);
        if (!lane) return;

        // Toggle collapsed state
        lane.collapsed = !lane.collapsed;
        console.log(`[App] Lane ${laneId} toggled to: ${lane.collapsed ? 'collapsed' : 'expanded'}`);

        // Lazy load tasks if expanding for the first time
        if (!lane.collapsed && !lane.tasksLoaded) {
            console.log(`[App] Desktop Optimization: Lazy fetching tasks for lane ${laneId}`);
            lane.loading = true;
            try {
                // this.fetchLaneTasks already pushes tasks to this.tasks
                await this.fetchLaneTasks(laneId);
                lane.tasksLoaded = true;

                // Re-init Sortable so drag-and-drop works for newly loaded cards
                this.$nextTick(() => {
                    this.reinitSortableForLane(laneId);
                });
            } catch (e) {
                console.error(`[App] Failed to lazy load tasks for lane ${laneId}:`, e);
                this.showError('Failed to load tasks');
            } finally {
                lane.loading = false;
            }
        }

    },

    /**
     * Get lane name by ID (for sidebar display)
     */
    getLaneName(laneId) {
        const lane = this.lanes.find(l => l.id === laneId);
        return lane ? lane.name : '';
    },

    /**
     * Fallback: Load data via async API calls (slower, causes CLS)
     * Used when server-side initial data is not available or fails to parse
     */
    async loadViaApi() {
        console.log('[App] loadViaApi: Starting async API fetch...');
        const data = await this.loadData();
        this.lanes = data.lanes;

        // CRITICAL BUG FIX: Clear tasks array before re-fetching
        this.tasks = [];

        console.log(`[App] Loaded ${this.lanes.length} lanes via API. Tasks cleared.`);

        if (this.isMobile) {
            console.log('[App] loadViaApi: Mobile mode - eager loading all tasks');
            // Mobile: Eager load all tasks to keep swipe transitions fast
            const lanePromises = this.lanes.map(async (lane) => {
                try {
                    const newTasks = await this.fetchLaneTasks(lane.id);
                    const localLane = this.lanes.find(l => l.id === lane.id);
                    if (localLane) {
                        localLane.loading = false;
                        localLane.tasksLoaded = true;
                        localLane.collapsed = newTasks.length === 0;
                    }
                    this.$nextTick(() => {
                        this.reinitSortableForLane(lane.id);
                    });
                } catch (e) {
                    console.error(`[App] Error fetching tasks for lane ${lane.id}:`, e);
                }
            });
            await Promise.all(lanePromises);
        } else {
            console.log('[App] loadViaApi: Desktop mode - lazy loading tasks (lanes starting collapsed)');
            // Desktop: Optimization - lanes start collapsed and empty
            this.lanes.forEach(lane => {
                lane.loading = false;
                lane.collapsed = true;
                lane.tasksLoaded = false;
            });
        }

        console.log('[App] Data load phase complete');
        this.isLoading = false;
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
            Drag.initLaneSortable(container, this, () => {
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
        // console.log(`[App] reinitSortableForLane: ${laneId}`);
        const laneEl = document.getElementById(`lane-${laneId}-container`); // Ensure we target correct container if needed

        // Find all columns for this lane
        // They should have ID: lane-{laneId}-{status}
        // OR we can querySelector by data-lane-id
        const columns = document.querySelectorAll(`.lane-column[data-lane-id="${laneId}"]`);

        if (columns.length === 0) {
            console.warn(`[App] reinitSortableForLane: No columns found for lane ${laneId}`);
            return;
        }

        columns.forEach(col => {
            // Destroy existing instance if it exists to prevent duplicates
            if (col.sortableInstance) {
                // console.log(`[App] Destroying old Sortable for lane ${laneId} status ${col.getAttribute('data-status')}`);
                col.sortableInstance.destroy();
                col.sortableInstance = null;
            }

            // Re-init
            Drag.initOneColumn(col, this, Alpine);
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
        // 1. Initial Candidate Set (Lane + Status matched)
        let tasks = this.tasks.filter(t =>
            t.swimLane && t.swimLane.id === laneId && t.status === status
        );

        // 2. Global Status Filters
        if (this.hideDone && status === 'DONE') {
            return [];
        }
        if (this.showOnlyBlocked && status !== 'BLOCKED') {
            return [];
        }

        // 3. Faceted Tag Filtering (AND Logic)
        if (this.selectedTags.length > 0) {
            tasks = tasks.filter(task => {
                const taskTags = this.parseTags(task.tags);
                // Task must have ALL selected tags
                return this.selectedTags.every(tag => taskTags.includes(tag));
            });
        }

        // 4. Sort by Position
        return tasks.sort((a, b) => (a.position || 0) - (b.position || 0));
    },

    /**
     * Get lanes sorted by relevance (match count) if filtering is active
     */
    getSortedLanes() {
        // If not filtering or on mobile (users requested simple view), return default order
        // Check window width for mobile breakpoint (lg = 992px)
        const isMobile = window.innerWidth < 992;

        if (this.selectedTags.length === 0 || isMobile) {
            return this.lanes;
        }

        // Create a shallow copy to sort
        return [...this.lanes].sort((a, b) => {
            const countA = this.countMatchingTasks(a.id);
            const countB = this.countMatchingTasks(b.id);

            // Primary Sort: Match Count (Descending)
            if (countB !== countA) {
                return countB - countA;
            }

            // Secondary Sort: Original Position (Ascending)
            return (a.position || 0) - (b.position || 0);
        });
    },

    /**
     * Count total matching tasks in a lane (across all columns)
     * Helper for sorting
     */
    countMatchingTasks(laneId) {
        return this.columns.reduce((total, status) => {
            return total + this.getTasks(laneId, status).length;
        }, 0);
    },

    /**
     * Check if a lane has any visible tasks across relevant columns
     * Used to hide empty lanes when filtering
     */
    laneHasMatchingTasks(laneId) {
        // If no filters are active, show all lanes (or keep standard behavior)
        if (this.selectedTags.length === 0 && !this.hideDone && !this.showOnlyBlocked) return true;

        // Check if ANY column has tasks for this lane
        return this.columns.some(status => this.getTasks(laneId, status).length > 0);
    },

    /**
     * Check if a specific status column in a lane has any tasks
     */
    columnHasMatchingTasks(laneId, status) {
        return this.getTasks(laneId, status).length > 0;
    },

    /**
     * Complete visibility logic for a status column
     * Only hide empty columns if we are aggressively filtering by tags
     */
    isColumnVisible(laneId, status) {
        // Always show columns in normal view
        if (this.selectedTags.length === 0) return true;

        // In filtered view, hide empty columns to reduce clutter? 
        // Or keep them valid drop targets? 
        // Let's keep them visible for now to avoid layout shift, 
        // OR return true.
        // The error was "isColumnVisible is not defined", so we MUST define it.
        return true;
    },

    // --- Tag Logic ---

    parseTags(tagStringOrArray) {
        if (!tagStringOrArray) return [];
        if (Array.isArray(tagStringOrArray)) return tagStringOrArray;
        try {
            const parsed = JSON.parse(tagStringOrArray);
            return Array.isArray(parsed) ? parsed : [];
        } catch (e) {
            return [];
        }
    },

    getAllUniqueTags() {
        // Collect all distinct tags from ALL loaded tasks to allow discovery
        const allTags = new Set();
        this.tasks.forEach(task => {
            if (this.hideDone && task.status === 'DONE') return; // Optional: Hide tags only found in Done?
            const tags = this.parseTags(task.tags);
            tags.forEach(t => allTags.add(t));
        });
        return Array.from(allTags).sort();
    },

    /**
     * Get tags that are present in the currently filtered task set.
     * Used to fade/disable tags that would lead to 0 results if added.
     */
    activeUniqueTags() {
        // 1. Start with all tasks matching Global Filters
        let candidateTasks = this.tasks.filter(t => {
            if (this.hideDone && t.status === 'DONE') return false;
            // if (this.showOnlyBlocked && t.status !== 'BLOCKED') return false; // Maybe don't filter tags by Blocked-only view? Let's be consistent.
            return true;
        });

        // 2. Filter by CURRENTLY selected tags
        if (this.selectedTags.length > 0) {
            candidateTasks = candidateTasks.filter(task => {
                const taskTags = this.parseTags(task.tags);
                return this.selectedTags.every(tag => taskTags.includes(tag));
            });
        }

        // 3. Collect unique tags from these tasks
        const validTags = new Set();
        candidateTasks.forEach(task => {
            const tags = this.parseTags(task.tags);
            tags.forEach(t => validTags.add(t));
        });

        return Array.from(validTags);
    },

    toggleTag(tag) {
        if (this.selectedTags.includes(tag)) {
            this.selectedTags = this.selectedTags.filter(t => t !== tag);
        } else {
            this.selectedTags.push(tag);
        }
        this.applySmartCollapse();
    },

    isTagSelected(tag) {
        return this.selectedTags.includes(tag);
    },

    clearSelectedTags() {
        this.selectedTags = [];
        this.applySmartCollapse();
    },

    /**
     * Smart Collapse Logic:
     * When filtering by tags (Desktop), automatically expand lanes with matches
     * and collapse lanes with 0 matches.
     */
    applySmartCollapse() {
        // Only apply on Desktop (matches sorting logic)
        if (window.innerWidth < 992) return;

        if (this.selectedTags.length > 0) {
            this.lanes.forEach(lane => {
                const matchCount = this.countMatchingTasks(lane.id);
                // Expand if matches > 0, Collapse if 0
                lane.collapsed = (matchCount === 0);
            });
        } else {
            // If filters cleared, maybe expand all? Or leave as is?
            // Let's expand all to restore visibility
            this.lanes.forEach(lane => lane.collapsed = false);
        }
    },

    /**
     * Auto-expand logic: if only one lane is visible, expand it.
     */
    async checkSingleLaneAutoExpand() {
        // Wait for any concurrent state updates to settle
        await this.$nextTick();

        const visibleLanes = this.lanes.filter(lane => this.isLaneVisible(lane.id));

        if (visibleLanes.length === 1) {
            const lane = visibleLanes[0];
            if (lane.collapsed) {
                console.log(`[App] Auto-expanding single visible lane: ${lane.name} (${lane.id})`);
                lane.collapsed = false;

                // Load tasks if not already loaded
                if (!lane.tasksLoaded) {
                    lane.loading = true;
                    try {
                        await this.fetchLaneTasks(lane.id);
                        lane.tasksLoaded = true;
                        this.$nextTick(() => {
                            this.reinitSortableForLane(lane.id);
                        });
                    } catch (e) {
                        console.error(`[App] Failed to auto-load single lane ${lane.id}:`, e);
                    } finally {
                        lane.loading = false;
                    }
                }
            }
        }
    },

    // =========================================================================
    // TASK RESIZE FEATURE
    // =========================================================================

    // Resize state
    resizingTaskId: null,
    resizeStartX: 0,
    resizeStartY: 0,
    resizeStartHeight: 0,
    resizeLaneId: null,
    resizeStatus: null,
    taskSizes: {}, // { taskId: { height: px, expanded: bool } }
    expandedColumns: {}, // { laneId: status }

    /**
     * Get inline style for task card based on resize state
     */
    getTaskStyle(taskId) {
        const size = this.taskSizes[taskId];
        if (!size) return '';
        return `height: ${size.height}px; min-height: ${size.height}px;`;
    },

    /**
     * Start resizing a task card
     */
    startResize(event, taskId, laneId, status) {
        console.log('[App] startResize:', { taskId, laneId, status });

        const card = event.target.closest('.task-card');
        if (!card) return;

        this.resizingTaskId = taskId;
        this.resizeLaneId = laneId;
        this.resizeStatus = status;
        this.resizeStartX = event.clientX;
        this.resizeStartY = event.clientY;
        this.resizeStartHeight = card.offsetHeight;

        // Store initial height if not set
        if (!this.taskSizes[taskId]) {
            this.taskSizes[taskId] = { height: card.offsetHeight, initialHeight: card.offsetHeight };
        }

        card.classList.add('resizing');

        // Bind event handlers
        this._boundDoResize = this.doResize.bind(this);
        this._boundStopResize = this.stopResize.bind(this);

        document.addEventListener('mousemove', this._boundDoResize);
        document.addEventListener('mouseup', this._boundStopResize);
    },

    /**
     * Handle resize drag
     */
    doResize(event) {
        if (!this.resizingTaskId) return;

        const deltaY = event.clientY - this.resizeStartY;
        const deltaX = event.clientX - this.resizeStartX;

        const taskSize = this.taskSizes[this.resizingTaskId];
        const initialHeight = taskSize.initialHeight || this.resizeStartHeight;
        const maxHeight = initialHeight * 2; // 2x vertical limit

        // Calculate new height (vertical resize)
        let newHeight = this.resizeStartHeight + deltaY;
        newHeight = Math.max(initialHeight, Math.min(maxHeight, newHeight));

        this.taskSizes[this.resizingTaskId] = {
            ...taskSize,
            height: newHeight
        };

        // Horizontal: if dragging right significantly, expand the column
        if (deltaX > 50) {
            this.expandColumn(this.resizeLaneId, this.resizeStatus);
        } else if (deltaX < -50) {
            this.shrinkColumn(this.resizeLaneId, this.resizeStatus);
        }
    },

    /**
     * Stop resizing
     */
    stopResize() {
        console.log('[App] stopResize');

        if (this.resizingTaskId) {
            const card = document.querySelector(`[data-task-id="${this.resizingTaskId}"]`);
            if (card) card.classList.remove('resizing');
        }

        this.resizingTaskId = null;
        this.resizeLaneId = null;
        this.resizeStatus = null;

        document.removeEventListener('mousemove', this._boundDoResize);
        document.removeEventListener('mouseup', this._boundStopResize);
    },

    /**
     * Expand a column (shrink others in same lane)
     */
    expandColumn(laneId, status) {
        console.log('[App] expandColumn:', { laneId, status });

        const swimlaneContent = document.querySelector(`[data-lane-id="${laneId}"].swimlane-row .swimlane-content`);
        if (!swimlaneContent) return;

        swimlaneContent.classList.add('has-expanded-column');

        // Mark all columns in this lane
        const columns = swimlaneContent.querySelectorAll('.lane-column');
        columns.forEach(col => {
            const colStatus = col.getAttribute('data-status');
            if (colStatus === status) {
                col.classList.add('col-expanded');
                col.classList.remove('col-shrunk');
            } else {
                col.classList.add('col-shrunk');
                col.classList.remove('col-expanded');
            }
        });

        this.expandedColumns[laneId] = status;
    },

    /**
     * Shrink column back to normal
     */
    shrinkColumn(laneId, status) {
        console.log('[App] shrinkColumn:', { laneId, status });

        const swimlaneContent = document.querySelector(`[data-lane-id="${laneId}"].swimlane-row .swimlane-content`);
        if (!swimlaneContent) return;

        swimlaneContent.classList.remove('has-expanded-column');

        const columns = swimlaneContent.querySelectorAll('.lane-column');
        columns.forEach(col => {
            col.classList.remove('col-expanded', 'col-shrunk');
        });

        delete this.expandedColumns[laneId];
    },

    /**
     * Reset task size to default (double-click handler)
     */
    resetTaskSize(taskId) {
        console.log('[App] resetTaskSize:', taskId);

        const taskSize = this.taskSizes[taskId];
        if (taskSize && taskSize.initialHeight) {
            this.taskSizes[taskId] = {
                ...taskSize,
                height: taskSize.initialHeight
            };
        } else {
            delete this.taskSizes[taskId];
        }

        // Also reset column if this was the only expanded card
        // Find the task and its lane
        const task = this.tasks.find(t => t.id === taskId);
        if (task && task.swimLane) {
            this.shrinkColumn(task.swimLane.id, task.status);
        }
    },

    // =========================================================================
    // TASK DETAIL PANE METHODS
    // =========================================================================

    /**
     * Open task detail pane
     */
    openTaskDetail(taskId) {
        console.log('[App] openTaskDetail:', taskId);
        const task = this.tasks.find(t => t.id === taskId);
        if (task) {
            this.taskDetail.task = task;
            this.taskDetail.open = true;
            this.taskDetail.newComment = '';
        }
    },

    /**
     * Close task detail pane
     */
    closeTaskDetail() {
        console.log('[App] closeTaskDetail');
        this.taskDetail.open = false;
        this.taskDetail.selectedCommentIndex = -1;
        setTimeout(() => {
            this.taskDetail.task = null;
        }, 300); // Wait for transition
    },

    /**
     * Handle keyboard events in task detail pane
     */
    handleDetailKeydown(event) {
        console.log('[App] handleDetailKeydown:', {
            key: event.key,
            altKey: event.altKey,
            ctrlKey: event.ctrlKey,
            shiftKey: event.shiftKey,
            taskDetailOpen: this.taskDetail.open,
            selectedCommentIndex: this.taskDetail.selectedCommentIndex
        });

        if (!this.taskDetail.open) {
            console.log('[App] handleDetailKeydown: Pane not open, ignoring');
            return;
        }

        const comments = this.getTaskComments(this.taskDetail.task);
        console.log('[App] handleDetailKeydown: Comments count:', comments.length);

        // ESC: Close pane
        if (event.key === 'Escape') {
            console.log('[App] handleDetailKeydown: ESC pressed, closing pane');
            this.closeTaskDetail();
            event.preventDefault();
            return;
        }

        // Alt+Enter: Submit comment
        if (event.key === 'Enter' && event.altKey) {
            console.log('[App] handleDetailKeydown: Alt+Enter pressed');
            if (this.taskDetail.newComment.trim()) {
                console.log('[App] handleDetailKeydown: Submitting comment');
                this.addComment();
            }
            event.preventDefault();
            return;
        }

        // Arrow Up: Select previous comment (from bottom up)
        if (event.key === 'ArrowUp' && !this.taskDetail.editingCommentId) {
            console.log('[App] handleDetailKeydown: ArrowUp pressed');
            if (comments.length === 0) {
                console.log('[App] handleDetailKeydown: No comments to navigate');
                return;
            }
            if (this.taskDetail.selectedCommentIndex === -1) {
                // Start from bottom
                this.taskDetail.selectedCommentIndex = comments.length - 1;
            } else if (this.taskDetail.selectedCommentIndex > 0) {
                this.taskDetail.selectedCommentIndex--;
            }
            console.log('[App] handleDetailKeydown: New selectedCommentIndex:', this.taskDetail.selectedCommentIndex);
            this.scrollToSelectedComment();
            event.preventDefault();
            return;
        }

        // Arrow Down: Select next comment
        if (event.key === 'ArrowDown' && !this.taskDetail.editingCommentId) {
            console.log('[App] handleDetailKeydown: ArrowDown pressed');
            if (comments.length === 0) {
                console.log('[App] handleDetailKeydown: No comments to navigate');
                return;
            }
            if (this.taskDetail.selectedCommentIndex < comments.length - 1) {
                this.taskDetail.selectedCommentIndex++;
            }
            console.log('[App] handleDetailKeydown: New selectedCommentIndex:', this.taskDetail.selectedCommentIndex);
            this.scrollToSelectedComment();
            event.preventDefault();
            return;
        }

        // Enter: Edit selected comment
        if (event.key === 'Enter' && !event.altKey && !event.ctrlKey && this.taskDetail.selectedCommentIndex !== -1) {
            console.log('[App] handleDetailKeydown: Enter pressed on selected comment index:', this.taskDetail.selectedCommentIndex);
            const comment = comments[this.taskDetail.selectedCommentIndex];
            if (comment) {
                console.log('[App] handleDetailKeydown: Starting edit for comment ID:', comment.id);
                this.startEditComment(comment);
            }
            event.preventDefault();
            return;
        }
    },

    /**
     * Scroll to the selected comment for visibility
     */
    scrollToSelectedComment() {
        this.$nextTick(() => {
            const selected = document.querySelector('.comment-item.comment-selected');
            if (selected) {
                selected.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
            }
        });
    },

    /**
     * Check if comment at index is selected
     */
    isCommentSelected(index) {
        return this.taskDetail.selectedCommentIndex === index;
    },

    // =========================================================================
    // MAIN VIEW KEYBOARD NAVIGATION
    // =========================================================================

    /**
     * Global keyboard handler - routes to appropriate handler
     */
    handleGlobalKeydown(event) {
        // Ignore if user is typing in an input/textarea
        const activeEl = document.activeElement;
        if (activeEl && (activeEl.tagName === 'INPUT' || activeEl.tagName === 'TEXTAREA')) {
            return;
        }

        console.log('[App] handleGlobalKeydown:', {
            key: event.key,
            altKey: event.altKey,
            ctrlKey: event.ctrlKey,
            taskDetailOpen: this.taskDetail.open
        });

        // Route to appropriate handler
        if (this.taskDetail.open) {
            this.handleDetailKeydown(event);
        } else {
            this.handleMainViewKeydown(event);
        }
    },

    /**
     * Handle keyboard navigation in main swimlane view
     */
    handleMainViewKeydown(event) {
        console.log('[App] handleMainViewKeydown:', event.key);

        // Space: Open task detail for selected task
        if (event.key === ' ' || event.key === 'Space') {
            if (this.selectedTaskId) {
                console.log('[App] Space pressed - opening task detail:', this.selectedTaskId);
                this.openTaskDetail(this.selectedTaskId);
                event.preventDefault();
            }
            return;
        }

        // Escape: Clear selection
        if (event.key === 'Escape') {
            console.log('[App] Escape pressed - clearing task selection');
            this.selectedTaskId = null;
            return;
        }

        // Arrow navigation
        if (['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight'].includes(event.key)) {
            this.navigateTask(event.key);
            event.preventDefault();
            return;
        }
    },

    /**
     * Navigate to adjacent task based on arrow key
     */
    navigateTask(direction) {
        console.log('[App] navigateTask:', direction, 'current:', this.selectedTaskId);

        // If no task selected, select the first visible task
        if (!this.selectedTaskId) {
            const firstTask = this.getFirstVisibleTask();
            if (firstTask) {
                this.selectedTaskId = firstTask.id;
                this.scrollToSelectedTask();
            }
            return;
        }

        // Find current task and its position
        const currentTask = this.tasks.find(t => t.id === this.selectedTaskId);
        if (!currentTask) {
            this.selectedTaskId = null;
            return;
        }

        let nextTask = null;

        switch (direction) {
            case 'ArrowUp':
                nextTask = this.getTaskAbove(currentTask);
                break;
            case 'ArrowDown':
                nextTask = this.getTaskBelow(currentTask);
                break;
            case 'ArrowLeft':
                nextTask = this.getTaskInPreviousColumn(currentTask);
                break;
            case 'ArrowRight':
                nextTask = this.getTaskInNextColumn(currentTask);
                break;
        }

        if (nextTask) {
            console.log('[App] Navigating to task:', nextTask.id, nextTask.name);
            this.selectedTaskId = nextTask.id;
            this.scrollToSelectedTask();
        }
    },

    /**
     * Get first visible task in the view
     */
    getFirstVisibleTask() {
        // Get first non-collapsed lane
        const firstLane = this.lanes.find(l => !l.collapsed && this.laneHasMatchingTasks(l.id));
        if (!firstLane) return null;

        // Get first status column that has tasks
        for (const status of this.columns) {
            const tasks = this.getTasks(firstLane.id, status);
            if (tasks.length > 0) {
                return tasks[0];
            }
        }
        return null;
    },

    /**
     * Get task above current in same column
     */
    getTaskAbove(currentTask) {
        const laneId = currentTask.swimLane?.id;
        if (!laneId) return null;

        const columnTasks = this.getTasks(laneId, currentTask.status);
        const currentIndex = columnTasks.findIndex(t => t.id === currentTask.id);

        if (currentIndex > 0) {
            return columnTasks[currentIndex - 1];
        }

        // Try previous lane, same status
        return this.getTaskInPreviousLane(currentTask);
    },

    /**
     * Get task below current in same column
     */
    getTaskBelow(currentTask) {
        const laneId = currentTask.swimLane?.id;
        if (!laneId) return null;

        const columnTasks = this.getTasks(laneId, currentTask.status);
        const currentIndex = columnTasks.findIndex(t => t.id === currentTask.id);

        if (currentIndex < columnTasks.length - 1) {
            return columnTasks[currentIndex + 1];
        }

        // Try next lane, same status
        return this.getTaskInNextLane(currentTask);
    },

    /**
     * Get task in previous column (left)
     */
    getTaskInPreviousColumn(currentTask) {
        const laneId = currentTask.swimLane?.id;
        if (!laneId) return null;

        const currentColIndex = this.columns.indexOf(currentTask.status);
        if (currentColIndex <= 0) return null;

        // Search columns to the left
        for (let i = currentColIndex - 1; i >= 0; i--) {
            const tasks = this.getTasks(laneId, this.columns[i]);
            if (tasks.length > 0) {
                return tasks[0]; // First task in that column
            }
        }
        return null;
    },

    /**
     * Get task in next column (right)
     */
    getTaskInNextColumn(currentTask) {
        const laneId = currentTask.swimLane?.id;
        if (!laneId) return null;

        const currentColIndex = this.columns.indexOf(currentTask.status);
        if (currentColIndex >= this.columns.length - 1) return null;

        // Search columns to the right
        for (let i = currentColIndex + 1; i < this.columns.length; i++) {
            const tasks = this.getTasks(laneId, this.columns[i]);
            if (tasks.length > 0) {
                return tasks[0]; // First task in that column
            }
        }
        return null;
    },

    /**
     * Get task in previous lane (same status)
     */
    getTaskInPreviousLane(currentTask) {
        const currentLaneIndex = this.lanes.findIndex(l => l.id === currentTask.swimLane?.id);
        if (currentLaneIndex <= 0) return null;

        // Search previous lanes
        for (let i = currentLaneIndex - 1; i >= 0; i--) {
            const lane = this.lanes[i];
            if (lane.collapsed || !this.laneHasMatchingTasks(lane.id)) continue;

            const tasks = this.getTasks(lane.id, currentTask.status);
            if (tasks.length > 0) {
                return tasks[tasks.length - 1]; // Last task in column (bottom)
            }
        }
        return null;
    },

    /**
     * Get task in next lane (same status)
     */
    getTaskInNextLane(currentTask) {
        const currentLaneIndex = this.lanes.findIndex(l => l.id === currentTask.swimLane?.id);
        if (currentLaneIndex >= this.lanes.length - 1) return null;

        // Search next lanes
        for (let i = currentLaneIndex + 1; i < this.lanes.length; i++) {
            const lane = this.lanes[i];
            if (lane.collapsed || !this.laneHasMatchingTasks(lane.id)) continue;

            const tasks = this.getTasks(lane.id, currentTask.status);
            if (tasks.length > 0) {
                return tasks[0]; // First task in column (top)
            }
        }
        return null;
    },

    /**
     * Scroll to make selected task visible
     */
    scrollToSelectedTask() {
        this.$nextTick(() => {
            const selected = document.querySelector('.task-card.task-selected');
            if (selected) {
                selected.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
            }
        });
    },

    /**
     * Check if task is currently selected
     */
    isTaskSelected(taskId) {
        return this.selectedTaskId === taskId;
    },

    /**
     * Get comments array from task
     * Comments are now stored as an array via JPA @OneToMany
     */
    getTaskComments(task) {
        if (!task || !task.comments) return [];
        return Array.isArray(task.comments) ? task.comments : [];
    },

    /**
     * Add a new comment to the current task
     */
    async addComment() {
        const text = this.taskDetail.newComment.trim();
        if (!text || !this.taskDetail.task) return;

        console.log('[App] addComment:', text);
        this.taskDetail.isLoading = true;

        try {
            const taskId = this.taskDetail.task.id;
            const newComment = await Api.addComment(taskId, text);

            // Comments are now an array on the task object
            const task = this.tasks.find(t => t.id === taskId);
            if (task) {
                if (!task.comments) task.comments = [];
                task.comments.push(newComment);
                this.taskDetail.newComment = '';

                // Force reactivity update
                if (this.taskDetail.task.id === task.id) {
                    this.taskDetail.task = { ...task };
                }
            }

            this.triggerSave();
        } catch (e) {
            console.error('[App] Failed to add comment:', e);
            this.showError('Failed to add comment');
        } finally {
            this.taskDetail.isLoading = false;
        }
    },
    /**
     * Start editing a comment
     */
    startEditComment(comment) {
        this.taskDetail.editingCommentId = comment.id;
        this.taskDetail.editingCommentText = comment.text || comment;
    },

    /**
     * Cancel editing
     */
    cancelEditComment() {
        this.taskDetail.editingCommentId = null;
        this.taskDetail.editingCommentText = '';
    },

    /**
     * Update an existing comment
     */
    async updateComment() {
        const text = this.taskDetail.editingCommentText.trim();
        const commentId = this.taskDetail.editingCommentId;
        if (!text || !commentId || !this.taskDetail.task) return;

        this.taskDetail.isLoading = true;
        try {
            const taskId = this.taskDetail.task.id;
            const updatedComment = await Api.updateComment(taskId, commentId, text);

            // Comments are now an array
            const task = this.tasks.find(t => t.id == taskId);
            if (task && task.comments) {
                const idx = task.comments.findIndex(c => c.id == commentId);
                if (idx !== -1) {
                    task.comments[idx] = updatedComment;
                    // Force reactivity update
                    this.taskDetail.task = { ...task };
                }
            }

            this.cancelEditComment();
            this.triggerSave();
        } catch (e) {
            console.error('[App] Failed to update comment:', e);
            this.showError('Failed to update comment');
        } finally {
            this.taskDetail.isLoading = false;
        }
    },

    /**
     * Request to delete a comment (opens confirmation modal)
     */
    deleteComment(commentId) {
        if (!this.taskDetail.task) return;
        // Use custom modal instead of native confirm()
        this.confirmAction('deleteComment', commentId);
    },

    /**
     * Format date string to locale string
     */
    formatDate(dateString) {
        if (!dateString) return '';
        if (dateString === 'N/A') return 'N/A';

        try {
            let date;
            // Handle array format [year, month, day, hour, minute, second, nano]
            if (Array.isArray(dateString)) {
                const [year, month, day, hour, minute, second] = dateString;
                date = new Date(year, month - 1, day, hour, minute, second);
            } else {
                date = new Date(dateString);
            }

            // Check for invalid date
            if (isNaN(date.getTime())) {
                return dateString;
            }

            const options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
            const datePart = date.toLocaleDateString('en-GB', options); // "Monday, 10 December 2025"

            // Calculate relative time
            const now = new Date();
            const diffMs = now - date; // Result in milliseconds
            const diffSeconds = Math.floor(diffMs / 1000);

            let relativePart = "Just now";

            // Define intervals in seconds
            const intervals = [
                { label: 'year', seconds: 31536000 },
                { label: 'month', seconds: 2592000 },
                { label: 'week', seconds: 604800 },
                { label: 'day', seconds: 86400 },
                { label: 'hour', seconds: 3600 },
                { label: 'minute', seconds: 60 }
            ];

            for (const interval of intervals) {
                const count = Math.floor(diffSeconds / interval.seconds);
                if (count >= 1) {
                    relativePart = `${count} ${interval.label}${count !== 1 ? 's' : ''} ago`;
                    break;
                }
            }

            return `${datePart} (${relativePart})`;

        } catch (e) {
            console.error('Date parse error:', e);
            return dateString;
        }
    },

    /**
     * Execute delete comment after modal confirmation
     */
    async executeDeleteComment(commentId) {
        if (!this.taskDetail.task) return;

        // Guard: Prevent duplicate delete calls
        if (this.isDeletingComment) {
            console.warn('[App] executeDeleteComment - Already deleting, ignoring');
            return;
        }
        this.isDeletingComment = true;
        this.taskDetail.isLoading = true;

        try {
            const taskId = this.taskDetail.task.id;
            await Api.deleteComment(taskId, commentId);

            // Comments are now an array
            const task = this.tasks.find(t => t.id == taskId);
            if (task && task.comments) {
                task.comments = task.comments.filter(c => c.id != commentId);
                // Force reactivity update
                this.taskDetail.task = { ...task };
            }
            this.triggerSave();
        } catch (e) {
            console.error('[App] Failed to delete comment:', e);
            this.showError('Failed to delete comment');
        } finally {
            this.isDeletingComment = false;
            this.taskDetail.isLoading = false;
            this.closeModal();
        }
    },

    // =========================================================================
    // TASK TAG MANAGEMENT
    // =========================================================================

    /**
     * Add a tag to the currently open task
     */
    async addTaskTag() {
        const tag = this.taskDetail.tagInput.trim();
        if (!tag || !this.taskDetail.task) return;

        console.log('[App] addTaskTag:', tag);
        const task = this.taskDetail.task;

        // Parse existing tags
        let currentTags = this.parseTags(task.tags); // Helper in app.js

        // Deduplicate (case-insensitive check)
        if (currentTags.some(t => t.toLowerCase() === tag.toLowerCase())) {
            this.taskDetail.tagInput = ''; // Clear input even if duplicate
            return;
        }

        // Add new tag
        currentTags.push(tag);

        // Optimistic update
        this.taskDetail.isLoading = true;
        const tagsJson = JSON.stringify(currentTags);

        // Prepare payload (Full task object to be safe, but overriding tags)
        const payload = {
            ...task,
            tags: tagsJson,
            // Ensure swimLane object has ID if it exists
            swimLane: task.swimLane ? { id: task.swimLane.id } : null
        };

        try {
            await this.updateTask(task.id, payload);
            this.taskDetail.tagInput = '';

            // Local state update is handled by updateTask's optimistic logic + SSE
            // But we ensure taskDetail reflects it immediately
            this.taskDetail.task.tags = tagsJson;

        } catch (e) {
            console.error('[App] Failed to add tag:', e);
            // Revert is complex here, but updateTask handles its own error toast
        } finally {
            this.taskDetail.isLoading = false;
        }
    },

    /**
     * Remove a tag from the currently open task
     */
    async removeTaskTag(tagToRemove) {
        if (!this.taskDetail.task) return;

        console.log('[App] removeTaskTag:', tagToRemove);
        const task = this.taskDetail.task;

        // Filter out tag
        let currentTags = this.parseTags(task.tags);
        const newTags = currentTags.filter(t => t !== tagToRemove);

        if (newTags.length === currentTags.length) return; // No change

        // Optimistic Update
        this.taskDetail.isLoading = true;
        const tagsJson = JSON.stringify(newTags);

        const payload = {
            ...task,
            tags: tagsJson,
            swimLane: task.swimLane ? { id: task.swimLane.id } : null
        };

        try {
            await this.updateTask(task.id, payload);
            this.taskDetail.task.tags = tagsJson;
        } catch (e) {
            console.error('[App] Failed to remove tag:', e);
        } finally {
            this.taskDetail.isLoading = false;
        }
    }
}));

// =============================================================================
// START ALPINE
// =============================================================================
window.Alpine = Alpine; // Expose globally for debugging in console
Alpine.start();
console.log('[App] ====== Alpine Started ======');
