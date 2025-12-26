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
const USER_URL = '/api/user';
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

    /**
     * Update a comment
     * @param {number} taskId - Task ID
     * @param {number} commentId - Comment ID
     * @param {string} text - New comment text
     */
    async updateComment(taskId, commentId, text) {
        console.log('[API] updateComment() - Params:', { taskId, commentId, text });
        const response = await axios.put(`${TASKS_URL}/${taskId}/comments/${commentId}`, text, {
            headers: { 'Content-Type': 'text/plain' }
        });
        console.log('[API] updateComment() - Response:', response.data);
        return response.data;
    },

    /**
     * Delete a comment
     * @param {number} taskId - Task ID
     * @param {number} commentId - Comment ID
     */
    async deleteComment(taskId, commentId) {
        console.log('[API] deleteComment() - Params:', { taskId, commentId });
        await axios.delete(`${TASKS_URL}/${taskId}/comments/${commentId}`);
        console.log('[API] deleteComment() - Deleted successfully');
    },

    // =========================================================================
    // SWIMLANES
    // =========================================================================

    /**
     * Fetch all swimlanes (including completed and active)
     */
    async fetchAllSwimLanes() {
        console.log('[API] fetchAllSwimLanes() - Fetching all swimlanes');
        const response = await axios.get(SWIMLANES_URL);
        console.log(`[API] fetchAllSwimLanes() - Returned ${response.data.length} lanes`);
        return response.data;
    },

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
     * Mark a swimlane as uncomplete (reactivate)
     * @param {number} id - Swimlane ID
     */
    async uncompleteSwimLane(id) {
        console.log(`[API] uncompleteSwimLane() - Params: { id: ${id} }`);
        const response = await axios.patch(`${SWIMLANES_URL}/${id}/uncomplete`);
        console.log('[API] uncompleteSwimLane() - Response:', response.data);
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
    // USER
    // =========================================================================

    /**
     * Update user details (e.g. name)
     * @param {string} name - New name
     */
    async updateUser(name) {
        console.log(`[API] updateUser() - Params: { name: "${name}" }`);
        const response = await axios.put(USER_URL, name, {
            headers: { 'Content-Type': 'text/plain' }
        });
        console.log('[API] updateUser() - Response:', response.data);
        return response.data;
    },

    // =========================================================================
    // SERVER-SENT EVENTS (SSE) with Connection Status Tracking
    // =========================================================================

    /** Active SSE connection */
    currentEventSource: null,

    /** Prevents stacking beforeunload listeners on reconnects */
    beforeUnloadRegistered: false,

    /** Connection state */
    isConnected: true,
    reconnectAttempts: 0,
    maxReconnectAttempts: 10,
    lastHeartbeat: Date.now(),
    monitorInterval: null,

    /**
     * Show connection lost overlay
     */
    showConnectionLostOverlay() {
        let overlay = document.getElementById('connection-lost-overlay');
        if (!overlay) {
            overlay = document.createElement('div');
            overlay.id = 'connection-lost-overlay';
            overlay.classList.add('connection-navigation-overlay'); // Using class for CSS targeting
            overlay.innerHTML = `
                <div class="connection-lost-content animate__animated animate__fadeInDown">
                    <div class="connection-icon-wrapper mb-3">
                        <i class="fa-solid fa-wifi-slash fa-3x"></i>
                        <div class="connection-pulse"></div>
                    </div>
                    <h4>Connection Lost</h4>
                    <p class="text-secondary mb-3">The server is restarting or your connection was interrupted. Attempting to reconnect...</p>
                    <div class="d-flex align-items-center justify-content-center gap-2 mb-2">
                        <div class="spinner-border spinner-border-sm text-primary" role="status"></div>
                        <span class="small" id="reconnect-status">Initializing...</span>
                    </div>
                    <div class="reconnect-progress-container mt-3">
                        <div class="reconnect-progress-bar" id="reconnect-progress"></div>
                    </div>
                </div>
            `;
            document.body.appendChild(overlay);
        }
        overlay.style.display = 'flex';
        console.log('[SSE] Connection lost overlay shown');
    },

    /**
     * Hide connection lost overlay
     */
    hideConnectionLostOverlay() {
        const overlay = document.getElementById('connection-lost-overlay');
        if (overlay) {
            overlay.style.display = 'none';
        }
        console.log('[SSE] Connection restored, overlay hidden');
    },

    /**
     * Update reconnect status message
     */
    updateReconnectStatus(message) {
        const statusEl = document.getElementById('reconnect-status');
        if (statusEl) {
            statusEl.textContent = message;
        }

        // Also update progress bar if we have attempt count
        if (message.includes('Attempt')) {
            const match = message.match(/Attempt (\d+)\/(\d+)/);
            if (match) {
                const current = parseInt(match[1]);
                const total = parseInt(match[2]);
                const progressEl = document.getElementById('reconnect-progress');
                if (progressEl) {
                    progressEl.style.width = `${(current / total) * 100}%`;
                }
            }
        }
    },

    /**
     * Proactive connection monitoring
     * Starts a loop to check if heartbeat is stale
     */
    startConnectionMonitor() {
        if (this.monitorInterval) {
            clearInterval(this.monitorInterval);
        }

        console.log('[SSE] Starting connection monitor (checking every 5s)...');
        this.monitorInterval = setInterval(() => {
            const timeSinceHeartbeat = Date.now() - this.lastHeartbeat;

            // If offline or no heartbeat for 25s (server sends every 10s)
            const isStale = timeSinceHeartbeat > 25000;
            const isOffline = !navigator.onLine;

            if (this.isConnected && (isStale || isOffline)) {
                console.warn(`[SSE] Connection monitor detected failure! Stale: ${isStale} (${Math.round(timeSinceHeartbeat / 1000)}s), Offline: ${isOffline}`);
                this.isConnected = false;
                this.showConnectionLostOverlay();
                this.updateReconnectStatus(isOffline ? 'You are offline' : 'Waiting for server heartbeat...');

                // If stale, try to re-init SSE immediately to trigger native reconnect logic
                if (isStale && this.currentEventSource) {
                    this.currentEventSource.close();
                    this.currentEventSource = null;
                }
            } else if (!isStale && isOffline && !this.isConnected) {
                // If back online but still disconnected, keep overlay but update text
                this.updateReconnectStatus('Re-establishing connection...');
            }
        }, 5000);
    },

    /**
     * Initialize SSE connection for real-time updates
     * @param {Function} onTaskUpdate - Callback for task updates
     * @param {Function} onTaskDelete - Callback for task deletions
     * @param {Function} onLaneUpdate - Callback for lane updates
     */
    initSSE(onTaskUpdate, onTaskDelete, onLaneUpdate) {
        console.log('[SSE] Initializing connection to', SSE_URL);

        // Close any existing connection before creating a new one
        if (this.currentEventSource) {
            console.log('[SSE] Closing previous connection');
            this.currentEventSource.close();
            this.currentEventSource = null;
        }

        const eventSource = new EventSource(SSE_URL);
        this.currentEventSource = eventSource;

        // Register cleanup on page unload/refresh (only once to prevent stacking)
        if (!this.beforeUnloadRegistered) {
            window.addEventListener('beforeunload', () => {
                if (this.currentEventSource) {
                    console.log('[SSE] Page unloading, closing connection');
                    this.currentEventSource.close();
                    this.currentEventSource = null;
                }
            });
            this.beforeUnloadRegistered = true;
            console.log('[SSE] Registered beforeunload cleanup handler');
        }

        eventSource.onopen = () => {
            console.log('[SSE] Connection established');
            this.isConnected = true;
            this.reconnectAttempts = 0;
            // Reset lastHeartbeat to prevent monitor from seeing stale timestamp
            this.lastHeartbeat = Date.now();
            this.hideConnectionLostOverlay();
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

        eventSource.addEventListener('heartbeat', (e) => {
            // console.log('[SSE] Event: heartbeat (pong)');
            this.lastHeartbeat = Date.now();

            // If we were marked as disconnected, restore state
            if (!this.isConnected) {
                console.log('[SSE] Connection restored via heartbeat');
                this.isConnected = true;
                this.reconnectAttempts = 0;
                this.hideConnectionLostOverlay();
            }
        });

        eventSource.onerror = (e) => {
            console.error('[SSE] Connection error event fired', e);
            this.isConnected = false;
            this.reconnectAttempts++;

            // Close old source - native EventSource usually tries to reconnect itself,
            // but we use our own timeout-based initSSE for more control over intervals.
            if (this.currentEventSource) {
                this.currentEventSource.close();
                this.currentEventSource = null;
            }

            // Show overlay on first disconnect
            this.showConnectionLostOverlay();

            if (this.reconnectAttempts <= this.maxReconnectAttempts) {
                const delay = Math.min(2000 * this.reconnectAttempts, 10000); // 2s, 4s, 6s... Max 10s for faster recovery
                this.updateReconnectStatus(`Attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts}... (${delay / 1000}s)`);
                console.log(`[SSE] Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts})`);
                setTimeout(() => this.initSSE(onTaskUpdate, onTaskDelete, onLaneUpdate), delay);
            } else {
                this.updateReconnectStatus('Connection failed. Please refresh the page.');
                console.error('[SSE] Max reconnect attempts reached');
            }
        };

        return eventSource;
    }
};
