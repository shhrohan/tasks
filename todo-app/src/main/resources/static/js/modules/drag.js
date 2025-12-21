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
     * @param {Object} Alpine - The Alpine global object (for mutateDom)
     * 
     * IMPORTANT: This must be called AFTER task cards exist in the DOM!
     */
    initOneColumn(col, scope, Alpine) {
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
                // Use lane-specific group to prevent cross-swimlane dragging
                // Tasks can only move between columns within the SAME lane
                group: `tasks-lane-${laneId}`,
                animation: 150,           // Smooth animation duration (ms)
                delay: 0,                 // No delay for desktop
                delayOnTouchOnly: true,   // Delay only on touch devices (prevents scroll conflict)
                touchStartThreshold: 3,   // Pixels before drag starts on touch
                ghostClass: 'task-ghost', // CSS class for ghost element
                dragClass: 'task-drag',   // CSS class while dragging

                // CRITICAL: These options prevent Alpine.js errors during mobile/touch drag
                // When elements are cloned, they lose Alpine.js context causing "task is not defined" errors
                removeCloneOnHide: true,  // Remove clone immediately when hidden (prevents re-evaluation)
                forceFallback: true,      // Use forceFallback to ensure a ghost element is created

                /**
                 * onChoose: Fired when user clicks/touches an item to start drag
                 */
                // onChoose: Fired when user clicks/touches an item to start drag
                onChoose(evt) {
                    const item = evt.item;
                    if (!item) return;

                    // Log for debugging
                    console.log(
                        '[Drag] onChoose - item:',
                        item.getAttribute('data-task-id'),
                        'tagName:',
                        item.tagName
                    );

                    // IMPORTANT: prevent Alpine from re‑initializing this element or its clone.
                    // Alpine will skip any node that has x-ignore.
                    item.setAttribute('x-ignore', '');

                    // Extra safety: if inner .task-card exists, mark it too.
                    const innerCard = item.matches('.task-card')
                        ? item
                        : item.querySelector('.task-card');

                    if (innerCard && innerCard !== item) {
                        innerCard.setAttribute('x-ignore', '');
                        console.log(
                            '[Drag] onChoose - Added x-ignore to inner .task-card',
                            innerCard.getAttribute('data-task-id')
                        );
                    }

                    console.log(
                        '[Drag] onChoose - x-ignore applied',
                        item.getAttribute('data-task-id')
                    );
                },

                /**
                 * onUnchoose: Fired when the drag operation is completed or canceled
                 * Ensure we clean up x-ignore just in case mouseup/touchend missed it
                 */
                // onUnchoose: Fired when drag completes or is cancelled
                onUnchoose(evt) {
                    const item = evt.item;
                    if (!item) return;

                    // Remove x-ignore so Alpine becomes reactive again.
                    item.removeAttribute('x-ignore');

                    // Also remove from inner .task-card if we added it.
                    const innerCard = item.matches('.task-card')
                        ? item
                        : item.querySelector('.task-card');

                    if (innerCard && innerCard !== item) {
                        innerCard.removeAttribute('x-ignore');
                    }

                    console.log(
                        '[Drag] onUnchoose - Removed x-ignore from item',
                        item.getAttribute('data-task-id')
                    );
                },

                /**
                 * onStart: Fired when drag actually begins (after threshold)
                 * We add visual feedback by dimming other swimlanes
                 */
                onStart: (evt) => {
                    const currentLaneId = evt.from.getAttribute('data-lane-id');
                    console.log('[Drag] onStart - Drag began:', {
                        taskId: evt.item.getAttribute('data-task-id'),
                        fromColumn: evt.from.getAttribute('data-status'),
                        fromLane: currentLaneId
                    });

                    // Add visual feedback: highlight current lane, dim others
                    document.querySelectorAll('.swimlane-row').forEach(row => {
                        const rowLaneId = row.querySelector('.lane-column')?.getAttribute('data-lane-id');
                        if (rowLaneId === currentLaneId) {
                            row.classList.add('drag-active-lane');
                        } else {
                            row.classList.add('drag-disabled-lane');
                        }
                    });
                },

                // NOTE: onClone is NOT used because it doesn't fire reliably for fallback ghosts 
                // in a way that lets us modify the clone before Alpine sees it. 
                // The onChoose x-ignore inheritance strategy is more robust.


                /**
                 * onMove: Fired continuously while dragging
                 * Return false to BLOCK the move to a different swimlane
                 */
                onMove: (evt) => {
                    const fromLaneId = evt.from?.getAttribute('data-lane-id');
                    const toLaneId = evt.to?.getAttribute('data-lane-id');

                    // Block moves between different swimlanes
                    if (fromLaneId !== toLaneId) {
                        console.log('[Drag] onMove - BLOCKED: Cannot move between swimlanes', {
                            fromLane: fromLaneId,
                            toLane: toLaneId
                        });
                        return false; // Block the move
                    }

                    console.log('[Drag] onMove - Allowed:', {
                        targetColumn: evt.to?.getAttribute('data-status'),
                        targetLane: toLaneId
                    });
                    return true; // Allow move within same lane
                },

                /**
                 * onEnd: Fired when drag ends (drop or cancel)
                 * This is where we update the backend
                 */
                onEnd: (evt) => {
                    // Clear visual feedback classes from all swimlanes
                    document.querySelectorAll('.swimlane-row').forEach(row => {
                        row.classList.remove('drag-active-lane', 'drag-disabled-lane');
                    });

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

        console.log('[Drag] Initializing Lane Sortable on .board-container');

        // Destroy existing instance to prevent duplicates
        if (containerElement.sortableInstance) {
            console.log('[Drag] Destroying existing lane Sortable instance');
            containerElement.sortableInstance.destroy();
        }

        containerElement.sortableInstance = new Sortable(containerElement, {
            handle: '.swimlane-header', // Only drag lanes by their header
            animation: 150,
            ghostClass: 'lane-ghost',

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
