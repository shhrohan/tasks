-- V1__Create_indexes_for_performance.sql
-- Database performance optimization indexes

-- ================================================================
-- SWIM_LANES TABLE INDEXES
-- ================================================================

-- Index on user_id for filtering by user
CREATE INDEX IF NOT EXISTS idx_swim_lanes_user_id 
    ON swim_lanes(user_id);

-- Composite index for active/deleted swimlanes by user
CREATE INDEX IF NOT EXISTS idx_swim_lanes_user_deleted_position 
    ON swim_lanes(user_id, is_deleted, position_order);

-- Composite index for completed/active queries
CREATE INDEX IF NOT EXISTS idx_swim_lanes_user_completed_deleted 
    ON swim_lanes(user_id, is_completed, is_deleted, position_order);

-- ================================================================
-- USERS TABLE INDEXES
-- ================================================================

-- Unique index on email for fast login lookup
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email 
    ON users(email);

-- ================================================================
-- TASKS TABLE INDEXES
-- ================================================================

-- Index on swim_lane_id for foreign key lookups
CREATE INDEX IF NOT EXISTS idx_tasks_swim_lane_id 
    ON tasks(swim_lane_id);

-- Index on status for filtering by status
CREATE INDEX IF NOT EXISTS idx_tasks_status 
    ON tasks(status);

-- Composite index for getting tasks by lane and status
CREATE INDEX IF NOT EXISTS idx_tasks_lane_status_position 
    ON tasks(swim_lane_id, status, position_order);
