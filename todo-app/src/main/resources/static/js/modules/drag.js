/*
 * drag.js
 * Encapsulates SortableJS logic.
 * Handles the "Flash Reset" to keep DOM/Alpine in sync.
 */

export const Drag = {

    initOneColumn(col, scope) {
        if (col.sortableInstance) return;

        col.sortableInstance = new Sortable(col, {
            group: 'tasks',
            animation: 150,
            delay: 0,
            delayOnTouchOnly: true,
            touchStartThreshold: 3,
            ghostClass: 'task-ghost',
            dragClass: 'task-drag',
            forceFallback: false, // Revert to Native Drag (CSS fix should handle hit-test)

            onChoose: (evt) => {
                console.log('[Drag] onChoose (Sortable Selection)', evt.item);
            },

            onStart: (evt) => {
                console.log('[Drag] onStart (Drag Started)', evt.item);
            },


            onEnd: (evt) => {
                try {
                    const item = evt.item;
                    const to = evt.to;
                    const from = evt.from;
                    const newIndex = evt.newIndex;

                    if (!item || !to || (to === from && newIndex === evt.oldIndex)) return;

                    const taskId = item.getAttribute('data-task-id');
                    const newStatus = to.getAttribute('data-status');
                    const newLaneId = to.getAttribute('data-lane-id');

                    if (!taskId || !newStatus || !newLaneId) {
                        console.warn('[Drag] Missing attributes:', { taskId, newStatus, newLaneId });
                        return;
                    }

                    scope.moveTaskOptimistic(taskId, newStatus, newLaneId, newIndex);
                } catch (err) {
                    console.error('[Drag] Error in onEnd:', err);
                }
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

                if (refreshCallback) refreshCallback();
            }
        });
    }
};
