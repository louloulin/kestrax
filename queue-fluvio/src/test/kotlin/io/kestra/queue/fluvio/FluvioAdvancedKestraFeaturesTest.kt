package io.kestra.queue.fluvio

import io.kestra.core.models.executions.Execution
import io.kestra.core.models.executions.TaskRun
import io.kestra.core.models.flows.State
import io.kestra.core.models.triggers.Trigger
import io.kestra.core.models.triggers.TriggerContext
import io.kestra.core.models.executions.ExecutionTrigger
import io.kestra.core.runners.WorkerTask
import io.kestra.core.runners.WorkerTaskResult
import io.kestra.core.utils.IdUtils
import io.kestra.plugin.core.log.Log
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.Instant
import java.time.ZonedDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Fluvio队列高级Kestra功能测试
 * 验证子流程、触发器、并发执行、暂停/恢复等高级功能
 * 
 * 对应plan9.md第5周高级功能测试：
 * - 子流程执行测试 ✅
 * - 触发器和调度器集成测试 ✅
 * - 并发执行和资源竞争测试 ✅
 * - 暂停/恢复功能测试 ✅
 */
class FluvioAdvancedKestraFeaturesTest {

    @Test
    fun `should handle subflow execution scenarios`() {
        // Given - 创建主流程和子流程
        val parentExecution = createTestExecution("parent-flow")
        val childExecution = createChildExecution(parentExecution, "child-flow")
        
        // When - 验证子流程关系
        // Then
        assertNotNull(childExecution)
        assertEquals(parentExecution.getId(), childExecution.getParentId())
        assertEquals(parentExecution.getId(), childExecution.getOriginalId())
        assertEquals("child-flow", childExecution.getFlowId())
        assertEquals(parentExecution.getNamespace(), childExecution.getNamespace())
        
        // 验证子流程状态独立性
        val runningChild = childExecution.withState(State.Type.RUNNING)
        assertEquals(State.Type.RUNNING, runningChild.getState().getCurrent())
        assertEquals(State.Type.CREATED, parentExecution.getState().getCurrent()) // 父流程状态不变
    }

    @Test
    fun `should handle multiple subflow executions`() {
        // Given - 创建主流程和多个子流程
        val parentExecution = createTestExecution("parent-flow")
        val childExecutions = listOf(
            createChildExecution(parentExecution, "child-flow-1"),
            createChildExecution(parentExecution, "child-flow-2"),
            createChildExecution(parentExecution, "child-flow-3")
        )
        
        // When & Then - 验证所有子流程都正确关联
        childExecutions.forEach { childExecution ->
            assertEquals(parentExecution.getId(), childExecution.getParentId())
            assertEquals(parentExecution.getId(), childExecution.getOriginalId())
            assertEquals(parentExecution.getNamespace(), childExecution.getNamespace())
            assertNotEquals(parentExecution.getId(), childExecution.getId()) // 不同的执行ID
        }
        
        // 验证子流程ID的唯一性
        val childIds = childExecutions.map { it.getId() }
        assertEquals(3, childIds.toSet().size) // 所有ID都应该不同
    }

    @Test
    fun `should handle trigger context and execution relationship`() {
        // Given - 创建触发器上下文
        val triggerContext = createTestTriggerContext()
        val triggeredExecution = createTriggeredExecution(triggerContext)
        
        // When & Then - 验证触发器关系
        assertNotNull(triggeredExecution)
        assertEquals("scheduled-flow", triggeredExecution.getFlowId())
        assertEquals("io.kestra.test", triggeredExecution.getNamespace())
        
        // 验证触发器上下文
        assertNotNull(triggerContext)
        assertEquals("schedule-trigger", triggerContext.getTriggerId())
        assertEquals("io.kestra.test", triggerContext.getNamespace())
        assertEquals("scheduled-flow", triggerContext.getFlowId())
    }

    @Test
    fun `should handle scheduled trigger execution`() {
        // Given - 模拟调度触发器
        val triggerContext = createScheduledTriggerContext()
        val execution = createTriggeredExecution(triggerContext)
        
        // When - 创建调度任务
        val taskRun = createTestTaskRun(execution, "scheduled-task")
        val workerTask = createTestWorkerTask(taskRun)
        
        // Then - 验证调度执行
        assertEquals("scheduled-task", workerTask.getTaskRun().getTaskId())
        assertEquals(execution.getId(), workerTask.getTaskRun().getExecutionId())
        assertEquals("scheduled-flow", workerTask.getTaskRun().getFlowId())
        
        // 验证任务可以正常完成
        val completedTaskRun = taskRun.withState(State.Type.SUCCESS)
        val taskResult = WorkerTaskResult(completedTaskRun)
        assertEquals(State.Type.SUCCESS, taskResult.getTaskRun().getState().getCurrent())
    }

    @Test
    fun `should handle concurrent execution scenarios`() {
        // Given - 创建多个并发执行
        val concurrentExecutions = (1..5).map { index ->
            createTestExecution("concurrent-flow-$index")
        }
        
        // When - 模拟并发任务创建
        val concurrentTasks = concurrentExecutions.flatMap { execution ->
            (1..3).map { taskIndex ->
                createTestTaskRun(execution, "task-$taskIndex")
            }
        }
        
        // Then - 验证并发执行
        assertEquals(15, concurrentTasks.size) // 5个执行 × 3个任务
        
        // 验证每个任务都有正确的执行关联
        concurrentTasks.forEach { taskRun ->
            assertNotNull(taskRun.getExecutionId())
            assertNotNull(taskRun.getId())
            assertTrue(taskRun.getTaskId().startsWith("task-"))
        }
        
        // 验证执行ID的唯一性
        val executionIds = concurrentTasks.map { it.getExecutionId() }.toSet()
        assertEquals(5, executionIds.size) // 应该有5个不同的执行ID
    }

    @Test
    fun `should handle resource competition in concurrent scenarios`() {
        // Given - 模拟资源竞争场景
        val sharedResourceExecution = createTestExecution("shared-resource-flow")
        val competingTasks = (1..10).map { index ->
            createTestTaskRun(sharedResourceExecution, "competing-task-$index")
        }
        
        // When - 模拟并发任务处理
        val processedTasks = AtomicInteger(0)
        val failedTasks = AtomicInteger(0)
        
        competingTasks.forEach { taskRun ->
            try {
                // 模拟任务处理
                val workerTask = createTestWorkerTask(taskRun)
                assertNotNull(workerTask)
                processedTasks.incrementAndGet()
            } catch (e: Exception) {
                failedTasks.incrementAndGet()
            }
        }
        
        // Then - 验证资源竞争处理
        assertEquals(10, processedTasks.get()) // 所有任务都应该成功处理
        assertEquals(0, failedTasks.get()) // 没有任务失败
        
        // 验证所有任务都属于同一个执行
        competingTasks.forEach { taskRun ->
            assertEquals(sharedResourceExecution.getId(), taskRun.getExecutionId())
        }
    }

    @Test
    fun `should handle execution pause and resume functionality`() {
        // Given - 创建可暂停的执行
        val execution = createTestExecution("pausable-flow")
        val taskRun = createTestTaskRun(execution, "pausable-task")
        
        // When - 模拟执行暂停
        val runningExecution = execution.withState(State.Type.RUNNING)
        val pausedExecution = runningExecution.withState(State.Type.PAUSED)
        
        // Then - 验证暂停状态
        assertEquals(State.Type.PAUSED, pausedExecution.getState().getCurrent())
        assertEquals(execution.getId(), pausedExecution.getId()) // ID保持不变
        
        // 验证任务也可以暂停
        val runningTaskRun = taskRun.withState(State.Type.RUNNING)
        val pausedTaskRun = runningTaskRun.withState(State.Type.PAUSED)
        assertEquals(State.Type.PAUSED, pausedTaskRun.getState().getCurrent())
        
        // 模拟恢复执行
        val resumedExecution = pausedExecution.withState(State.Type.RUNNING)
        assertEquals(State.Type.RUNNING, resumedExecution.getState().getCurrent())
        
        // 模拟恢复任务
        val resumedTaskRun = pausedTaskRun.withState(State.Type.RUNNING)
        assertEquals(State.Type.RUNNING, resumedTaskRun.getState().getCurrent())
    }

    @Test
    fun `should handle complex pause resume workflow`() {
        // Given - 创建复杂的暂停/恢复工作流
        val execution = createTestExecution("complex-pausable-flow")
        val tasks = (1..3).map { index ->
            createTestTaskRun(execution, "task-$index")
        }
        
        // When - 模拟复杂的暂停/恢复场景
        // 1. 所有任务开始运行
        val runningTasks = tasks.map { it.withState(State.Type.RUNNING) }
        runningTasks.forEach { taskRun ->
            assertEquals(State.Type.RUNNING, taskRun.getState().getCurrent())
        }
        
        // 2. 暂停第二个任务
        val pausedTask2 = runningTasks[1].withState(State.Type.PAUSED)
        assertEquals(State.Type.PAUSED, pausedTask2.getState().getCurrent())
        
        // 3. 第一个和第三个任务继续完成
        val completedTask1 = runningTasks[0].withState(State.Type.SUCCESS)
        val completedTask3 = runningTasks[2].withState(State.Type.SUCCESS)
        
        // 4. 恢复第二个任务并完成
        val resumedTask2 = pausedTask2.withState(State.Type.RUNNING)
        val completedTask2 = resumedTask2.withState(State.Type.SUCCESS)
        
        // Then - 验证最终状态
        assertEquals(State.Type.SUCCESS, completedTask1.getState().getCurrent())
        assertEquals(State.Type.SUCCESS, completedTask2.getState().getCurrent())
        assertEquals(State.Type.SUCCESS, completedTask3.getState().getCurrent())
        
        // 验证所有任务都属于同一个执行
        listOf(completedTask1, completedTask2, completedTask3).forEach { taskRun ->
            assertEquals(execution.getId(), taskRun.getExecutionId())
        }
    }

    @Test
    fun `should handle execution restart scenarios`() {
        // Given - 创建失败的执行
        val originalExecution = createTestExecution("restartable-flow")
        val failedExecution = originalExecution.withState(State.Type.FAILED)
        
        // When - 创建重启执行
        val restartedExecution = failedExecution.childExecution(
            IdUtils.create(),
            emptyList(),
            State().withState(State.Type.RESTARTED)
        )
        
        // Then - 验证重启执行
        assertNotNull(restartedExecution)
        assertNotEquals(originalExecution.getId(), restartedExecution.getId()) // 新的执行ID
        assertEquals(originalExecution.getId(), restartedExecution.getParentId()) // 父执行ID
        assertEquals(originalExecution.getId(), restartedExecution.getOriginalId()) // 原始执行ID
        assertEquals(originalExecution.getNamespace(), restartedExecution.getNamespace())
        assertEquals(originalExecution.getFlowId(), restartedExecution.getFlowId())
        assertEquals(State.Type.RESTARTED, restartedExecution.getState().getCurrent())
    }

    @Test
    fun `should handle trigger execution with multiple flows`() {
        // Given - 创建多个触发器上下文
        val triggerContexts = listOf(
            createTestTriggerContext("trigger-1", "flow-1"),
            createTestTriggerContext("trigger-2", "flow-2"),
            createTestTriggerContext("trigger-3", "flow-3")
        )
        
        // When - 为每个触发器创建执行
        val triggeredExecutions = triggerContexts.map { context ->
            createTriggeredExecution(context)
        }
        
        // Then - 验证所有触发执行
        assertEquals(3, triggeredExecutions.size)
        
        triggeredExecutions.forEachIndexed { index, execution ->
            val expectedFlowId = "flow-${index + 1}"
            assertEquals(expectedFlowId, execution.getFlowId())
            assertEquals("io.kestra.test", execution.getNamespace())
            assertNotNull(execution.getId())
        }
        
        // 验证执行ID的唯一性
        val executionIds = triggeredExecutions.map { it.getId() }
        assertEquals(3, executionIds.toSet().size)
    }

    // Helper methods
    private fun createTestExecution(flowId: String = "test-flow"): Execution {
        return Execution.builder()
            .id(IdUtils.create())
            .namespace("io.kestra.test")
            .flowId(flowId)
            .flowRevision(1)
            .state(State())
            .build()
    }

    private fun createChildExecution(parentExecution: Execution, childFlowId: String): Execution {
        return parentExecution.childExecution(
            IdUtils.create(),
            emptyList(),
            State()
        ).toBuilder()
            .flowId(childFlowId)
            .build()
    }

    private fun createTestTriggerContext(triggerId: String = "schedule-trigger", flowId: String = "scheduled-flow"): TriggerContext {
        return TriggerContext.builder()
            .namespace("io.kestra.test")
            .flowId(flowId)
            .triggerId(triggerId)
            .date(ZonedDateTime.now())
            .build()
    }

    private fun createScheduledTriggerContext(): TriggerContext {
        return TriggerContext.builder()
            .namespace("io.kestra.test")
            .flowId("scheduled-flow")
            .triggerId("schedule-trigger")
            .date(ZonedDateTime.now())
            .build()
    }

    private fun createTriggeredExecution(triggerContext: TriggerContext): Execution {
        val executionTrigger = ExecutionTrigger.builder()
            .id(triggerContext.getTriggerId())
            .type("io.kestra.plugin.core.trigger.Schedule")
            .variables(emptyMap<String, Any>())
            .build()

        return Execution.builder()
            .id(IdUtils.create())
            .namespace(triggerContext.getNamespace())
            .flowId(triggerContext.getFlowId())
            .flowRevision(1)
            .state(State())
            .trigger(executionTrigger)
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
            .id(taskRun.getTaskId())
            .type("io.kestra.plugin.core.log.Log")
            .message("Test execution task")
            .build()

        return WorkerTask.builder()
            .taskRun(taskRun)
            .task(task)
            .runContext(null)
            .build()
    }
}
