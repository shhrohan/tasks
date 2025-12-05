// ============================================================================
// TODO APP - Rewritten with Alpine.js, Axios, GSAP, and Bootstrap 5
// ============================================================================

document.addEventListener('alpine:init', () => {
    Alpine.data('todoApp', () => ({
        // API Configuration
        API_URL: '/api/tasks',
        SWIMLANE_URL: '/api/swimlanes',

        // State
        tasks: [],
        swimLanes: [],
        currentView: 'main', // 'main', 'list', 'completed'
        showStats: false,
        showDoneTasks: true,
        showSaveIndicator: false,
        columns: ['TODO', 'IN_PROGRESS', 'DONE', 'BLOCKED', 'DEFERRED'],
        loadingLanes: {}, // Track loading state per swimlane

        // Modal/Offcanvas Instances (Bootstrap)
        taskPane: null, // Removed bootstrap instance
        laneModal: null,
        confirmModal: null,
        confirmModal: null,
        // statsModal removed

        // Charts
        statusChart: null,
        swimlaneChart: null,

        // Modal State
        isTaskOpen: false, // Custom slide-out state
        modalTitle: 'Create Task',
        isEditMode: false,
        isViewMode: false,
        confirmMessage: '',
        confirmType: null,
        confirmId: null,

        // Form data
        currentTask: {
            id: null,
            name: '',
            tags: [],
            comments: ''
        },
        newTag: '',
        searchTags: [],
        newSearchTag: '',
        currentLane: {
            name: ''
        },
        selectedSwimLaneId: null,

        // Computed properties
        get activeLanes() {
            return this.swimLanes.filter(lane => !lane.isCompleted && !lane.isDeleted);
        },

        get visibleLanes() {
            // If no search active, return all active lanes
            if (this.searchTags.length === 0 && !this.newSearchTag.trim()) {
                return this.activeLanes;
            }

            // Calculate matching tasks for each lane
            const lanesWithCounts = this.activeLanes.map(lane => {
                const matchCount = this.tasks.filter(t =>
                    t.swimLane?.id === lane.id && this.isTaskVisible(t)
                ).length;
                return { lane, matchCount };
            });

            // Filter out lanes with 0 matches and sort by count descending
            return lanesWithCounts
                .filter(item => item.matchCount > 0)
                .sort((a, b) => b.matchCount - a.matchCount)
                .map(item => item.lane);
        },

        get completedLanes() {
            return this.swimLanes.filter(lane => lane.isCompleted && !lane.isDeleted);
        },

        get filteredTasks() {
            let tasks = this.tasks.filter(t => !t.swimLane?.isDeleted && !t.swimLane?.isCompleted);

            // Filter by search tags
            if (this.searchTags.length > 0) {
                tasks = tasks.filter(t => {
                    if (!t.tags) return false;
                    const taskTags = this.getTaskTags(t.tags);
                    return this.searchTags.every(tag => taskTags.includes(tag));
                });
            }

            // Sort by Swimlane then Name
            return tasks.sort((a, b) => {
                const laneA = a.swimLane?.name || '';
                const laneB = b.swimLane?.name || '';
                return laneA.localeCompare(laneB) || a.name.localeCompare(b.name);
            });
        },


        getSwimLaneStats(laneId) {
            const laneTasks = this.tasks.filter(t => t.swimLane && t.swimLane.id === laneId);
            const total = laneTasks.length;
            if (total === 0) return { total: 0, done: 0, percent: 0, todo: 0 };

            const done = laneTasks.filter(t => t.status === 'DONE').length;
            const todo = laneTasks.filter(t => t.status === 'TODO' || t.status === 'IN_PROGRESS').length;

            return {
                total,
                done,
                todo,
                percent: Math.round((done / total) * 100)
            };
        },

        // Initialize app
        async init() {
            try {
                if (typeof axios === 'undefined') {
                    console.error('CRITICAL: Axios is undefined!');
                } else {
                    console.log('Axios is defined');
                }

                // Global Axios Interceptor for Logging
                axios.interceptors.request.use(request => {
                    console.log(`[API Request] ${request.method.toUpperCase()} ${request.url}`);
                    return request;
                });

                axios.interceptors.response.use(response => {
                    console.log(`[API Response] ${response.config.method.toUpperCase()} ${response.config.url} - ${response.status}`);
                    return response;
                }, error => {
                    console.error('[API Error]', error);
                    return Promise.reject(error);
                });

                // Initialize Bootstrap Modals (Offcanvas removed)
                // this.taskPane = ... (Removed)
                this.laneModal = new bootstrap.Modal(document.getElementById('laneModal'));
                this.confirmModal = new bootstrap.Modal(document.getElementById('confirmModal'));
                // this.statsModal removed

                // Load swimlanes first and update UI incrementally
                await this.fetchSwimLanes();

                // Initialize loading state for all lanes to ensure reactivity
                // Swimlane Reordering
                this.$nextTick(() => {
                    const boardContainer = document.querySelector('.board-container');
                    if (boardContainer) {
                        new Sortable(boardContainer, {
                            animation: 150,
                            handle: '.swimlane-title', // Drag by header title area
                            draggable: '.swimlane-row',
                            onEnd: (evt) => {
                                const lanes = Array.from(boardContainer.querySelectorAll('.swimlane-row'));
                                const orderedIds = lanes.map(lane => parseInt(lane.dataset.laneId));

                                axios.patch(`${this.SWIMLANE_URL}/reorder`, orderedIds)
                                    .then(() => {
                                        console.log('Swimlane order updated');
                                        // Update local data order without re-fetching
                                        // This is tricky with active/completed filters, but visual order is already correct via DOM
                                    })
                                    .catch(error => {
                                        console.error('Error reordering swimlanes:', error);
                                        this.showNotification('Failed to save swimlane order', 'error');
                                    });
                            }
                        });
                    }
                });

                // Task Reordering (Existing)
                this.activeLanes.forEach(lane => {
                    this.loadingLanes[lane.id] = false;
                });

                this.$nextTick(() => {
                    this.setupSortables();
                });
                // Then load tasks and update UI incrementally
                await this.fetchTasks();
                this.$nextTick(() => {
                    this.setupSortables();
                });

                this.initSSE();

                this.$nextTick(() => {
                    this.animateElements();
                });
            } catch (error) {
                console.error('Initialization error:', error);
                this.showNotification('Failed to load data. Is the server running?', 'error');
            }
        },

        // =====================================================================
        // API Methods
        // =====================================================================

        async fetchTasks() {
            // Clear existing tasks to ensure clean slate, or handle differential updates?
            // User wants individual loading. Let's clear and rebuild.
            this.tasks = [];

            // We need active lanes to fetch tasks for.
            // Ensure swimlanes are loaded first (init does this).

            const fetchPromises = this.activeLanes.map(async (lane) => {
                try {
                    this.loadingLanes[lane.id] = true;
                    // Force reactivity if needed, though simple assignment usually works for objects in Alpine if defined

                    const response = await axios.get(`${this.API_URL}/swimlane/${lane.id}`);
                    const newTasks = response.data;

                    // Add to tasks array safely
                    // We filter out any existing tasks with same ID just in case (though shouldn't happen with fresh load)
                    const existingIds = new Set(this.tasks.map(t => t.id));
                    const uniqueNewTasks = newTasks.filter(t => !existingIds.has(t.id));

                    this.tasks = [...this.tasks, ...uniqueNewTasks];

                    // Smart Expansion: Only expand if tasks found (and current view allows)
                    if (newTasks.length > 0) {
                        lane.collapsed = false;
                    }

                } catch (error) {
                    console.error(`Error fetching tasks for lane ${lane.id}:`, error);
                    this.showNotification(`Failed to load tasks for ${lane.name}`, 'error');
                } finally {
                    this.loadingLanes[lane.id] = false;
                }
            });

            await Promise.all(fetchPromises);
        },

        async fetchSwimLanes() {
            try {
                const response = await axios.get(this.SWIMLANE_URL);
                this.swimLanes = response.data.map(lane => ({
                    ...lane,
                    collapsed: true // Default to collapsed
                }));
            } catch (error) {
                console.error('Error fetching swim lanes:', error);
            }
        },

        // =====================================================================
        // SSE Methods
        // =====================================================================

        initSSE() {
            console.log('Initializing SSE connection...');
            const eventSource = new EventSource('/api/sse/stream');

            eventSource.onopen = () => {
                console.log('%c SSE Connected successfully to /api/sse/stream', 'background: #222; color: #bada55');
                this.showNotification('Connected', 'success');
            };

            eventSource.addEventListener('task-updated', (e) => {
                const task = JSON.parse(e.data);
                console.log('SSE: task-updated', task);
                this.showNotification('Changes saved', 'success');
                this.handleSseTaskUpdate(task);
            });

            eventSource.addEventListener('task-deleted', (e) => {
                const id = JSON.parse(e.data);
                console.log('SSE: task-deleted', id);
                this.handleSseTaskDelete(id);
            });

            eventSource.addEventListener('lane-updated', (e) => {
                const lane = JSON.parse(e.data);
                console.log('SSE: lane-updated', lane);
                this.handleSseLaneUpdate(lane);
            });

            eventSource.onerror = (e) => {
                console.error('SSE Error:', e);
                eventSource.close();
                // Reconnect after 5 seconds
                setTimeout(() => this.initSSE(), 5000);
            };
        },

        handleSseTaskUpdate(updatedTask) {
            const index = this.tasks.findIndex(t => t.id === updatedTask.id);
            if (index !== -1) {
                // Update existing - SMART MERGE to avoid redraws
                const localTask = this.tasks[index];

                // Only update properties, keep the object reference
                Object.keys(updatedTask).forEach(key => {
                    // Special handling for swimLane to avoid object replacement if ID matches
                    if (key === 'swimLane' && localTask.swimLane && updatedTask.swimLane) {
                        if (localTask.swimLane.id !== updatedTask.swimLane.id) {
                            localTask.swimLane = updatedTask.swimLane;
                        }
                    } else {
                        localTask[key] = updatedTask[key];
                    }
                });

                // If this is the current task in modal, update it too
                if (this.currentTask && this.currentTask.id === updatedTask.id) {
                    this.refreshTaskData(updatedTask);
                }
            } else {
                // Add new (if it belongs to an active lane)
                this.tasks.push(updatedTask);
            }
            // Only re-setup sortables if we actually added a new task or changed lanes
            // For simple updates, we don't need to destroy/recreate sortables
            // this.$nextTick(() => {
            //     this.setupSortables();
            // });
        },

        handleSseTaskDelete(id) {
            this.tasks = this.tasks.filter(t => t.id !== id);
            if (this.currentTask && this.currentTask.id === id) {
                this.taskPane.hide();
            }
        },

        handleSseLaneUpdate(updatedLane) {
            const index = this.swimLanes.findIndex(l => l.id === updatedLane.id);
            if (index !== -1) {
                this.swimLanes[index] = updatedLane;
            } else {
                this.swimLanes.push(updatedLane);
            }
            this.$nextTick(() => this.setupSortables());
        },

        async createSwimLane(name) {
            try {
                const response = await axios.post(this.SWIMLANE_URL, { name });
                this.swimLanes.push(response.data);
                this.laneModal.hide();
                this.showNotification('Swimlane created!', 'success');
                this.$nextTick(() => this.setupSortables());
            } catch (error) {
                console.error('Error creating swim lane:', error);
                this.showNotification('Failed to create swimlane', 'error');
            }
        },

        async createTask(taskData) {
            try {
                const response = await axios.post(this.API_URL, taskData);
                this.tasks.push(response.data);
                this.taskPane.hide();
                this.showNotification('Task created!', 'success');
                this.$nextTick(() => this.setupSortables());
            } catch (error) {
                console.error('Error creating task:', error);
                this.showNotification('Failed to create task', 'error');
            }
        },

        async updateTask(id, taskData) {
            try {
                const response = await axios.put(`${this.API_URL}/${id}`, taskData);
                const index = this.tasks.findIndex(t => t.id === id);
                if (index !== -1) this.tasks[index] = response.data;
                this.taskPane.hide();
                this.showNotification('Task updated!', 'success');
            } catch (error) {
                console.error('Error updating task:', error);
                this.showNotification('Failed to update task', 'error');
            }
        },

        confirmSaveWithUnsavedTag() {
            this.newTag = ''; // Clear the unsaved tag

            // Remove receding effect
            const taskOffcanvas = document.getElementById('taskOffcanvas');
            if (taskOffcanvas) taskOffcanvas.classList.remove('modal-receding');

            const modalEl = document.getElementById('unsavedTagModal');
            const modal = bootstrap.Modal.getInstance(modalEl);
            if (modal) {
                modal.hide();
            }
            this.handleTaskSubmit(); // Retry save
        },

        cancelUnsavedTag() {
            // Remove receding effect
            const taskOffcanvas = document.getElementById('taskOffcanvas');
            if (taskOffcanvas) taskOffcanvas.classList.remove('modal-receding');

            const modalEl = document.getElementById('unsavedTagModal');
            const modal = bootstrap.Modal.getInstance(modalEl);
            if (modal) {
                modal.hide();
            }
        },

        async deleteTask(id) {
            try {
                await axios.delete(`${this.API_URL}/${id}`);
                this.tasks = this.tasks.filter(t => t.id !== id);
                this.taskPane.hide();
                this.showNotification('Task deleted!', 'success');
            } catch (error) {
                console.error('Error deleting task:', error);
                this.showNotification('Failed to delete task', 'error');
            }
        },

        async moveTask(id, newStatus, newSwimLaneId) {
            // Optimistic Update
            const task = this.tasks.find(t => t.id == id);
            const originalStatus = task ? task.status : null;
            const originalSwimLane = task ? task.swimLane : null;

            if (task) {
                console.log(`[DEBUG] Optimistic Move: Task ${id} -> Status: ${newStatus}, Lane: ${newSwimLaneId}`);
                console.log('[DEBUG] Task before update:', JSON.parse(JSON.stringify(task)));
                task.status = newStatus;
                if (newSwimLaneId) {
                    task.swimLane = this.swimLanes.find(l => l.id == newSwimLaneId);
                }
                console.log('[DEBUG] Task after update:', JSON.parse(JSON.stringify(task)));
            }

            try {
                let url = `${this.API_URL}/${id}/move?status=${newStatus}`;
                if (newSwimLaneId) url += `&swimLaneId=${newSwimLaneId}`;

                await axios.patch(url);

                // PURE OPTIMISTIC UI:
                // We assume the backend succeeded and matches our local state.
                // We DO NOT update the local task with the response to avoid redraws.
                // Just show the save indicator.
                this.triggerSaveIndicator();

            } catch (error) {
                console.error('Error moving task:', error);
                // Revert on error
                if (task) {
                    task.status = originalStatus;
                    task.swimLane = originalSwimLane;
                }
                this.showNotification('Failed to move task', 'error');
            }
        },

        async completeSwimLane(id) {
            try {
                const response = await axios.patch(`${this.SWIMLANE_URL}/${id}/complete`);
                const index = this.swimLanes.findIndex(l => l.id === id);
                if (index !== -1) this.swimLanes[index] = response.data;

                this.currentView = 'completed';
                this.showNotification('Swimlane completed!', 'success');
                this.$nextTick(() => this.animateElements());
            } catch (error) {
                console.error('Error completing swim lane:', error);
                this.showNotification('Failed to complete swimlane', 'error');
            }
        },

        async uncompleteSwimLane(id) {
            try {
                const response = await axios.patch(`${this.SWIMLANE_URL}/${id}/uncomplete`);
                const index = this.swimLanes.findIndex(l => l.id === id);
                if (index !== -1) this.swimLanes[index] = response.data;

                this.currentView = 'main';
                this.showNotification('Swimlane reactivated!', 'success');
                this.$nextTick(() => this.animateElements());
            } catch (error) {
                console.error('Error uncompleting swim lane:', error);
                this.showNotification('Failed to reactivate swimlane', 'error');
            }
        },

        async deleteSwimLane(id) {
            try {
                await axios.delete(`${this.SWIMLANE_URL}/${id}`);
                this.swimLanes = this.swimLanes.filter(l => l.id !== id);
                this.tasks = this.tasks.filter(t => !t.swimLane || t.swimLane.id !== id);
                this.showNotification('Swimlane deleted!', 'success');
            } catch (error) {
                console.error('Error deleting swim lane:', error);
                this.showNotification('Failed to delete swimlane', 'error');
            }
        },

        // =====================================================================
        // UI Methods
        // =====================================================================

        addTag() {
            const tag = this.newTag.trim();
            if (tag && !this.currentTask.tags.includes(tag)) {
                this.currentTask.tags.push(tag);
            }
            this.newTag = '';
        },

        removeTag(index) {
            this.currentTask.tags.splice(index, 1);
        },

        addSearchTag() {
            const tag = this.newSearchTag.trim();
            if (tag && !this.searchTags.includes(tag)) {
                this.searchTags.push(tag);
            }
            this.newSearchTag = '';
        },

        removeSearchTag(index) {
            this.searchTags.splice(index, 1);
        },

        clearSearch() {
            this.searchTags = [];
            this.newSearchTag = '';
        },

        toggleView() {
            if (this.currentView === 'main') {
                this.currentView = 'list';
            } else if (this.currentView === 'list') {
                this.currentView = 'completed';
            } else {
                this.currentView = 'main';
            }
            this.$nextTick(() => this.animateElements());
        },

        selectTask(task) {
            this.loadTaskDetails(task);
        },

        loadTaskDetails(taskData) {
            // Always find the latest task state from the main array
            const task = this.tasks.find(t => t.id == taskData.id) || taskData;

            this.currentTask = {
                id: task.id,
                name: task.name,
                tags: this.getTaskTags(task.tags),
                comments: '', // Input for new comment
                existingComments: this.getTaskComments(task.comments), // Helper to parse existing
                status: task.status,
                swimLaneName: task.swimLane?.name
            };
            this.newTag = '';
            this.editingCommentId = null;
            this.editingCommentText = '';
            this.selectedSwimLaneId = task.swimLane?.id || null;
        },

        // --- Comment Management ---

        getTaskComments(commentsJson) {
            if (!commentsJson) return [];
            try {
                const parsed = JSON.parse(commentsJson);
                // Handle legacy array of strings
                if (Array.isArray(parsed) && parsed.length > 0 && typeof parsed[0] === 'string') {
                    return parsed.map(text => ({
                        id: 'legacy-' + Math.random().toString(36).substr(2, 9),
                        text: text,
                        createdAt: null,
                        updatedAt: null,
                        isLegacy: true
                    }));
                }
                return parsed;
            } catch (e) {
                console.error('Error parsing comments:', e);
                return [];
            }
        },

        formatDate(isoString) {
            if (!isoString) return 'Date unknown';
            let date;
            if (Array.isArray(isoString)) {
                // Handle Java LocalDateTime array [yyyy, MM, dd, HH, mm, ss]
                // JS months are 0-based
                date = new Date(isoString[0], isoString[1] - 1, isoString[2], isoString[3], isoString[4], (isoString[5] || 0));
            } else {
                date = new Date(isoString);
            }

            if (isNaN(date.getTime())) return 'Invalid Date';

            const days = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
            const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

            const dayName = days[date.getDay()];
            const day = String(date.getDate()).padStart(2, '0');
            const month = months[date.getMonth()];
            const year = date.getFullYear();
            const hours = String(date.getHours()).padStart(2, '0');
            const minutes = String(date.getMinutes()).padStart(2, '0');

            return `${dayName}, ${day}-${month}-${year} ${hours}:${minutes}`;
        },

        async addComment() {
            if (!this.currentTask.comments.trim()) return;

            const text = this.currentTask.comments;
            const taskId = this.currentTask.id;

            try {
                const response = await axios.post(`${this.API_URL}/${taskId}/comments`, text, {
                    headers: { 'Content-Type': 'text/plain' }
                });

                // Add new comment to list
                this.currentTask.existingComments.push(response.data);
                this.currentTask.comments = ''; // Clear input

                // Update main task list to reflect changes (optional, but good for consistency)
                const taskIndex = this.tasks.findIndex(t => t.id === taskId);
                if (taskIndex !== -1) {
                    // We need to update the comments string in the main list
                    // But since we don't have the full string, we might need to fetch or just trust the local update
                    // For now, let's just keep the local view updated.
                }
            } catch (error) {
                console.error('Error adding comment:', error);
                this.showNotification('Failed to add comment', 'danger');
            }
        },

        startEditingComment(comment) {
            this.editingCommentId = comment.id;
            this.editingCommentText = comment.text;
        },

        cancelEditingComment() {
            this.editingCommentId = null;
            this.editingCommentText = '';
        },

        async saveCommentEdit(comment) {
            if (!this.editingCommentText.trim()) return;

            try {
                const response = await axios.put(`${this.API_URL}/${this.currentTask.id}/comments/${comment.id}`, this.editingCommentText, {
                    headers: { 'Content-Type': 'text/plain' }
                });

                // Update local list
                const index = this.currentTask.existingComments.findIndex(c => c.id === comment.id);
                if (index !== -1) {
                    this.currentTask.existingComments[index] = response.data;
                }
                this.cancelEditingComment();
            } catch (error) {
                console.error('Error updating comment:', error);
                this.showNotification('Failed to update comment', 'danger');
            }
        },

        async deleteComment(commentId) {
            if (!confirm('Delete this comment?')) return;

            try {
                await axios.delete(`${this.API_URL}/${this.currentTask.id}/comments/${commentId}`);
                this.currentTask.existingComments = this.currentTask.existingComments.filter(c => c.id !== commentId);
            } catch (error) {
                console.error('Error deleting comment:', error);
                this.showNotification('Failed to delete comment', 'danger');
            }
        },

        openCreateLaneModal() {
            this.currentLane.name = '';
            this.laneModal.show();
        },

        toggleStats() {
            this.showStats = !this.showStats;
            if (this.showStats) {
                this.$nextTick(() => {
                    this.renderCharts();
                });
            }
        },

        renderCharts() {
            // 0. Data Fix: Filter out tasks in deleted swimlanes
            const visibleTasks = this.tasks.filter(t => t.swimLane && !t.swimLane.isDeleted);

            // 1. Status Chart Data (3D Pie)
            const statusCounts = this.columns.map(status => {
                return {
                    name: status,
                    y: visibleTasks.filter(t => t.status === status).length
                };
            }).filter(item => item.y > 0);

            // Highcharts 3D Pie
            Highcharts.chart('statusChart', {
                chart: {
                    type: 'pie',
                    options3d: {
                        enabled: true,
                        alpha: 45,
                        beta: 0
                    },
                    backgroundColor: 'transparent'
                },
                title: { text: null },
                tooltip: {
                    pointFormat: '{series.name}: <b>{point.y}</b>'
                },
                plotOptions: {
                    pie: {
                        allowPointSelect: true,
                        cursor: 'pointer',
                        depth: 35,
                        dataLabels: {
                            enabled: true,
                            format: '{point.name}'
                        }
                    }
                },
                series: [{
                    type: 'pie',
                    name: 'Tasks',
                    data: statusCounts,
                    showInLegend: true
                }],
                legend: {
                    enabled: true,
                    labelFormat: '{name}: {y}',
                    itemStyle: {
                        color: '#f1f5f9',
                        fontSize: '14px'
                    }
                },
                credits: { enabled: false }
            });

            // 2. Swimlane Chart Data (3D Column)
            const laneNames = this.activeLanes.map(l => l.name);
            const laneCounts = this.activeLanes.map(l => {
                return visibleTasks.filter(t => t.swimLane?.id === l.id).length;
            });

            // Highcharts 3D Column
            Highcharts.chart('swimlaneChart', {
                chart: {
                    type: 'column',
                    options3d: {
                        enabled: true,
                        alpha: 10,
                        beta: 25,
                        depth: 70
                    },
                    backgroundColor: 'transparent'
                },
                title: { text: null },
                xAxis: {
                    categories: laneNames,
                    labels: { skew3d: true, style: { fontSize: '16px' } }
                },
                yAxis: {
                    title: { text: null }
                },
                tooltip: {
                    headerFormat: '<b>{point.key}</b><br>',
                    pointFormat: 'Tasks: {point.y}'
                },
                plotOptions: {
                    column: {
                        depth: 25
                    }
                },
                series: [{
                    name: 'Tasks',
                    data: laneCounts,
                    colorByPoint: true
                }],
                credits: { enabled: false }
            });

            // Add rotation support to both charts
            this.addRotationSupport('statusChart', 'pie');
            this.addRotationSupport('swimlaneChart', 'column');
        },

        addRotationSupport(containerId, type) {
            const chart = Highcharts.charts.find(c => c && c.renderTo.id === containerId);
            if (!chart) return;

            const container = document.getElementById(containerId);

            // We need to re-get the chart instance after replacing the container?
            // Actually, replacing the container might kill the chart.
            // Better approach: Highcharts handles events internally, but for custom rotation we need custom listeners.
            // Let's just add listeners to the existing container. If we need to clean up, we should store the controller.
            // For simplicity in this context, we'll assume fresh modal open = fresh listeners.
            // But wait, Alpine re-runs renderCharts. We should be careful not to stack listeners.
            // The cloneNode trick destroys the chart inside. We shouldn't do that here if we want to keep the chart.
            // Instead, let's just add the listeners. The modal teardown might not remove them, but they are on the DOM element.
            // If Alpine re-renders the modal content, the DOM elements are new.

            // Let's stick to the previous pattern but make it reusable and robust.

            (function (H) {
                let isDragging = false;
                let startX;
                let startBeta;

                const chartContainer = document.getElementById(containerId);

                const mouseDownHandler = function (e) {
                    isDragging = true;
                    startX = e.clientX;
                    startBeta = chart.options.chart.options3d.beta;
                };

                const mouseMoveHandler = function (e) {
                    if (!isDragging) return;

                    const deltaX = e.clientX - startX;
                    const newBeta = startBeta + deltaX;

                    chart.options.chart.options3d.beta = newBeta;
                    chart.redraw(false);
                };

                const mouseUpHandler = function () {
                    isDragging = false;
                };

                // Remove existing listeners if we attached them before (using a custom property)
                if (chartContainer._rotationHandlers) {
                    chartContainer.removeEventListener('mousedown', chartContainer._rotationHandlers.down);
                    document.removeEventListener('mousemove', chartContainer._rotationHandlers.move);
                    document.removeEventListener('mouseup', chartContainer._rotationHandlers.up);
                }

                chartContainer.addEventListener('mousedown', mouseDownHandler);
                document.addEventListener('mousemove', mouseMoveHandler);
                document.addEventListener('mouseup', mouseUpHandler);

                // Store handlers for cleanup
                chartContainer._rotationHandlers = {
                    down: mouseDownHandler,
                    move: mouseMoveHandler,
                    up: mouseUpHandler
                };

            })(Highcharts);
        },

        openCreateTaskModal(swimLaneId) {
            this.modalTitle = 'Create Task';
            this.isEditMode = false;
            this.isViewMode = false;
            this.currentTask = { id: null, name: '', tags: [], comments: '', existingComments: [], status: 'TODO' };
            this.newTag = '';
            this.editingCommentId = null;
            this.editingCommentText = '';
            this.selectedSwimLaneId = swimLaneId;
            // this.taskPane.show();
            this.isTaskOpen = true;
        },

        refreshTaskData(taskData) {
            // Only update if the pane is open and showing THIS task
            // const offcanvasEl = document.getElementById('taskOffcanvas');
            // const isShown = offcanvasEl && offcanvasEl.classList.contains('show');
            const isShown = this.isTaskOpen;

            if (isShown && this.currentTask && this.currentTask.id == taskData.id) {
                // Update currentTask with new data without re-opening/resetting UI state
                this.currentTask.status = taskData.status;
                this.currentTask.swimLaneName = taskData.swimLane?.name;
                // We could update other fields too if needed
            }
        },

        openTaskModal(taskData) {
            this.loadTaskDetails(taskData);
            this.modalTitle = 'Task Details';
            this.isEditMode = true; // It is an existing task
            this.isViewMode = true; // Start in view mode

            // this.taskPane.show();
            this.isTaskOpen = true;
        },

        enableEditMode() {
            this.isViewMode = false;
            this.modalTitle = 'Edit Task';
        },

        closeTaskPane() {
            this.isTaskOpen = false;
        },

        cancelEdit() {
            if (this.isEditMode) {
                // Return to view mode if editing existing task
                this.isViewMode = true;
                this.modalTitle = 'Task Details';
            } else {
                // Close if creating new task
                this.isTaskOpen = false;
            }
        },

        handleTaskSubmit() {
            if (!this.selectedSwimLaneId) {
                this.showNotification('Please select a swimlane', 'warning');
                return;
            }
            if (!this.currentTask.name) {
                this.showNotification('Task name is required', 'warning');
                return;
            }

            // Check for unsaved tag
            // Check for unsaved tag
            if (this.newTag.trim()) {
                // Add receding effect to task offcanvas
                const taskOffcanvas = document.getElementById('taskOffcanvas');
                if (taskOffcanvas) taskOffcanvas.classList.add('modal-receding');

                const modal = new bootstrap.Modal(document.getElementById('unsavedTagModal'));
                modal.show();
                return;
            }

            const tags = this.currentTask.tags;

            const taskData = {
                name: this.currentTask.name,
                tags: JSON.stringify(tags),
                comments: JSON.stringify(this.currentTask.existingComments || []),
                swimLane: { id: this.selectedSwimLaneId }
            };

            if (this.isEditMode) {
                const existing = this.tasks.find(t => t.id === this.currentTask.id);
                taskData.status = existing.status;
                this.updateTask(this.currentTask.id, taskData);
            } else {
                taskData.status = 'TODO';
                this.createTask(taskData);
            }
        },



        handleLaneSubmit() {
            if (!this.currentLane.name.trim()) {
                this.showNotification('Lane name cannot be empty', 'warning');
                return;
            }
            this.createSwimLane(this.currentLane.name);
        },

        handleDeleteTask() {
            // Deprecated in favor of openConfirmModal, but kept for safety
            if (this.currentTask.id && confirm('Delete this task?')) {
                this.deleteTask(this.currentTask.id);
            }
        },

        openConfirmModal(type, id, message) {
            this.confirmType = type;
            this.confirmId = id;
            this.confirmMessage = message;
            this.confirmModal.show();
        },

        handleConfirm() {
            console.log('handleConfirm called', this.confirmType, this.confirmId);
            try {
                switch (this.confirmType) {
                    case 'complete_lane':
                        this.completeSwimLane(this.confirmId);
                        break;
                    case 'delete_lane':
                        this.deleteSwimLane(this.confirmId);
                        break;
                    case 'reactivate_lane':
                        this.uncompleteSwimLane(this.confirmId);
                        break;
                    case 'delete_task':
                        this.deleteTask(this.confirmId);
                        break;
                }
            } catch (error) {
                console.error('Error in handleConfirm:', error);
            } finally {
                if (this.confirmModal) {
                    this.confirmModal.hide();
                }
            }
        },

        // =====================================================================
        // UI Helper Methods
        // =====================================================================

        getSwimLaneTaskCount(laneId) {
            return this.tasks.filter(t => t.swimLane?.id === laneId).length;
        },

        getLaneTasksByStatus(laneId, status) {
            if (status === 'DONE' && !this.showDoneTasks) {
                return [];
            }
            return this.tasks.filter(t => {
                const matchesStatus = t.status === status && t.swimLane?.id === laneId;
                if (!matchesStatus) return false;
                return this.isTaskVisible(t);
            });
        },

        isTaskVisible(task) {
            // 1. Check Search Tags (Pills) - AND Logic
            if (this.searchTags.length > 0) {
                const taskTags = this.getTaskTags(task.tags);
                const hasAllTags = this.searchTags.every(searchTag =>
                    taskTags.some(tag => tag.toLowerCase() === searchTag.toLowerCase())
                );
                if (!hasAllTags) return false;
            }

            // 2. Check Input Text (Live Typing) - Partial Match
            const inputText = this.newSearchTag.trim().toLowerCase();
            if (inputText) {
                const taskTags = this.getTaskTags(task.tags);
                // Match against tags OR task name? User said "filter as user types", usually implies tags since it's a tag search box.
                // But context is "Tag Search". Let's match against TAGS.
                const hasPartialTag = taskTags.some(tag => tag.toLowerCase().includes(inputText));
                if (!hasPartialTag) return false;
            }

            return true;
        },

        getCompletedLaneTaskSummary(laneId) {
            const laneTasks = this.tasks.filter(t => t.swimLane?.id === laneId);
            return laneTasks.reduce((acc, task) => {
                acc[task.status] = (acc[task.status] || 0) + 1;
                return acc;
            }, {});
        },

        parseTags(tagsJson) {
            try {
                const tags = JSON.parse(tagsJson || '[]');
                return Array.isArray(tags) ? tags.join(', ') : '';
            } catch (e) {
                return '';
            }
        },

        getTaskTags(tagsJson) {
            try {
                const tags = JSON.parse(tagsJson || '[]');
                return Array.isArray(tags) ? tags : [];
            } catch (e) {
                return [];
            }
        },



        getTaskCommentCount(commentsJson) {
            try {
                const comments = JSON.parse(commentsJson || '[]');
                return Array.isArray(comments) ? comments.length : 0;
            } catch (e) {
                return 0;
            }
        },

        toggleLaneCollapse(lane) {
            lane.collapsed = !lane.collapsed;
        },

        // =====================================================================
        // Sortable Setup
        // =====================================================================

        setupSortables() {
            document.querySelectorAll('.lane-column').forEach(column => {
                if (column.sortableInstance) {
                    column.sortableInstance.destroy();
                }

                column.sortableInstance = new Sortable(column, {
                    group: 'shared',
                    animation: 150,
                    ghostClass: 'dragging',
                    onEnd: (evt) => {
                        const itemEl = evt.item;
                        const newStatus = evt.to.getAttribute('data-status');
                        const newLaneId = evt.to.getAttribute('data-lane-id');
                        const taskId = itemEl.getAttribute('data-id');
                        const oldStatus = itemEl.getAttribute('data-status');
                        const oldLaneId = itemEl.getAttribute('data-lane-id');

                        if (newStatus !== oldStatus || newLaneId !== oldLaneId) {
                            // Revert the DOM change so Alpine can handle the move via data binding
                            // This ensures the 'task' scope remains valid and prevents flickering/text loss
                            evt.from.appendChild(itemEl);

                            this.moveTask(taskId, newStatus, newLaneId);
                        }
                    }
                });
            });
        },

        // =====================================================================
        // Animations
        // =====================================================================

        animateElements() {
            this.$nextTick(() => {
                // Animate swimlane rows
                // Animate swimlane rows
                const rows = document.querySelectorAll('.swimlane-row');
                if (rows.length > 0) {
                    gsap.to(rows, {
                        duration: 0.5,
                        opacity: 1,
                        y: 0,
                        stagger: 0.1,
                        ease: 'power2.out'
                    });
                }

                // Animate task cards
                const cards = document.querySelectorAll('.task-card');
                if (cards.length > 0) {
                    gsap.to(cards, {
                        duration: 0.4,
                        opacity: 1,
                        scale: 1,
                        stagger: 0.02,
                        ease: 'back.out'
                    });
                }
            });
        },

        // =====================================================================
        // Notifications
        // =====================================================================

        triggerSaveIndicator() {
            this.showSaveIndicator = true;
            setTimeout(() => {
                this.showSaveIndicator = false;
            }, 2000);
        },

        showNotification(message, type = 'success') {
            const notification = document.createElement('div');
            notification.className = `notification notification-${type}`;

            const iconMap = {
                success: 'fa-check-circle',
                error: 'fa-exclamation-circle',
                warning: 'fa-exclamation-triangle'
            };

            notification.innerHTML = `
                <i class="fa-solid ${iconMap[type]}"></i>
                <span>${message}</span>
            `;

            document.body.appendChild(notification);

            // Trigger reflow to enable transition
            notification.offsetHeight;

            // Add show class to animate in
            notification.classList.add('show');

            // Remove after delay
            setTimeout(() => {
                notification.classList.remove('show');
                // Wait for transition to finish before removing from DOM
                setTimeout(() => {
                    if (notification.parentNode) {
                        notification.parentNode.removeChild(notification);
                    }
                }, 300);
            }, 3000);
        }
    }));
});
