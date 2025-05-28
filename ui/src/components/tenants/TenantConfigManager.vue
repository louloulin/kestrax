<template>
    <div class="tenant-config-manager">
        <!-- Header -->
        <div class="config-header">
            <h2 class="config-title">
                <i class="fas fa-cog" />
                Tenant Configuration
            </h2>
            <div class="config-actions">
                <button
                    class="btn btn-outline-secondary"
                    @click="refreshConfigs"
                    :disabled="loading"
                >
                    <i class="fas fa-sync-alt" :class="{'fa-spin': loading}" />
                    Refresh
                </button>
                <button
                    class="btn btn-primary"
                    @click="showAddConfigModal = true"
                >
                    <i class="fas fa-plus" />
                    Add Configuration
                </button>
                <button
                    class="btn btn-warning"
                    @click="resetToDefaults"
                    :disabled="loading"
                >
                    <i class="fas fa-undo" />
                    Reset to Defaults
                </button>
            </div>
        </div>

        <!-- Search and Filter -->
        <div class="config-filters">
            <div class="row">
                <div class="col-md-6">
                    <div class="input-group">
                        <span class="input-group-text">
                            <i class="fas fa-search" />
                        </span>
                        <input
                            type="text"
                            class="form-control"
                            placeholder="Search configurations..."
                            v-model="searchQuery"
                            @input="searchConfigs"
                        >
                    </div>
                </div>
                <div class="col-md-6">
                    <select
                        class="form-select"
                        v-model="selectedCategory"
                        @change="filterByCategory"
                    >
                        <option value="">
                            All Categories
                        </option>
                        <option
                            v-for="category in categories"
                            :key="category"
                            :value="category"
                        >
                            {{ formatCategoryName(category) }}
                            <span v-if="categoryCounts[category]">({{ categoryCounts[category] }})</span>
                        </option>
                    </select>
                </div>
            </div>
        </div>

        <!-- Configuration Categories -->
        <div class="config-categories" v-if="!searchQuery && !selectedCategory">
            <div class="row">
                <div
                    class="col-md-4 col-lg-3 mb-3"
                    v-for="category in categories"
                    :key="category"
                >
                    <div
                        class="category-card"
                        @click="selectedCategory = category; filterByCategory()"
                    >
                        <div class="category-icon">
                            <i :class="getCategoryIcon(category)" />
                        </div>
                        <div class="category-info">
                            <h5>{{ formatCategoryName(category) }}</h5>
                            <p class="text-muted">
                                {{ categoryCounts[category] || 0 }} configurations
                            </p>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Configuration List -->
        <div class="config-list" v-if="searchQuery || selectedCategory">
            <div class="config-breadcrumb" v-if="selectedCategory">
                <nav aria-label="breadcrumb">
                    <ol class="breadcrumb">
                        <li class="breadcrumb-item">
                            <a href="#" @click.prevent="clearFilters">All Configurations</a>
                        </li>
                        <li class="breadcrumb-item active">
                            {{ formatCategoryName(selectedCategory) }}
                        </li>
                    </ol>
                </nav>
            </div>

            <div class="table-responsive">
                <table class="table table-hover">
                    <thead>
                        <tr>
                            <th>Key</th>
                            <th>Value</th>
                            <th>Type</th>
                            <th>Category</th>
                            <th>Description</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="config in filteredConfigs" :key="config.key">
                            <td>
                                <code>{{ config.key }}</code>
                                <span v-if="config.required" class="badge bg-danger ms-1">Required</span>
                                <span v-if="config.sensitive" class="badge bg-warning ms-1">Sensitive</span>
                                <span v-if="config.readOnly" class="badge bg-secondary ms-1">Read-only</span>
                            </td>
                            <td>
                                <span v-if="config.sensitive" class="text-muted">
                                    <i class="fas fa-eye-slash" /> Hidden
                                </span>
                                <span v-else-if="config.type === 'BOOLEAN'">
                                    <i
                                        :class="config.value ? 'fas fa-check text-success' : 'fas fa-times text-danger'"
                                    />
                                    {{ config.value }}
                                </span>
                                <code v-else-if="config.value !== null">{{ formatConfigValue(config.value) }}</code>
                                <span v-else class="text-muted">
                                    <i>Default: {{ formatConfigValue(config.defaultValue) }}</i>
                                </span>
                            </td>
                            <td>
                                <span class="badge" :class="getTypeBadgeClass(config.type)">
                                    {{ config.type }}
                                </span>
                            </td>
                            <td>{{ formatCategoryName(config.category) }}</td>
                            <td>
                                <span class="text-muted">{{ config.description || 'No description' }}</span>
                            </td>
                            <td>
                                <div class="btn-group btn-group-sm">
                                    <button
                                        class="btn btn-outline-primary"
                                        @click="editConfig(config)"
                                        :disabled="config.readOnly"
                                    >
                                        <i class="fas fa-edit" />
                                    </button>
                                    <button
                                        class="btn btn-outline-danger"
                                        @click="deleteConfig(config)"
                                        :disabled="config.readOnly || config.required"
                                    >
                                        <i class="fas fa-trash" />
                                    </button>
                                </div>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <div v-if="filteredConfigs.length === 0" class="text-center py-4">
                <i class="fas fa-search fa-3x text-muted mb-3" />
                <p class="text-muted">
                    No configurations found
                </p>
            </div>
        </div>

        <!-- Add/Edit Configuration Modal -->
        <div
            class="modal fade"
            :class="{show: showAddConfigModal || showEditConfigModal}"
            :style="{display: showAddConfigModal || showEditConfigModal ? 'block' : 'none'}"
            tabindex="-1"
        >
            <div class="modal-dialog modal-lg">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">
                            {{ showEditConfigModal ? 'Edit Configuration' : 'Add Configuration' }}
                        </h5>
                        <button
                            type="button"
                            class="btn-close"
                            @click="closeModal"
                        />
                    </div>
                    <div class="modal-body">
                        <form @submit.prevent="saveConfig">
                            <div class="mb-3">
                                <label class="form-label">Key *</label>
                                <input
                                    type="text"
                                    class="form-control"
                                    v-model="configForm.key"
                                    :disabled="showEditConfigModal"
                                    placeholder="e.g., app.timeout"
                                    required
                                >
                                <div class="form-text">
                                    Use lowercase letters, numbers, hyphens, dots, and underscores
                                </div>
                            </div>

                            <div class="row">
                                <div class="col-md-6">
                                    <div class="mb-3">
                                        <label class="form-label">Type *</label>
                                        <select
                                            class="form-select"
                                            v-model="configForm.type"
                                            required
                                        >
                                            <option value="">
                                                Select type...
                                            </option>
                                            <option
                                                v-for="type in configTypes"
                                                :key="type"
                                                :value="type"
                                            >
                                                {{ type }}
                                            </option>
                                        </select>
                                    </div>
                                </div>
                                <div class="col-md-6">
                                    <div class="mb-3">
                                        <label class="form-label">Category *</label>
                                        <select
                                            class="form-select"
                                            v-model="configForm.category"
                                            required
                                        >
                                            <option value="">
                                                Select category...
                                            </option>
                                            <option
                                                v-for="category in categories"
                                                :key="category"
                                                :value="category"
                                            >
                                                {{ formatCategoryName(category) }}
                                            </option>
                                        </select>
                                    </div>
                                </div>
                            </div>

                            <div class="mb-3">
                                <label class="form-label">Value *</label>
                                <input
                                    v-if="configForm.type === 'BOOLEAN'"
                                    type="checkbox"
                                    class="form-check-input"
                                    v-model="configForm.value"
                                >
                                <input
                                    v-else-if="configForm.type === 'INTEGER' || configForm.type === 'LONG'"
                                    type="number"
                                    class="form-control"
                                    v-model.number="configForm.value"
                                    required
                                >
                                <input
                                    v-else-if="configForm.type === 'DOUBLE'"
                                    type="number"
                                    step="any"
                                    class="form-control"
                                    v-model.number="configForm.value"
                                    required
                                >
                                <input
                                    v-else-if="configForm.type === 'SECRET'"
                                    type="password"
                                    class="form-control"
                                    v-model="configForm.value"
                                    required
                                >
                                <textarea
                                    v-else-if="configForm.type === 'JSON' || configForm.type === 'OBJECT'"
                                    class="form-control"
                                    rows="4"
                                    v-model="configForm.value"
                                    placeholder="Enter JSON..."
                                    required
                                />
                                <input
                                    v-else
                                    type="text"
                                    class="form-control"
                                    v-model="configForm.value"
                                    required
                                >
                            </div>

                            <div class="mb-3">
                                <label class="form-label">Description</label>
                                <textarea
                                    class="form-control"
                                    rows="2"
                                    v-model="configForm.description"
                                    placeholder="Optional description..."
                                />
                            </div>

                            <div class="row">
                                <div class="col-md-4">
                                    <div class="form-check">
                                        <input
                                            type="checkbox"
                                            class="form-check-input"
                                            v-model="configForm.required"
                                        >
                                        <label class="form-check-label">Required</label>
                                    </div>
                                </div>
                                <div class="col-md-4">
                                    <div class="form-check">
                                        <input
                                            type="checkbox"
                                            class="form-check-input"
                                            v-model="configForm.sensitive"
                                        >
                                        <label class="form-check-label">Sensitive</label>
                                    </div>
                                </div>
                                <div class="col-md-4">
                                    <div class="form-check">
                                        <input
                                            type="checkbox"
                                            class="form-check-input"
                                            v-model="configForm.readOnly"
                                        >
                                        <label class="form-check-label">Read-only</label>
                                    </div>
                                </div>
                            </div>
                        </form>
                    </div>
                    <div class="modal-footer">
                        <button
                            type="button"
                            class="btn btn-secondary"
                            @click="closeModal"
                        >
                            Cancel
                        </button>
                        <button
                            type="button"
                            class="btn btn-primary"
                            @click="saveConfig"
                            :disabled="saving"
                        >
                            <i v-if="saving" class="fas fa-spinner fa-spin me-1" />
                            {{ showEditConfigModal ? 'Update' : 'Create' }}
                        </button>
                    </div>
                </div>
            </div>
        </div>

        <!-- Modal Backdrop -->
        <div
            v-if="showAddConfigModal || showEditConfigModal"
            class="modal-backdrop fade show"
            @click="closeModal"
        />
    </div>
</template>

<script>
    import {ref, reactive, onMounted, computed} from "vue"
    import {useToast} from "@/composables/toast"
    import {tenantConfigApi} from "@/api/tenantConfig"

    export default {
        name: "TenantConfigManager",
        setup() {
            const {showToast} = useToast()

            // Reactive state
            const loading = ref(false)
            const saving = ref(false)
            const configs = ref([])
            const categoryCounts = ref({})
            const configTypes = ref([])
            const categories = ref([])
            const searchQuery = ref("")
            const selectedCategory = ref("")
            const showAddConfigModal = ref(false)
            const showEditConfigModal = ref(false)

            // Form state
            const configForm = reactive({
                key: "",
                value: "",
                type: "",
                category: "",
                description: "",
                required: false,
                sensitive: false,
                readOnly: false
            })

            // Computed properties
            const filteredConfigs = computed(() => {
                let filtered = configs.value

                if (selectedCategory.value) {
                    filtered = filtered.filter(config => config.category === selectedCategory.value)
                }

                if (searchQuery.value) {
                    const query = searchQuery.value.toLowerCase()
                    filtered = filtered.filter(config =>
                        config.key.toLowerCase().includes(query) ||
                        (config.description && config.description.toLowerCase().includes(query))
                    )
                }

                return filtered
            })

            // Methods
            const loadConfigs = async () => {
                loading.value = true
                try {
                    const response = await tenantConfigApi.getAllConfigs()
                    configs.value = response.data
                } catch (error) {
                    console.error("Error loading configurations:", error)
                    showToast("Error loading configurations", "error")
                } finally {
                    loading.value = false
                }
            }

            const loadCategoryCounts = async () => {
                try {
                    const response = await tenantConfigApi.getCategoryCounts()
                    categoryCounts.value = response.data
                } catch (error) {
                    console.error("Error loading category counts:", error)
                }
            }

            const loadSchema = async () => {
                try {
                    const response = await tenantConfigApi.getSchema()
                    configTypes.value = response.data.types
                    categories.value = response.data.categories
                } catch (error) {
                    console.error("Error loading schema:", error)
                }
            }

            const refreshConfigs = async () => {
                await Promise.all([
                    loadConfigs(),
                    loadCategoryCounts()
                ])
            }

            const searchConfigs = async () => {
                if (searchQuery.value.length >= 2) {
                    loading.value = true
                    try {
                        const response = await tenantConfigApi.searchConfigs(searchQuery.value)
                        configs.value = response.data
                    } catch (error) {
                        console.error("Error searching configurations:", error)
                    } finally {
                        loading.value = false
                    }
                } else if (searchQuery.value.length === 0) {
                    await loadConfigs()
                }
            }

            const filterByCategory = async () => {
                if (selectedCategory.value) {
                    loading.value = true
                    try {
                        const response = await tenantConfigApi.getConfigsByCategory(selectedCategory.value)
                        configs.value = response.data
                    } catch (error) {
                        console.error("Error filtering by category:", error)
                    } finally {
                        loading.value = false
                    }
                } else {
                    await loadConfigs()
                }
            }

            const clearFilters = () => {
                searchQuery.value = ""
                selectedCategory.value = ""
                loadConfigs()
            }

            const editConfig = (config) => {
                configForm.key = config.key
                configForm.value = config.value
                configForm.type = config.type
                configForm.category = config.category
                configForm.description = config.description || ""
                configForm.required = config.required || false
                configForm.sensitive = config.sensitive || false
                configForm.readOnly = config.readOnly || false
                showEditConfigModal.value = true
            }

            const saveConfig = async () => {
                saving.value = true
                try {
                    const payload = {
                        value: configForm.value,
                        type: configForm.type,
                        category: configForm.category,
                        description: configForm.description,
                        required: configForm.required,
                        sensitive: configForm.sensitive,
                        readOnly: configForm.readOnly
                    }

                    await tenantConfigApi.setConfig(configForm.key, payload)

                    showToast(
                        showEditConfigModal.value ? "Configuration updated successfully" : "Configuration created successfully",
                        "success"
                    )

                    closeModal()
                    await refreshConfigs()
                } catch (error) {
                    console.error("Error saving configuration:", error)
                    showToast("Error saving configuration", "error")
                } finally {
                    saving.value = false
                }
            }

            const deleteConfig = async (config) => {
                if (confirm(`Are you sure you want to delete configuration "${config.key}"?`)) {
                    try {
                        await tenantConfigApi.deleteConfig(config.key)
                        showToast("Configuration deleted successfully", "success")
                        await refreshConfigs()
                    } catch (error) {
                        console.error("Error deleting configuration:", error)
                        showToast("Error deleting configuration", "error")
                    }
                }
            }

            const resetToDefaults = async () => {
                if (confirm("Are you sure you want to reset all configurations to defaults? This action cannot be undone.")) {
                    try {
                        await tenantConfigApi.resetToDefaults()
                        showToast("Configurations reset to defaults successfully", "success")
                        await refreshConfigs()
                    } catch (error) {
                        console.error("Error resetting configurations:", error)
                        showToast("Error resetting configurations", "error")
                    }
                }
            }

            const closeModal = () => {
                showAddConfigModal.value = false
                showEditConfigModal.value = false

                // Reset form
                Object.assign(configForm, {
                    key: "",
                    value: "",
                    type: "",
                    category: "",
                    description: "",
                    required: false,
                    sensitive: false,
                    readOnly: false
                })
            }

            // Utility methods
            const formatCategoryName = (category) => {
                return category.replace(/_/g, " ").replace(/\b\w/g, l => l.toUpperCase())
            }

            const formatConfigValue = (value) => {
                if (value === null || value === undefined) return "null"
                if (typeof value === "object") return JSON.stringify(value, null, 2)
                return String(value)
            }

            const getCategoryIcon = (category) => {
                const icons = {
                    GENERAL: "fas fa-cog",
                    SECURITY: "fas fa-shield-alt",
                    STORAGE: "fas fa-database",
                    NOTIFICATION: "fas fa-bell",
                    INTEGRATION: "fas fa-plug",
                    PERFORMANCE: "fas fa-tachometer-alt",
                    UI: "fas fa-palette",
                    WORKFLOW: "fas fa-project-diagram",
                    MONITORING: "fas fa-chart-line",
                    CUSTOM: "fas fa-wrench"
                }
                return icons[category] || "fas fa-cog"
            }

            const getTypeBadgeClass = (type) => {
                const classes = {
                    STRING: "bg-primary",
                    INTEGER: "bg-info",
                    LONG: "bg-info",
                    DOUBLE: "bg-info",
                    BOOLEAN: "bg-success",
                    JSON: "bg-warning",
                    ARRAY: "bg-warning",
                    OBJECT: "bg-warning",
                    SECRET: "bg-danger",
                    URL: "bg-secondary",
                    EMAIL: "bg-secondary",
                    DURATION: "bg-dark",
                    CRON: "bg-dark",
                    REGEX: "bg-dark"
                }
                return classes[type] || "bg-secondary"
            }

            // Lifecycle
            onMounted(async () => {
                await Promise.all([
                    loadConfigs(),
                    loadCategoryCounts(),
                    loadSchema()
                ])
            })

            return {
                // State
                loading,
                saving,
                configs,
                categoryCounts,
                configTypes,
                categories,
                searchQuery,
                selectedCategory,
                showAddConfigModal,
                showEditConfigModal,
                configForm,

                // Computed
                filteredConfigs,

                // Methods
                refreshConfigs,
                searchConfigs,
                filterByCategory,
                clearFilters,
                editConfig,
                saveConfig,
                deleteConfig,
                resetToDefaults,
                closeModal,
                formatCategoryName,
                formatConfigValue,
                getCategoryIcon,
                getTypeBadgeClass
            }
        }
    }
</script>

<style scoped>
.tenant-config-manager {
  padding: 1.5rem;
}

.config-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #dee2e6;
}

.config-title {
  margin: 0;
  color: #495057;
  font-weight: 600;
}

.config-title i {
  margin-right: 0.5rem;
  color: #6c757d;
}

.config-actions {
  display: flex;
  gap: 0.5rem;
}

.config-filters {
  margin-bottom: 2rem;
}

.config-categories {
  margin-bottom: 2rem;
}

.category-card {
  background: #fff;
  border: 1px solid #dee2e6;
  border-radius: 0.5rem;
  padding: 1.5rem;
  cursor: pointer;
  transition: all 0.2s ease;
  height: 100%;
  display: flex;
  align-items: center;
}

.category-card:hover {
  border-color: #007bff;
  box-shadow: 0 0.125rem 0.25rem rgba(0, 123, 255, 0.075);
  transform: translateY(-1px);
}

.category-icon {
  font-size: 2rem;
  color: #007bff;
  margin-right: 1rem;
  min-width: 3rem;
  text-align: center;
}

.category-info h5 {
  margin: 0 0 0.25rem 0;
  font-weight: 600;
  color: #495057;
}

.category-info p {
  margin: 0;
  font-size: 0.875rem;
}

.config-list {
  background: #fff;
  border-radius: 0.5rem;
  border: 1px solid #dee2e6;
  overflow: hidden;
}

.config-breadcrumb {
  padding: 1rem 1.5rem;
  background: #f8f9fa;
  border-bottom: 1px solid #dee2e6;
}

.config-breadcrumb .breadcrumb {
  margin: 0;
  background: none;
  padding: 0;
}

.table {
  margin: 0;
}

.table th {
  background: #f8f9fa;
  border-top: none;
  font-weight: 600;
  color: #495057;
}

.table td {
  vertical-align: middle;
}

.table code {
  background: #f8f9fa;
  color: #e83e8c;
  padding: 0.125rem 0.25rem;
  border-radius: 0.25rem;
  font-size: 0.875rem;
}

.badge {
  font-size: 0.75rem;
}

.modal.show {
  display: block !important;
}

.modal-backdrop.show {
  opacity: 0.5;
}

.form-check-input:checked {
  background-color: #007bff;
  border-color: #007bff;
}

.btn-group-sm .btn {
  padding: 0.25rem 0.5rem;
  font-size: 0.875rem;
}

@media (max-width: 768px) {
  .config-header {
    flex-direction: column;
    align-items: stretch;
    gap: 1rem;
  }

  .config-actions {
    justify-content: center;
  }

  .category-card {
    text-align: center;
    flex-direction: column;
  }

  .category-icon {
    margin-right: 0;
    margin-bottom: 0.5rem;
  }

  .table-responsive {
    font-size: 0.875rem;
  }
}
</style>
