/*
 * drag.js
 * Encapsulates SortableJS logic.
 * Handles the "Flash Reset" to keep DOM/Alpine in sync.
 */

export const Drag = {

    initOneColumn(col, scope) {
        // Idempotency: If already initialized, skip
        if (col.sortableInstance) {
            console.log(`[Drag] SKIP Init: Lane ${col.getAttribute('data-lane-id')} Status ${col.getAttribute('data-status')} (Already has instance)`);
            return;
        }

        // DIAGNOSTIC: Check what we're initializing
        const children = col.querySelectorAll('.task-card');
        console.log(`[Drag] Lifecycle Init: Lane ${col.getAttribute('data-lane-id')} Status ${col.getAttribute('data-status')}`);
        console.log(`[Drag] Column has ${children.length} task-card children at init time`);
        console.log(`[Drag] Column element:`, col);
        console.log(`[Drag] Column ID:`, col.id);
        console.log(`[Drag] Scope (this):`, scope);

        // DIAGNOSTIC: Check if Sortable is available
        if (typeof Sortable === 'undefined') {
            console.error('[Drag] ERROR: Sortable is not defined!');
            return;
        }
        console.log('[Drag] Sortable is available:', typeof Sortable);

        try {
            col.sortableInstance = new Sortable(col, {
                group: 'tasks',
                animation: 150,
                delay: 0,
                delayOnTouchOnly: true,
                touchStartThreshold: 3,
                ghostClass: 'task-ghost',
                dragClass: 'task-drag',
                // forceFallback: false, // Default - let browser decide

                onChoose: (evt) => {
                    console.log('[Drag] onChoose - item selected:', evt.item);
                },

                onStart: (evt) => {
                    console.log('[Drag] onStart - drag began:', evt.item, evt);
                },

                onMove: (evt) => {
                    console.log('[Drag] onMove - dragging:', evt.dragged);
                    return true; // Allow move
                },

                onEnd: (evt) => {
                    const item = evt.item;
                    const to = evt.to;
                    const from = evt.from;
                    const newIndex = evt.newIndex;

                    console.log('[Drag] onEnd', { item, from, to, sameContainer: to === from, oldIndex: evt.oldIndex, newIndex });

                    if (to === from && evt.newIndex === evt.oldIndex) {
                        console.log('[Drag] No change detected, skipping');
                        return;
                    }

                    const taskId = item.getAttribute('data-task-id');
                    const newStatus = to.getAttribute('data-status');
                    const newLaneId = to.getAttribute('data-lane-id');

                    console.log(`[Drag] Task ${taskId} -> ${newStatus} (Lane ${newLaneId})`);

                    scope.moveTaskOptimistic(taskId, newStatus, newLaneId, newIndex);
                }
            });

            console.log('[Drag] Sortable instance created successfully:', col.sortableInstance);

            // DIAGNOSTIC: Add direct mouse listeners to the column to see if events reach it
            col.addEventListener('mousedown', (e) => {
                const card = e.target.closest('.task-card');
                console.log('[Drag] DIRECT mousedown on column', {
                    target: e.target.tagName,
                    card: card ? card.getAttribute('data-task-id') : 'none',
                    column: col.getAttribute('data-status')
                });
            }, true);

        } catch (err) {
            console.error('[Drag] ERROR creating Sortable:', err);
        }
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

                if (refreshCallback) refreshCallback();
            }
        });
    }
};
