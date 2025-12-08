/**
 * ============================================================================
 * API.JS - Data Access Layer for Todo App
 * ============================================================================
 * 
 * Purpose: Handles all HTTP requests (Axios) and SSE connections.
 * 
 * LOGGING REQUIREMENTS:
 * ---------------------
 * Every API call MUST log:
 * 1. Method and URL
 * 2. ALL parameters being sent
 * 3. Response status
 * 4. Response data (or error)
 * 
 * This enables debugging without needing to check Network tab.
 * 
 * ============================================================================
 */

const TASKS_URL = '/api/tasks';
const SWIMLANES_URL = '/api/swimlanes';
const SSE_URL = '/api/sse/stream';

// ===========================================================================
// AXIOS INTERCEPTORS - Global request/response logging
// ===========================================================================
if (typeof axios !== 'undefined') {
    axios.interceptors.request.use(request => {
        console.log(`[API] >>> REQUEST: ${request.method.toUpperCase()} ${request.url}`);
        if (request.data) {
            console.log('[API] >>> Payload:', request.data);
        }
        if (request.params) {
            console.log('[API] >>> Params:', request.params);
        }
        return request;
    });

    axios.interceptors.response.use(
        response => {
            console.log(`[API] <<< RESPONSE: ${response.config.method.toUpperCase()} ${response.config.url} - Status: ${response.status}`);
            console.log('[API] <<< Data:', response.data);
            return response;
        },
        error => {
            console.error('[API] <<< ERROR:', {
                url: error.config?.url,
                method: error.config?.method,
                status: error.response?.status,
                message: error.message,
                data: error.response?.data
            });
            return Promise.reject(error);
        }
    );
} else {
    console.error('[API] FATAL: Axios is not loaded! API calls will fail.');
}

// ===========================================================================
// API MODULE
// ===========================================================================
export const Api = {

    // =========================================================================
    // TASKS
    // =========================================================================

    /**
     * Fetch all tasks (legacy bulk load)
     */
    async fetchTasks() {
        console.log('[API] fetchTasks() - Fetching all tasks');
        const response = await axios.get(TASKS_URL);
        console.log(`[API] fetchTasks() - Returned ${response.data.length} tasks`);
        return response.data;
    },

    /**
     * Fetch tasks for a specific lane
     * @param {number} swimLaneId - The lane ID
     */
    async fetchTasksByLane(swimLaneId) {
        console.log(`[API] fetchTasksByLane() - Params: { swimLaneId: ${swimLaneId} }`);
        const response = await axios.get(`${TASKS_URL}/swimlane/${swimLaneId}`);
        console.log(`[API] fetchTasksByLane(${swimLaneId}) - Returned ${response.data.length} tasks`);
        return response.data;
    },

    /**
     * Create a new task
     * @param {Object} taskData - { name, status, swimLane: { id }, tags, etc. }
     */
    async createTask(taskData) {
        console.log('[API] createTask() - Params:', taskData);
        const response = await axios.post(TASKS_URL, taskData);
        console.log('[API] createTask() - Created task:', response.data);
        return response.data;
    },

    /**
     * Update an existing task
     * @param {number} id - Task ID
     * @param {Object} taskData - Updated task data
     */
    async updateTask(id, taskData) {
        console.log(`[API] updateTask() - Params: { id: ${id}, data:`, taskData, '}');
        const response = await axios.put(`${TASKS_URL}/${id}`, taskData);
        console.log('[API] updateTask() - Updated task:', response.data);
        return response.data;
    },

    /**
     * Move a task to a new status/lane/position
     * @param {number} id - Task ID
     * @param {string} status - New status (TODO, IN_PROGRESS, etc.)
     * @param {number} swimLaneId - New lane ID
     * @param {number} position - New position in the column (optional)
     */
    async moveTask(id, status, swimLaneId, position) {
        console.log('[API] moveTask() - Params:', { id, status, swimLaneId, position });

        let url = `${TASKS_URL}/${id}/move?status=${status}&swimLaneId=${swimLaneId}`;
        if (position !== undefined && position !== null) {
            url += `&position=${position}`;
        }

        console.log(`[API] moveTask() - Full URL: ${url}`);
        const response = await axios.patch(url);
        console.log('[API] moveTask() - Response:', response.data);
        return response.data;
    },

    /**
     * Delete a task
     * @param {number} id - Task ID
     */
    async deleteTask(id) {
        console.log(`[API] deleteTask() - Params: { id: ${id} }`);
        await axios.delete(`${TASKS_URL}/${id}`);
        console.log(`[API] deleteTask(${id}) - Deleted successfully`);
    },

    // =========================================================================
    // COMMENTS
    // =========================================================================

    /**
     * Add a comment to a task
     * @param {number} taskId - Task ID
     * @param {string} text - Comment text
     */
    async addComment(taskId, text) {
        console.log('[API] addComment() - Params:', { taskId, text });
        const response = await axios.post(`${TASKS_URL}/${taskId}/comments`, text, {
            headers: { 'Content-Type': 'text/plain' }
        });
        console.log('[API] addComment() - Response:', response.data);
        return response.data;
    },

    // =========================================================================
    // SWIMLANES
    // =========================================================================

    /**
     * Fetch all active (non-completed) swimlanes
     */
    async fetchSwimLanes() {
        console.log('[API] fetchSwimLanes() - Fetching active swimlanes');
        const response = await axios.get(`${SWIMLANES_URL}/active`);
        console.log(`[API] fetchSwimLanes() - Returned ${response.data.length} lanes`);
        return response.data;
    },

    /**
     * Fetch completed swimlanes
     */
    async fetchCompletedSwimLanes() {
        console.log('[API] fetchCompletedSwimLanes() - Fetching completed swimlanes');
        const response = await axios.get(`${SWIMLANES_URL}/completed`);
        console.log(`[API] fetchCompletedSwimLanes() - Returned ${response.data.length} lanes`);
        return response.data;
    },

    /**
     * Create a new swimlane
     * @param {string} name - Swimlane name
     */
    async createSwimLane(name) {
        console.log(`[API] createSwimLane() - Params: { name: "${name}" }`);
        const response = await axios.post(SWIMLANES_URL, { name });
        console.log('[API] createSwimLane() - Created lane:', response.data);
        return response.data;
    },

    /**
     * Reorder swimlanes
     * @param {Array<number>} orderedIds - Array of lane IDs in new order
     */
    async reorderSwimlanes(orderedIds) {
        console.log('[API] reorderSwimlanes() - Params:', { orderedIds });
        await axios.patch(`${SWIMLANES_URL}/reorder`, orderedIds);
        console.log('[API] reorderSwimlanes() - Reorder successful');
    },

    /**
     * Mark a swimlane as complete
     * @param {number} id - Swimlane ID
     */
    async completeSwimLane(id) {
        console.log(`[API] completeSwimLane() - Params: { id: ${id} }`);
        const response = await axios.patch(`${SWIMLANES_URL}/${id}/complete`);
        console.log('[API] completeSwimLane() - Response:', response.data);
        return response.data;
    },

    /**
     * Delete a swimlane (soft delete)
     * @param {number} id - Swimlane ID
     */
    async deleteSwimLane(id) {
        console.log(`[API] deleteSwimLane() - Params: { id: ${id} }`);
        await axios.delete(`${SWIMLANES_URL}/${id}`);
        console.log(`[API] deleteSwimLane(${id}) - Deleted successfully`);
    },

    // =========================================================================
    // SERVER-SENT EVENTS (SSE)
    // =========================================================================

    /**
     * Initialize SSE connection for real-time updates
     * @param {Function} onTaskUpdate - Callback for task updates
     * @param {Function} onTaskDelete - Callback for task deletions
     * @param {Function} onLaneUpdate - Callback for lane updates
     */
    initSSE(onTaskUpdate, onTaskDelete, onLaneUpdate) {
        console.log('[SSE] Initializing connection to', SSE_URL);
        const eventSource = new EventSource(SSE_URL);

        eventSource.onopen = () => {
            console.log('[SSE] Connection established');
        };

        eventSource.addEventListener('task-updated', (e) => {
            const data = JSON.parse(e.data);
            console.log('[SSE] Event: task-updated', data);
            onTaskUpdate(data);
        });

        eventSource.addEventListener('task-deleted', (e) => {
            const id = JSON.parse(e.data);
            console.log('[SSE] Event: task-deleted', { taskId: id });
            onTaskDelete(id);
        });

        eventSource.addEventListener('lane-updated', (e) => {
            const lane = JSON.parse(e.data);
            console.log('[SSE] Event: lane-updated', lane);
            onLaneUpdate(lane);
        });

        eventSource.onerror = (e) => {
            console.error('[SSE] Connection error, will reconnect in 5s...', e);
            eventSource.close();
            setTimeout(() => this.initSSE(onTaskUpdate, onTaskDelete, onLaneUpdate), 5000);
        };

        return eventSource;
    }
};
