/**
 * ============================================================================
 * DRAG.JS - SortableJS Integration Module
 * ============================================================================
 * 
 * Purpose: Encapsulates all SortableJS drag-and-drop logic for task cards and
 *          swimlane reordering.
 * 
 * CRITICAL NOTES FOR FUTURE DEVELOPERS:
 * -------------------------------------
 * 1. TIMING: Sortable MUST be initialized AFTER task cards are rendered in DOM.
 *    If initialized on empty columns, drag events won't fire on later-added cards.
 *    Solution: Use reinitSortableForLane() after async task loading.
 * 
 * 2. CSS 3D TRANSFORMS BREAK DRAG EVENTS:
 *    - Do NOT use `perspective` on body
 *    - Do NOT use `transform-style: preserve-3d` on parent containers
 *    - Do NOT use `translateZ()` on draggable elements or their parents
 *    These create stacking context issues that prevent Sortable from receiving
 *    mouse events correctly.
 * 
 * 3. ALPINE.JS INTEGRATION:
 *    - x-init fires when element is created, NOT when children are rendered
 *    - Must pass correct 'scope' (Alpine component instance) to access methods
 *    - Use $nextTick after data changes to reinitialize Sortable
 * 
 * 4. DEBUGGING CHECKLIST:
 *    - Check: "[Drag] Column has X task-card children" - should be > 0
 *    - Check: "[Drag] onChoose" appears when clicking a card
 *    - If no onChoose: CSS or stacking context issue
 *    - If no onStart: Check forceFallback setting and draggable attribute
 * 
 * ============================================================================
 */

export const Drag = {

    /**
     * Initialize Sortable on a single column (status column within a swimlane)
     * 
     * @param {HTMLElement} col - The .lane-column element to initialize
     * @param {Object} scope - The Alpine component instance (provides moveTaskOptimistic)
     * 
     * IMPORTANT: This must be called AFTER task cards exist in the DOM!
     */
    initOneColumn(col, scope) {
        // =====================================================================
        // IDEMPOTENCY CHECK
        // Prevents duplicate Sortable instances which cause unpredictable behavior
        // =====================================================================
        if (col.sortableInstance) {
            console.log(`[Drag] SKIP Init: Lane ${col.getAttribute('data-lane-id')} Status ${col.getAttribute('data-status')} (Already has instance)`);
            return;
        }

        // =====================================================================
        // DIAGNOSTIC LOGGING - Essential for debugging drag issues
        // =====================================================================
        const laneId = col.getAttribute('data-lane-id');
        const status = col.getAttribute('data-status');
        const children = col.querySelectorAll('.task-card');

        console.log(`[Drag] ====== Initializing Column ======`);
        console.log(`[Drag] Lane: ${laneId}, Status: ${status}`);
        console.log(`[Drag] Column ID: ${col.id}`);
        console.log(`[Drag] Task cards found: ${children.length}`);

        // CRITICAL: If 0 cards, log warning - Sortable won't work properly
        if (children.length === 0) {
            console.warn(`[Drag] WARNING: Column has 0 task-cards! Sortable drag may not work until reinitSortableForLane() is called after tasks load.`);
        }

        // =====================================================================
        // SORTABLE AVAILABILITY CHECK
        // =====================================================================
        if (typeof Sortable === 'undefined') {
            console.error('[Drag] FATAL ERROR: Sortable library is not loaded!');
            console.error('[Drag] Ensure <script src="Sortable.min.js"></script> is included before app.js');
            return;
        }

        // =====================================================================
        // CREATE SORTABLE INSTANCE
        // =====================================================================
        try {
            col.sortableInstance = new Sortable(col, {
                group: 'tasks',           // All columns share same group = cross-column drag
                animation: 150,           // Smooth animation duration (ms)
                delay: 0,                 // No delay for desktop
                delayOnTouchOnly: true,   // Delay only on touch devices (prevents scroll conflict)
                touchStartThreshold: 3,   // Pixels before drag starts on touch
                ghostClass: 'task-ghost', // CSS class for ghost element
                dragClass: 'task-drag',   // CSS class while dragging

                // CRITICAL: These options prevent Alpine.js errors during mobile/touch drag
                // When elements are cloned, they lose Alpine.js context causing "task is not defined" errors
                removeCloneOnHide: true,  // Remove clone immediately when hidden (prevents re-evaluation)
                forceFallback: false,     // Use native HTML5 drag when possible (avoids cloning issues)

                /**
                 * onChoose: Fired when user clicks/touches an item to start drag
                 * If this doesn't fire, there's a CSS hit-testing issue (likely 3D transforms)
                 */
                onChoose: (evt) => {
                    console.log('[Drag] onChoose - User selected item:', {
                        taskId: evt.item.getAttribute('data-task-id'),
                        element: evt.item
                    });
                },

                /**
                 * onStart: Fired when drag actually begins (after threshold)
                 */
                onStart: (evt) => {
                    console.log('[Drag] onStart - Drag began:', {
                        taskId: evt.item.getAttribute('data-task-id'),
                        fromColumn: evt.from.getAttribute('data-status'),
                        fromLane: evt.from.getAttribute('data-lane-id')
                    });
                },

                /**
                 * onClone: Fired when element is cloned (for ghost/fallback)
                 * CRITICAL: Replace clone's inner content to prevent Alpine errors.
                 * The clone is created outside the x-for loop context, so `task` 
                 * variable is not defined. We replace the content with static HTML.
                 */
                onClone: (evt) => {
                    const clone = evt.clone;
                    if (clone) {
                        // Get task name from the original element before replacing content
                        const taskName = clone.querySelector('.card-title')?.textContent || 'Task';
                        const taskId = clone.getAttribute('data-task-id');

                        // Add x-ignore to tell Alpine to skip this element entirely
                        clone.setAttribute('x-ignore', '');

                        // Replace innerHTML with static content (no Alpine bindings)
                        clone.innerHTML = `
                            <div class="card-body p-2">
                                <p class="h6 card-title text-white mb-1">${taskName}</p>
                            </div>
                        `;

                        console.log('[Drag] onClone - Created static ghost for task:', taskId);
                    }
                },

                /**
                 * onMove: Fired continuously while dragging
                 * Return false to cancel the move to current position
                 */
                onMove: (evt) => {
                    console.log('[Drag] onMove - Dragging over:', {
                        targetColumn: evt.to?.getAttribute('data-status'),
                        targetLane: evt.to?.getAttribute('data-lane-id')
                    });
                    return true; // Allow move
                },

                /**
                 * onEnd: Fired when drag ends (drop or cancel)
                 * This is where we update the backend
                 */
                onEnd: (evt) => {
                    const item = evt.item;
                    const to = evt.to;
                    const from = evt.from;
                    const newIndex = evt.newIndex;
                    const oldIndex = evt.oldIndex;

                    console.log('[Drag] onEnd - Drag completed:', {
                        taskId: item.getAttribute('data-task-id'),
                        fromColumn: from.getAttribute('data-status'),
                        toColumn: to.getAttribute('data-status'),
                        fromLane: from.getAttribute('data-lane-id'),
                        toLane: to.getAttribute('data-lane-id'),
                        oldIndex,
                        newIndex,
                        sameContainer: to === from
                    });

                    // Skip if no actual change
                    if (to === from && newIndex === oldIndex) {
                        console.log('[Drag] No position change, skipping API call');
                        return;
                    }

                    // Extract data for API call
                    const taskId = item.getAttribute('data-task-id');
                    const newStatus = to.getAttribute('data-status');
                    const newLaneId = to.getAttribute('data-lane-id');

                    console.log(`[Drag] Moving task ${taskId} to ${newStatus} in lane ${newLaneId} at index ${newIndex}`);

                    // Call Alpine component method to update state and API
                    scope.moveTaskOptimistic(taskId, newStatus, newLaneId, newIndex);
                }
            });

            console.log('[Drag] Sortable instance created successfully');
            console.log('[Drag] ====================================');

        } catch (err) {
            console.error('[Drag] ERROR creating Sortable instance:', err);
            console.error('[Drag] Stack trace:', err.stack);
        }
    },

    /**
     * Initialize Sortable on the swimlane container for reordering entire lanes
     * 
     * @param {HTMLElement} containerElement - The .board-container element
     * @param {Object} store - The Store module (provides reorderLanesOptimistic)
     * @param {Function} refreshCallback - Optional callback after reorder
     */
    initLaneSortable(containerElement, store, refreshCallback) {
        if (!containerElement) {
            console.error('[Drag] ERROR: No container element provided for lane sorting');
            return;
        }

        const isMobile = window.innerWidth < 992;
        console.log(`[Drag] Initializing Lane Sortable on .board-container (Mobile: ${isMobile})`);

        // Destroy existing instance to prevent duplicates
        if (containerElement.sortableInstance) {
            console.log('[Drag] Destroying existing lane Sortable instance');
            containerElement.sortableInstance.destroy();
        }

        containerElement.sortableInstance = new Sortable(containerElement, {
            handle: '.swimlane-header', // Only drag lanes by their header
            animation: 150,
            ghostClass: 'lane-ghost',
            disabled: isMobile, // Disable dragging on mobile initially

            onStart: (evt) => {
                console.log('[Drag] Lane drag started:', {
                    laneId: evt.item.getAttribute('data-lane-id')
                });
            },

            onEnd: (evt) => {
                if (evt.newIndex === evt.oldIndex) {
                    console.log('[Drag] Lane position unchanged, skipping');
                    return;
                }

                console.log('[Drag] Lane reorder detected:', {
                    oldIndex: evt.oldIndex,
                    newIndex: evt.newIndex
                });

                // Capture new order from DOM
                const laneElements = Array.from(containerElement.children);
                const newIds = laneElements
                    .map(el => el.getAttribute('data-lane-id'))
                    .filter(id => id); // Filter nulls

                console.log('[Drag] New lane order:', newIds);

                // Update store/API
                store.reorderLanesOptimistic(newIds);

                if (refreshCallback) refreshCallback();
            }
        });

        console.log('[Drag] Lane Sortable initialized successfully');
    }
};
