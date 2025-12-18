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
    selectedTags: [], // Array of selected tags for multi-tag filtering

    // Loading state for skeleton loaders
    isLoading: true,  // Shows skeletons until tasks load

    // Mobile sidebar navigation
    mobileSidebarOpen: false,  // Toggle sidebar drawer
    activeLaneId: null,        // Currently selected lane on mobile (null = show all)
    isMobile: false,           // Track if we're on mobile viewport

    // Task keyboard navigation (main view)
    selectedTaskId: null,      // Currently keyboard-selected task in main view

    // Task Detail Pane State (Desktop Only)
    taskDetail: {
        open: false,
        task: null,
        newComment: '',
        isLoading: false,
        editingCommentId: null,
        editingCommentText: '',
        selectedCommentIndex: -1  // For arrow key navigation
    },

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

                    // Expand lanes that have tasks
                    this.lanes.forEach(lane => {
                        const laneTasks = this.tasks.filter(t => t.swimLane && t.swimLane.id === lane.id);
                        lane.loading = false;
                        lane.collapsed = laneTasks.length === 0;
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

        // ---------------------------------------------------------------------
        // STEP 6: Watchers
        // ---------------------------------------------------------------------
        this.$watch('selectedTags', (value) => {
            const isFilterActive = value.length > 0;
            console.log('[App] Tag filter state changed. isFilterActive:', isFilterActive);

            // Update all task Sortable instances
            document.querySelectorAll('.lane-column').forEach(col => {
                if (col.sortableInstance) {
                    col.sortableInstance.option('disabled', isFilterActive);
                }
            });

            // Update lane reorder Sortable instance
            const boardContainer = document.querySelector('.board-container');
            if (boardContainer && boardContainer.sortableInstance) {
                boardContainer.sortableInstance.option('disabled', isFilterActive);
            }
        });

        // ---------------------------------------------------------------------
        // STEP 7: Global Keyboard Navigation
        // ---------------------------------------------------------------------
        document.addEventListener('keydown', (e) => this.handleGlobalKeydown(e));
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

        console.log('[App] checkMobile:', { isMobile: this.isMobile, width: window.innerWidth });
    },

    /**
     * Toggle mobile sidebar open/close
     */
    toggleMobileSidebar() {
        this.mobileSidebarOpen = !this.mobileSidebarOpen;
        console.log('[App] toggleMobileSidebar:', this.mobileSidebarOpen);
    },

    /**
     * Select a swimlane on mobile (closes sidebar automatically)
     */
    selectLane(laneId) {
        console.log('[App] selectLane:', laneId);
        this.activeLaneId = laneId;
        this.mobileSidebarOpen = false; // Auto-close sidebar

        // Scroll to top of content
        window.scrollTo({ top: 0, behavior: 'smooth' });
    },

    /**
     * Check if a lane should be visible
     * - On desktop: always check tag matching
     * - On mobile with tag filters: show ALL lanes with matching tasks
     * - On mobile without tags: show only active lane
     */
    isLaneVisible(laneId) {
        // On desktop, show all lanes (filtered by tags)
        if (!this.isMobile) {
            return this.laneHasMatchingTasks(laneId);
        }

        // On mobile WITH tag filters active: show ALL lanes that have matching tasks
        if (this.selectedTags.length > 0) {
            return this.laneHasMatchingTasks(laneId);
        }

        // On mobile WITHOUT tag filters: show only active lane
        return laneId === this.activeLaneId;
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
        const data = await Store.loadData();
        this.lanes = data.lanes;
        console.log(`[App] Loaded ${this.lanes.length} lanes via API`);

        // Load tasks per lane
        const lanePromises = this.lanes.map(async (lane) => {
            try {
                const newTasks = await Store.fetchLaneTasks(lane.id);
                this.tasks.push(...newTasks);

                const localLane = this.lanes.find(l => l.id === lane.id);
                if (localLane) {
                    localLane.loading = false;
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
        console.log('[App] All lanes loaded via API');
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
        // Apply filters
        if (this.hideDone && status === 'DONE') {
            return []; // Hide entire DONE column when filter active
        }
        if (this.showOnlyBlocked && status !== 'BLOCKED') {
            return []; // Show only BLOCKED column when filter active
        }

        let tasks = this.tasks.filter(t =>
            t.swimLane && t.swimLane.id === laneId && t.status === status
        );

        // Apply multi-tag filter (show tasks with ALL of the selected tags)
        if (this.selectedTags && this.selectedTags.length > 0) {
            tasks = tasks.filter(t => {
                const taskTags = this.getTags(t.tags).map(tag => tag.toLowerCase());
                // Task must have ALL selected tags (AND logic)
                return this.selectedTags.every(selectedTag =>
                    taskTags.includes(selectedTag.toLowerCase())
                );
            });
        }

        // Sort by position
        tasks.sort((a, b) => {
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
    },

    /**
     * Check if a lane has any tasks matching the current tag filter
     * Used to hide/show lanes when tag filter is active
     * 
     * @param {number} laneId - The swimlane ID
     * @returns {boolean} True if lane has matching tasks or no filter active
     */
    laneHasMatchingTasks(laneId) {
        // If no tags selected, always show the lane
        if (!this.selectedTags || this.selectedTags.length === 0) {
            return true;
        }

        // Get all tasks in this lane
        const laneTasks = this.tasks.filter(t => t.swimLane && t.swimLane.id === laneId);

        // Check if any task has ALL the selected tags
        return laneTasks.some(task => {
            const taskTags = this.getTags(task.tags).map(tag => tag.toLowerCase());
            return this.selectedTags.every(selectedTag =>
                taskTags.includes(selectedTag.toLowerCase())
            );
        });
    },

    /**
     * Check if a specific status column in a lane has any tasks matching the current tag filter
     */
    columnHasMatchingTasks(laneId, status) {
        if (!this.selectedTags || this.selectedTags.length === 0) {
            return true;
        }
        return this.getTasks(laneId, status).length > 0;
    },

    /**
     * Complete visibility logic for a status column
     * Used in index.html to hide empty columns in mobile filter view
     */
    isColumnVisible(laneId, status) {
        // Always show on desktop
        if (!this.isMobile) return true;

        // On mobile, if no tags selected, show all (active) columns
        if (!this.selectedTags || this.selectedTags.length === 0) return true;

        // On mobile WITH filters, check if column has matching tasks
        const visible = this.columnHasMatchingTasks(laneId, status);

        if (!visible) {
            console.log(`[App] Hiding empty column ${status} for lane ${laneId} in mobile filter view`);
        }

        return visible;
    },

    /**
     * Get all unique tags from all tasks
     * Used to render clickable tag capsules
     * 
     * @returns {Array} Sorted array of unique tag strings
     */
    getAllUniqueTags() {
        const tagSet = new Set();
        this.tasks.forEach(task => {
            const tags = this.getTags(task.tags);
            tags.forEach(tag => tagSet.add(tag));
        });
        return Array.from(tagSet).sort((a, b) => a.toLowerCase().localeCompare(b.toLowerCase()));
    },

    /**
     * Toggle a tag in the selectedTags array
     * If tag is selected, remove it; otherwise add it
     * 
     * @param {string} tag - Tag to toggle
     */
    toggleTag(tag) {
        const index = this.selectedTags.indexOf(tag);
        if (index > -1) {
            // Remove tag using filter (immutable, more reliably reactive)
            this.selectedTags = this.selectedTags.filter(t => t !== tag);
        } else {
            // Add tag using spread (immutable)
            this.selectedTags = [...this.selectedTags, tag];
        }
        console.log('[App] selectedTags:', this.selectedTags);
    },

    /**
     * Check if a tag is currently selected
     * 
     * @param {string} tag - Tag to check
     * @returns {boolean} True if tag is in selectedTags
     */
    isTagSelected(tag) {
        return this.selectedTags.includes(tag);
    },

    /**
     * Clear all selected tags
     */
    clearSelectedTags() {
        this.selectedTags = [];
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
        if (this.isMobile) return; // Desktop only for now

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
     * Execute delete comment after modal confirmation
     */
    async executeDeleteComment(commentId) {
        if (!this.taskDetail.task) return;

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
