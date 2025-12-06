/*
 * drag.js
 * Encapsulates SortableJS logic.
 * Handles the "Flash Reset" to keep DOM/Alpine in sync.
 */

export const Drag = {

    initTaskSortables(store) {
        console.log('[Drag] Initializing Task Sortables...');
        const columns = document.querySelectorAll('.lane-column');

        columns.forEach(col => {
            // Idempotency check
            if (col.sortableInstance) col.sortableInstance.destroy();

            col.sortableInstance = new Sortable(col, {
                group: 'tasks',
                animation: 150,
                delay: 100, // Slight delay prevents accidental drags on touch
                delayOnTouchOnly: true,
                ghostClass: 'task-ghost',
                dragClass: 'task-drag',

                onEnd: (evt) => {
                    const item = evt.item;
                    const to = evt.to;
                    const from = evt.from;

                    // If dropped in same place, ignore
                    if (to === from && evt.newIndex === evt.oldIndex) return;

                    const taskId = item.getAttribute('data-task-id');
                    const newStatus = to.getAttribute('data-status');
                    const newLaneId = to.getAttribute('data-lane-id');

                    console.log(`[Drag] Task ${taskId} -> ${newStatus} (Lane ${newLaneId})`);

                    // Action
                    store.moveTaskOptimistic(taskId, newStatus, newLaneId);
                }
            });
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
