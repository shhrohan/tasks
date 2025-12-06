/*
 * drag.js
 * Encapsulates SortableJS logic.
 * Handles the "Flash Reset" to keep DOM/Alpine in sync.
 */

export const Drag = {

    initOneColumn(col, scope) {
        // Idempotency: If already initialized, skip (or destroy/recreate if you prefer)
        if (col.sortableInstance) {
            console.log(`[Drag] SKIP Init: Lane ${col.getAttribute('data-lane-id')} Status ${col.getAttribute('data-status')} (Already has instance)`);
            return;
        }

        console.log(`[Drag] Lifecycle Init: Lane ${col.getAttribute('data-lane-id')} Status ${col.getAttribute('data-status')} | DOM Element:`, col);

        col.sortableInstance = new Sortable(col, {
            group: 'tasks',
            animation: 150,
            delay: 0, // Instant drag
            delayOnTouchOnly: true,
            touchStartThreshold: 3,
            ghostClass: 'task-ghost',
            dragClass: 'task-drag',
            // forceFallback: false, // Default

            onStart: (evt) => {
                console.log('[Drag] Started', evt);
            },

            onEnd: (evt) => {
                const item = evt.item;
                const to = evt.to;
                const from = evt.from;

                console.log('[Drag] Ended', { item, from, to, sameContainer: to === from });

                if (to === from && evt.newIndex === evt.oldIndex) return;

                const taskId = item.getAttribute('data-task-id');
                const newStatus = to.getAttribute('data-status');
                const newLaneId = to.getAttribute('data-lane-id');

                // Log the state of the 'to' column to see if it survives
                console.log(`[Drag] Task ${taskId} -> ${newStatus} (Lane ${newLaneId})`);

                scope.moveTaskOptimistic(taskId, newStatus, newLaneId);

                // Debug Check after small delay to see if instance survives
                setTimeout(() => {
                    if (to.sortableInstance) {
                        console.log(`[Drag] Check: Sortable instance survived on destination column ${newStatus}`);
                    } else {
                        console.error(`[Drag] Check: Sortable instance LOST on destination column ${newStatus} (Alpine re-render likely destroyed it)`);
                        // Attempt re-init if lost (failsafe)
                        // Drag.initOneColumn(to, scope);
                    }
                }, 500);
            }
        });
    },

    initLaneSortable(containerElement, store, refreshCallback) {
        if (!containerElement) return;
        console.log('[Drag] Initializing Lane Sortable...');

        if (containerElement.sortableInstance) containerElement.sortableInstance.destroy();

        containerElement.sortableInstance = new Sortable(containerElement, {
            handle: '.swimlane-header', // Only drag by header
            animation: 150,
            ghostClass: 'lane-ghost',

            onEnd: (evt) => {
                if (evt.newIndex === evt.oldIndex) return;

                console.log('[Drag] Lane reorder detected');

                // 1. Capture new order from DOM
                const laneElements = Array.from(containerElement.children);
                const newIds = laneElements.map(el => el.getAttribute('data-lane-id')).filter(id => id); // Filter nulls

                // 2. Update Store
                store.reorderLanesOptimistic(newIds);

                // 3. NUCLEAR FLASH RESET (Essential for Alpine Sync)
                // We must force Alpine to re-render the list to match its internal state
                // otherwise future drags fail because DOM != VirtualDOM
                // However, we wait for the store to update first.
                // In this simplified version, let's try relying on the Store's refresh.
                // If regression happens, we re-add the "activeLanes = []" hack here.

                if (refreshCallback) refreshCallback();
            }
        });
    }
};
