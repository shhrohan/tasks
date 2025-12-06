/*
 * api.js
 * Pure Data Access Layer for Todo App.
 * Handles all Axios requests and SSE connections.
 */

// Configure Axios (Global Interceptors for MANDATORY LOGGING)
if (typeof axios !== 'undefined') {
    axios.interceptors.request.use(request => {
        console.log(`[API Request] ${request.method.toUpperCase()} ${request.url}`);
        return request;
    });

    axios.interceptors.response.use(
        response => {
            console.log(`[API Response] ${response.config.method.toUpperCase()} ${response.config.url} - ${response.status}`);
            return response;
        },
        error => {
            console.error('[API Error]', error);
            return Promise.reject(error);
        }
    );
}

const TASKS_URL = '/api/tasks';
const SWIMLANES_URL = '/api/swimlanes';
const SSE_URL = '/api/sse/stream';

export const Api = {
    // --- Tasks ---
    async fetchTasks() {
        // Fetch all tasks using the generic endpoint
        // Note: The previous app fetched per-swimlane. We can do that or fetch all.
        // Given the controller supports GET /, let's try fetching all to be efficient.
        // If that's not supported by backend logic (it was separate calls), we can adapt.
        // Checking GEMINI.md: "GET /: Get all tasks." -> Supported.
        const response = await axios.get(TASKS_URL);
        return response.data;
    },

    async createTask(taskData) {
        const response = await axios.post(TASKS_URL, taskData);
        return response.data;
    },

    async updateTask(id, taskData) {
        const response = await axios.put(`${TASKS_URL}/${id}`, taskData);
        return response.data;
    },

    async moveTask(id, status, swimLaneId) {
        // PATCH /api/tasks/{id}/move?status=...&swimLaneId=...
        const url = `${TASKS_URL}/${id}/move?status=${status}&swimLaneId=${swimLaneId}`;
        const response = await axios.patch(url);
        return response.data;
    },

    async deleteTask(id) {
        await axios.delete(`${TASKS_URL}/${id}`);
    },

    // --- Comments ---
    async addComment(taskId, text) {
        const response = await axios.post(`${TASKS_URL}/${taskId}/comments`, text, {
            headers: { 'Content-Type': 'text/plain' }
        });
        return response.data;
    },

    // --- Swimlanes ---
    async fetchSwimLanes() {
        const response = await axios.get(`${SWIMLANES_URL}/active`);
        return response.data;
    },

    async fetchCompletedSwimLanes() {
        const response = await axios.get(`${SWIMLANES_URL}/completed`);
        return response.data;
    },

    async createSwimLane(name) {
        const response = await axios.post(SWIMLANES_URL, { name });
        return response.data;
    },

    async reorderSwimlanes(orderedIds) {
        // PATCH /api/swimlanes/reorder with [1, 2, 3]
        await axios.patch(`${SWIMLANES_URL}/reorder`, orderedIds);
    },

    async completeSwimLane(id) {
        const response = await axios.patch(`${SWIMLANES_URL}/${id}/complete`);
        return response.data;
    },

    async deleteSwimLane(id) {
        await axios.delete(`${SWIMLANES_URL}/${id}`);
    },

    // --- SSE ---
    initSSE(onTaskUpdate, onTaskDelete, onLaneUpdate) {
        console.log('[SSE] Initializing connection...');
        const eventSource = new EventSource(SSE_URL);

        eventSource.onopen = () => {
            console.log('[SSE] Connected');
        };

        eventSource.addEventListener('task-updated', (e) => {
            const data = JSON.parse(e.data);
            console.log('[SSE] task-updated', data);
            onTaskUpdate(data);
        });

        eventSource.addEventListener('task-deleted', (e) => {
            const id = JSON.parse(e.data);
            console.log('[SSE] task-deleted', id);
            onTaskDelete(id);
        });

        eventSource.addEventListener('lane-updated', (e) => {
            const lane = JSON.parse(e.data);
            console.log('[SSE] lane-updated', lane);
            onLaneUpdate(lane);
        });

        eventSource.onerror = (e) => {
            console.error('[SSE] Error, reconnecting in 5s...', e);
            eventSource.close();
            setTimeout(() => this.initSSE(onTaskUpdate, onTaskDelete, onLaneUpdate), 5000);
        };

        return eventSource;
    }
};
