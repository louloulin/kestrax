<template>
    <div class="enterprise-data-table">
        <el-table
            :data="data"
            :loading="loading"
            v-bind="$attrs"
            @selection-change="handleSelectionChange"
            @sort-change="handleSortChange"
            style="width: 100%"
        >
            <slot />
        </el-table>

        <div v-if="showPagination" class="table-pagination">
            <el-pagination
                v-model:current-page="currentPage"
                v-model:page-size="pageSize"
                :page-sizes="pageSizes"
                :total="total"
                layout="total, sizes, prev, pager, next, jumper"
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"
            />
        </div>
    </div>
</template>

<script setup lang="ts">
    import {ref, watch} from "vue"

    interface Props {
        data: any[]
        loading?: boolean
        total?: number
        currentPage?: number
        pageSize?: number
        pageSizes?: number[]
        showPagination?: boolean
    }

    const props = withDefaults(defineProps<Props>(), {
        loading: false,
        total: 0,
        currentPage: 1,
        pageSize: 20,
        pageSizes: () => [10, 20, 50, 100],
        showPagination: true
    })

    const emit = defineEmits<{
        "selection-change": [selection: any[]]
        "sort-change": [sort: any]
        "page-change": [page: number]
        "size-change": [size: number]
    }>()

    const currentPage = ref(props.currentPage)
    const pageSize = ref(props.pageSize)

    const handleSelectionChange = (selection: any[]) => {
        emit("selection-change", selection)
    }

    const handleSortChange = (sort: any) => {
        emit("sort-change", sort)
    }

    const handleCurrentChange = (page: number) => {
        currentPage.value = page
        emit("page-change", page)
    }

    const handleSizeChange = (size: number) => {
        pageSize.value = size
        currentPage.value = 1
        emit("size-change", size)
    }

    watch(() => props.currentPage, (newVal) => {
        currentPage.value = newVal
    })

    watch(() => props.pageSize, (newVal) => {
        pageSize.value = newVal
    })
</script>

<style scoped>
.enterprise-data-table {
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.table-pagination {
    padding: 1rem;
    display: flex;
    justify-content: flex-end;
    border-top: 1px solid #e4e7ed;
}

:deep(.el-table) {
    border-radius: 8px 8px 0 0;
}

:deep(.el-table__header) {
    background-color: #f8f9fa;
}

:deep(.el-table th) {
    background-color: #f8f9fa;
    color: #303133;
    font-weight: 600;
}

:deep(.el-table td) {
    border-bottom: 1px solid #f0f0f0;
}

:deep(.el-table__row:hover) {
    background-color: #f5f7fa;
}
</style>
