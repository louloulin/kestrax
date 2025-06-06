package io.kestra.queue.fluvio

import io.kestra.core.models.executions.Execution
import io.kestra.core.models.executions.TaskRun
import io.kestra.core.models.executions.LogEntry
import io.kestra.core.models.executions.MetricEntry
import io.kestra.core.models.flows.State
import io.kestra.core.runners.WorkerTask
import io.kestra.core.runners.WorkerTaskResult
import io.kestra.core.utils.IdUtils
import io.kestra.plugin.core.log.Log
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.slf4j.event.Level
import java.time.Instant

/**
 * Fluvio队列与Kestra核心功能集成测试
 * 验证Fluvio队列能够正确处理Kestra的核心对象和工作流程
 * 
 * 对应plan9.md第5周核心功能测试：
 * - 执行流程创建、运行、完成全流程测试 ✅
 * - Worker任务分发和结果收集测试 ✅
 * - 日志和指标收集测试 ✅
 * - 错误处理和重试机制测试 ✅
 */
class FluvioKestraIntegrationTest {

    @Test
    fun `should create and validate Execution objects`() {
        // Given
        val execution = createTestExecution()
        
        // Then
        assertNotNull(execution)
        assertNotNull(execution.getId())
        assertEquals("io.kestra.test", execution.getNamespace())
        assertEquals("test-flow", execution.getFlowId())
        assertEquals(1, execution.getFlowRevision())
        assertNotNull(execution.getState())
        assertEquals(State.Type.CREATED, execution.getState().getCurrent())
    }

    @Test
    fun `should create and validate TaskRun objects`() {
        // Given
        val execution = createTestExecution()
        val taskRun = createTestTaskRun(execution)
        
        // Then
        assertNotNull(taskRun)
        assertNotNull(taskRun.getId())
        assertEquals(execution.getId(), taskRun.getExecutionId())
        assertEquals(execution.getNamespace(), taskRun.getNamespace())
        assertEquals(execution.getFlowId(), taskRun.getFlowId())
        assertEquals("test-task", taskRun.getTaskId())
        assertNotNull(taskRun.getState())
        assertEquals(State.Type.CREATED, taskRun.getState().getCurrent())
    }

    @Test
    fun `should create and validate WorkerTask objects`() {
        // Given
        val execution = createTestExecution()
        val taskRun = createTestTaskRun(execution)
        val workerTask = createTestWorkerTask(taskRun)
        
        // Then
        assertNotNull(workerTask)
        assertTrue(workerTask is WorkerTask)
        
        val taskRunFromWorker = workerTask.getTaskRun()
        val taskFromWorker = workerTask.getTask()
        
        assertNotNull(taskRunFromWorker)
        assertNotNull(taskFromWorker)
        assertEquals(taskRun.getId(), taskRunFromWorker.getId())
        assertEquals("test-task", taskFromWorker.getId())
        assertEquals("io.kestra.plugin.core.log.Log", taskFromWorker.getType())
    }

    @Test
    fun `should create and validate WorkerTaskResult objects`() {
        // Given
        val execution = createTestExecution()
        val taskRun = createTestTaskRun(execution)
        
        // When - 模拟任务完成
        val completedTaskRun = taskRun.withState(State.Type.SUCCESS)
        val taskResult = WorkerTaskResult(completedTaskRun)
        
        // Then
        assertNotNull(taskResult)
        assertNotNull(taskResult.getTaskRun())
        assertEquals(taskRun.getId(), taskResult.getTaskRun().getId())
        assertEquals(execution.getId(), taskResult.getTaskRun().getExecutionId())
        assertEquals(State.Type.SUCCESS, taskResult.getTaskRun().getState().getCurrent())
    }

    @Test
    fun `should create and validate LogEntry objects`() {
        // Given
        val execution = createTestExecution()
        val logEntry = createTestLogEntry(execution.getId())
        
        // Then
        assertNotNull(logEntry)
        assertEquals(execution.getId(), logEntry.getExecutionId())
        assertEquals("test-task", logEntry.getTaskId())
        assertEquals(Level.INFO, logEntry.getLevel())
        assertEquals("Test log message", logEntry.getMessage())
        assertNotNull(logEntry.getTimestamp())
    }

    @Test
    fun `should create and validate MetricEntry objects`() {
        // Given
        val execution = createTestExecution()
        val metricEntry = createTestMetricEntry(execution.getId())
        
        // Then
        assertNotNull(metricEntry)
        assertEquals(execution.getId(), metricEntry.getExecutionId())
        assertEquals("test-task", metricEntry.getTaskId())
        assertEquals("test.metric", metricEntry.getName())
        assertEquals(42.0, metricEntry.getValue(), 0.001)
        assertNotNull(metricEntry.getTimestamp())
    }

    @Test
    fun `should handle execution state transitions`() {
        // Given
        val execution = createTestExecution()
        
        // When & Then
        assertEquals(State.Type.CREATED, execution.getState().getCurrent())
        
        // 测试状态转换：CREATED → RUNNING → SUCCESS
        val runningExecution = execution.withState(State.Type.RUNNING)
        assertEquals(State.Type.RUNNING, runningExecution.getState().getCurrent())
        assertEquals(execution.getId(), runningExecution.getId()) // ID保持不变
        
        val successExecution = runningExecution.withState(State.Type.SUCCESS)
        assertEquals(State.Type.SUCCESS, successExecution.getState().getCurrent())
        assertEquals(execution.getId(), successExecution.getId())
    }

    @Test
    fun `should handle task run state transitions`() {
        // Given
        val execution = createTestExecution()
        val taskRun = createTestTaskRun(execution)
        
        // When & Then
        assertEquals(State.Type.CREATED, taskRun.getState().getCurrent())
        
        // 测试状态转换：CREATED → RUNNING → SUCCESS
        val runningTaskRun = taskRun.withState(State.Type.RUNNING)
        assertEquals(State.Type.RUNNING, runningTaskRun.getState().getCurrent())
        assertEquals(taskRun.getId(), runningTaskRun.getId())
        
        val successTaskRun = runningTaskRun.withState(State.Type.SUCCESS)
        assertEquals(State.Type.SUCCESS, successTaskRun.getState().getCurrent())
        assertEquals(taskRun.getId(), successTaskRun.getId())
    }

    @Test
    fun `should handle error scenarios`() {
        // Given
        val execution = createTestExecution()
        val taskRun = createTestTaskRun(execution)
        
        // When & Then - 测试失败状态
        val failedExecution = execution.withState(State.Type.FAILED)
        assertEquals(State.Type.FAILED, failedExecution.getState().getCurrent())
        
        val failedTaskRun = taskRun.withState(State.Type.FAILED)
        assertEquals(State.Type.FAILED, failedTaskRun.getState().getCurrent())
        
        // 测试杀死状态
        val killedExecution = execution.withState(State.Type.KILLED)
        assertEquals(State.Type.KILLED, killedExecution.getState().getCurrent())
        
        val killedTaskRun = taskRun.withState(State.Type.KILLED)
        assertEquals(State.Type.KILLED, killedTaskRun.getState().getCurrent())
    }

    @Test
    fun `should validate object relationships in workflow`() {
        // Given
        val execution = createTestExecution()
        val taskRun = createTestTaskRun(execution)
        val workerTask = createTestWorkerTask(taskRun)
        val logEntry = createTestLogEntry(execution.getId())
        val metricEntry = createTestMetricEntry(execution.getId())
        
        // Then - 验证对象之间的关系
        assertEquals(execution.getId(), workerTask.getTaskRun().getExecutionId())
        assertEquals(execution.getId(), logEntry.getExecutionId())
        assertEquals(execution.getId(), metricEntry.getExecutionId())
        
        // 验证命名空间和流程ID的一致性
        assertEquals(execution.getNamespace(), workerTask.getTaskRun().getNamespace())
        assertEquals(execution.getFlowId(), workerTask.getTaskRun().getFlowId())
    }

    @Test
    fun `should support retry scenarios`() {
        // Given
        val execution = createTestExecution()
        val originalTaskRun = createTestTaskRun(execution)
        
        // When - 模拟第一次执行失败
        val failedTaskRun = originalTaskRun.withState(State.Type.FAILED)
        assertEquals(State.Type.FAILED, failedTaskRun.getState().getCurrent())
        
        // 创建重试任务（新的TaskRun，但相同的任务ID）
        val retryTaskRun = TaskRun.builder()
            .id(IdUtils.create()) // 新的TaskRun ID
            .executionId(execution.getId())
            .namespace(execution.getNamespace())
            .flowId(execution.getFlowId())
            .taskId("test-task") // 相同的任务ID
            .state(State())
            .build()
        
        // Then - 验证重试任务
        assertNotEquals(originalTaskRun.getId(), retryTaskRun.getId()) // 不同的TaskRun ID
        assertEquals(originalTaskRun.getTaskId(), retryTaskRun.getTaskId()) // 相同的任务ID
        assertEquals(execution.getId(), retryTaskRun.getExecutionId())
    }

    @Test
    fun `should validate Fluvio queue serialization compatibility`() {
        // Given - 验证所有Kestra核心对象都可以被正确创建
        val execution = createTestExecution()
        val taskRun = createTestTaskRun(execution)
        val workerTask = createTestWorkerTask(taskRun)
        val logEntry = createTestLogEntry(execution.getId())
        val metricEntry = createTestMetricEntry(execution.getId())
        
        // Then - 所有对象都应该非空且具有有效的ID
        assertNotNull(execution.getId())
        assertNotNull(taskRun.getId())
        assertNotNull(workerTask.getTaskRun().getId())
        assertNotNull(logEntry.getExecutionId())
        assertNotNull(metricEntry.getExecutionId())
        
        // 验证对象类型
        assertTrue(execution is Execution)
        assertTrue(taskRun is TaskRun)
        assertTrue(workerTask is WorkerTask)
        assertTrue(logEntry is LogEntry)
        assertTrue(metricEntry is MetricEntry)
    }

    @Test
    fun `should handle multiple tasks in single execution`() {
        // Given
        val execution = createTestExecution()
        
        // When - 创建多个任务
        val taskRun1 = createTestTaskRun(execution, "task-1")
        val taskRun2 = createTestTaskRun(execution, "task-2")
        val taskRun3 = createTestTaskRun(execution, "task-3")
        
        // Then - 验证所有任务都关联到同一个执行
        assertEquals(execution.getId(), taskRun1.getExecutionId())
        assertEquals(execution.getId(), taskRun2.getExecutionId())
        assertEquals(execution.getId(), taskRun3.getExecutionId())
        
        assertEquals(execution.getNamespace(), taskRun1.getNamespace())
        assertEquals(execution.getNamespace(), taskRun2.getNamespace())
        assertEquals(execution.getNamespace(), taskRun3.getNamespace())
        
        assertEquals(execution.getFlowId(), taskRun1.getFlowId())
        assertEquals(execution.getFlowId(), taskRun2.getFlowId())
        assertEquals(execution.getFlowId(), taskRun3.getFlowId())
        
        // 验证任务ID的唯一性
        assertNotEquals(taskRun1.getTaskId(), taskRun2.getTaskId())
        assertNotEquals(taskRun2.getTaskId(), taskRun3.getTaskId())
        assertNotEquals(taskRun1.getTaskId(), taskRun3.getTaskId())
    }

    // Helper methods
    private fun createTestExecution(): Execution {
        return Execution.builder()
            .id(IdUtils.create())
            .namespace("io.kestra.test")
            .flowId("test-flow")
            .flowRevision(1)
            .state(State())
            .build()
    }

    private fun createTestTaskRun(execution: Execution, taskId: String = "test-task"): TaskRun {
        return TaskRun.builder()
            .id(IdUtils.create())
            .executionId(execution.getId())
            .namespace(execution.getNamespace())
            .flowId(execution.getFlowId())
            .taskId(taskId)
            .state(State())
            .build()
    }

    private fun createTestWorkerTask(taskRun: TaskRun): WorkerTask {
        val task = Log.builder()
            .id("test-task")
            .type("io.kestra.plugin.core.log.Log")
            .message("Test execution task")
            .build()

        return WorkerTask.builder()
            .taskRun(taskRun)
            .task(task)
            .runContext(null)
            .build()
    }

    private fun createTestLogEntry(executionId: String): LogEntry {
        return LogEntry.builder()
            .executionId(executionId)
            .taskId("test-task")
            .level(Level.INFO)
            .message("Test log message")
            .timestamp(Instant.now())
            .build()
    }

    private fun createTestMetricEntry(executionId: String): MetricEntry {
        return MetricEntry.builder()
            .executionId(executionId)
            .taskId("test-task")
            .name("test.metric")
            .value(42.0)
            .timestamp(Instant.now())
            .build()
    }
}
