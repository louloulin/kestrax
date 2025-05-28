<template>
    <div class="tenant-routing-manager">
        <!-- Header -->
        <div class="routing-header">
            <h2 class="routing-title">
                <i class="fas fa-route" />
                Tenant Routing Management
            </h2>
            <div class="routing-actions">
                <button
                    class="btn btn-outline-secondary"
                    @click="refreshData"
                    :disabled="loading"
                >
                    <i class="fas fa-sync-alt" :class="{'fa-spin': loading}" />
                    Refresh
                </button>
                <button
                    class="btn btn-warning"
                    @click="clearCaches"
                    :disabled="loading"
                >
                    <i class="fas fa-trash" />
                    Clear Caches
                </button>
                <button
                    class="btn btn-primary"
                    @click="showTestModal = true"
                >
                    <i class="fas fa-vial" />
                    Test Routing
                </button>
            </div>
        </div>

        <!-- Status Cards -->
        <div class="status-cards">
            <div class="row">
                <div class="col-md-3">
                    <div class="status-card">
                        <div class="status-icon">
                            <i class="fas fa-toggle-on" :class="{'text-success': stats.enabled, 'text-danger': !stats.enabled}" />
                        </div>
                        <div class="status-info">
                            <h5>Routing Status</h5>
                            <p :class="{'text-success': stats.enabled, 'text-danger': !stats.enabled}">
                                {{ stats.enabled ? 'Enabled' : 'Disabled' }}
                            </p>
                        </div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="status-card">
                        <div class="status-icon">
                            <i class="fas fa-sitemap text-info" />
                        </div>
                        <div class="status-info">
                            <h5>Strategy</h5>
                            <p>{{ formatStrategy(stats.strategy) }}</p>
                        </div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="status-card">
                        <div class="status-icon">
                            <i class="fas fa-shield-alt text-warning" />
                        </div>
                        <div class="status-info">
                            <h5>Mode</h5>
                            <p>{{ formatMode(stats.mode) }}</p>
                        </div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="status-card">
                        <div class="status-icon">
                            <i class="fas fa-memory text-primary" />
                        </div>
                        <div class="status-info">
                            <h5>Cache Size</h5>
                            <p>{{ stats.resolutionCacheSize + stats.validationCacheSize }}</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Configuration Details -->
        <div class="config-section">
            <h3>
                <i class="fas fa-cogs" />
                Configuration Details
            </h3>

            <div class="row">
                <div class="col-md-6">
                    <div class="config-card">
                        <h5>Extraction Settings</h5>
                        <table class="table table-sm">
                            <tbody>
                                <tr>
                                    <td><strong>Strategy:</strong></td>
                                    <td>{{ formatStrategy(config.strategy) }}</td>
                                </tr>
                                <tr>
                                    <td><strong>Mode:</strong></td>
                                    <td>{{ formatMode(config.mode) }}</td>
                                </tr>
                                <tr>
                                    <td><strong>Default Tenant:</strong></td>
                                    <td><code>{{ config.defaultTenant }}</code></td>
                                </tr>
                                <tr>
                                    <td><strong>Tenant Header:</strong></td>
                                    <td><code>{{ config.tenantHeader }}</code></td>
                                </tr>
                                <tr>
                                    <td><strong>Query Parameter:</strong></td>
                                    <td><code>{{ config.tenantQueryParam }}</code></td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>

                <div class="col-md-6">
                    <div class="config-card">
                        <h5>Validation Settings</h5>
                        <table class="table table-sm">
                            <tbody>
                                <tr>
                                    <td><strong>Enabled:</strong></td>
                                    <td>
                                        <i :class="config.validation.enabled ? 'fas fa-check text-success' : 'fas fa-times text-danger'" />
                                        {{ config.validation.enabled ? 'Yes' : 'No' }}
                                    </td>
                                </tr>
                                <tr>
                                    <td><strong>Min Length:</strong></td>
                                    <td>{{ config.validation.minLength }}</td>
                                </tr>
                                <tr>
                                    <td><strong>Max Length:</strong></td>
                                    <td>{{ config.validation.maxLength }}</td>
                                </tr>
                                <tr>
                                    <td><strong>Case Sensitive:</strong></td>
                                    <td>
                                        <i :class="config.validation.caseSensitive ? 'fas fa-check text-success' : 'fas fa-times text-danger'" />
                                        {{ config.validation.caseSensitive ? 'Yes' : 'No' }}
                                    </td>
                                </tr>
                                <tr>
                                    <td><strong>Pattern:</strong></td>
                                    <td><code class="small">{{ config.validation.pattern }}</code></td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>

        <!-- Cache Statistics -->
        <div class="cache-section">
            <h3>
                <i class="fas fa-database" />
                Cache Statistics
            </h3>

            <div class="row">
                <div class="col-md-6">
                    <div class="cache-card">
                        <h5>Resolution Cache</h5>
                        <div class="cache-metric">
                            <span class="metric-value">{{ stats.resolutionCacheSize }}</span>
                            <span class="metric-label">Cached Entries</span>
                        </div>
                        <div class="cache-status">
                            <i class="fas fa-circle" :class="stats.resolutionCacheSize > 0 ? 'text-success' : 'text-muted'" />
                            {{ stats.resolutionCacheSize > 0 ? 'Active' : 'Empty' }}
                        </div>
                    </div>
                </div>

                <div class="col-md-6">
                    <div class="cache-card">
                        <h5>Validation Cache</h5>
                        <div class="cache-metric">
                            <span class="metric-value">{{ stats.validationCacheSize }}</span>
                            <span class="metric-label">Cached Entries</span>
                        </div>
                        <div class="cache-status">
                            <i class="fas fa-circle" :class="stats.validationCacheSize > 0 ? 'text-success' : 'text-muted'" />
                            {{ stats.validationCacheSize > 0 ? 'Active' : 'Empty' }}
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Exempt Paths -->
        <div class="paths-section">
            <h3>
                <i class="fas fa-ban" />
                Exempt Paths ({{ stats.exemptPathsCount }})
            </h3>

            <div class="paths-list">
                <div class="row">
                    <div
                        class="col-md-4 mb-2"
                        v-for="path in config.exemptPaths"
                        :key="path"
                    >
                        <div class="path-item">
                            <code>{{ path }}</code>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Reserved Subdomains -->
        <div class="subdomains-section">
            <h3>
                <i class="fas fa-globe" />
                Reserved Subdomains ({{ stats.reservedSubdomainsCount }})
            </h3>

            <div class="subdomains-list">
                <div class="row">
                    <div
                        class="col-md-3 mb-2"
                        v-for="subdomain in config.reservedSubdomains"
                        :key="subdomain"
                    >
                        <div class="subdomain-item">
                            <code>{{ subdomain }}</code>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Tenant Validation Tool -->
        <div class="validation-section">
            <h3>
                <i class="fas fa-check-circle" />
                Tenant ID Validation
            </h3>

            <div class="validation-tool">
                <div class="input-group">
                    <input
                        type="text"
                        class="form-control"
                        placeholder="Enter tenant ID to validate..."
                        v-model="validationInput"
                        @keyup.enter="validateTenantId"
                    >
                    <button
                        class="btn btn-primary"
                        @click="validateTenantId"
                        :disabled="!validationInput.trim() || validating"
                    >
                        <i v-if="validating" class="fas fa-spinner fa-spin" />
                        <i v-else class="fas fa-check" />
                        Validate
                    </button>
                </div>

                <div v-if="validationResult" class="validation-result mt-3">
                    <div class="alert" :class="validationResult.valid ? 'alert-success' : 'alert-danger'">
                        <div class="d-flex align-items-center">
                            <i :class="validationResult.valid ? 'fas fa-check-circle text-success' : 'fas fa-times-circle text-danger'" />
                            <div class="ms-2">
                                <strong>{{ validationResult.valid ? 'Valid' : 'Invalid' }}</strong>
                                <p class="mb-0">
                                    {{ validationResult.message }}
                                </p>
                                <small v-if="validationResult.normalizedTenantId !== validationResult.originalTenantId">
                                    Normalized: <code>{{ validationResult.normalizedTenantId }}</code>
                                </small>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Test Routing Modal -->
        <div
            class="modal fade"
            :class="{show: showTestModal}"
            :style="{display: showTestModal ? 'block' : 'none'}"
            tabindex="-1"
        >
            <div class="modal-dialog modal-lg">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">
                            Test Tenant Routing
                        </h5>
                        <button
                            type="button"
                            class="btn-close"
                            @click="closeTestModal"
                        />
                    </div>
                    <div class="modal-body">
                        <form @submit.prevent="testRouting">
                            <div class="row">
                                <div class="col-md-6">
                                    <div class="mb-3">
                                        <label class="form-label">HTTP Method</label>
                                        <select class="form-select" v-model="testForm.method">
                                            <option value="GET">
                                                GET
                                            </option>
                                            <option value="POST">
                                                POST
                                            </option>
                                            <option value="PUT">
                                                PUT
                                            </option>
                                            <option value="DELETE">
                                                DELETE
                                            </option>
                                        </select>
                                    </div>
                                </div>
                                <div class="col-md-6">
                                    <div class="mb-3">
                                        <label class="form-label">Path *</label>
                                        <input
                                            type="text"
                                            class="form-control"
                                            v-model="testForm.path"
                                            placeholder="/api/v1/tenant/example/workflows"
                                            required
                                        >
                                    </div>
                                </div>
                            </div>

                            <div class="mb-3">
                                <label class="form-label">Headers</label>
                                <div class="header-inputs">
                                    <div
                                        class="input-group mb-2"
                                        v-for="(header, index) in testForm.headersList"
                                        :key="index"
                                    >
                                        <input
                                            type="text"
                                            class="form-control"
                                            placeholder="Header name"
                                            v-model="header.name"
                                        >
                                        <input
                                            type="text"
                                            class="form-control"
                                            placeholder="Header value"
                                            v-model="header.value"
                                        >
                                        <button
                                            type="button"
                                            class="btn btn-outline-danger"
                                            @click="removeHeader(index)"
                                        >
                                            <i class="fas fa-times" />
                                        </button>
                                    </div>
                                    <button
                                        type="button"
                                        class="btn btn-outline-secondary btn-sm"
                                        @click="addHeader"
                                    >
                                        <i class="fas fa-plus" />
                                        Add Header
                                    </button>
                                </div>
                            </div>

                            <div class="mb-3">
                                <label class="form-label">Query Parameters</label>
                                <div class="param-inputs">
                                    <div
                                        class="input-group mb-2"
                                        v-for="(param, index) in testForm.paramsList"
                                        :key="index"
                                    >
                                        <input
                                            type="text"
                                            class="form-control"
                                            placeholder="Parameter name"
                                            v-model="param.name"
                                        >
                                        <input
                                            type="text"
                                            class="form-control"
                                            placeholder="Parameter value"
                                            v-model="param.value"
                                        >
                                        <button
                                            type="button"
                                            class="btn btn-outline-danger"
                                            @click="removeParam(index)"
                                        >
                                            <i class="fas fa-times" />
                                        </button>
                                    </div>
                                    <button
                                        type="button"
                                        class="btn btn-outline-secondary btn-sm"
                                        @click="addParam"
                                    >
                                        <i class="fas fa-plus" />
                                        Add Parameter
                                    </button>
                                </div>
                            </div>
                        </form>

                        <!-- Test Result -->
                        <div v-if="testResult" class="test-result mt-4">
                            <h6>Test Result</h6>
                            <div class="alert" :class="testResult.allowed ? 'alert-success' : 'alert-danger'">
                                <div class="row">
                                    <div class="col-md-6">
                                        <strong>Status:</strong>
                                        <span :class="testResult.allowed ? 'text-success' : 'text-danger'">
                                            {{ testResult.allowed ? 'Allowed' : 'Blocked' }}
                                        </span>
                                    </div>
                                    <div class="col-md-6">
                                        <strong>Resolved Tenant:</strong>
                                        <code>{{ testResult.resolvedTenantId || 'None' }}</code>
                                    </div>
                                </div>
                                <div class="row mt-2">
                                    <div class="col-md-6">
                                        <strong>Required:</strong>
                                        {{ testResult.required ? 'Yes' : 'No' }}
                                    </div>
                                    <div class="col-md-6">
                                        <strong>Reason:</strong>
                                        {{ testResult.reason }}
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button
                            type="button"
                            class="btn btn-secondary"
                            @click="closeTestModal"
                        >
                            Close
                        </button>
                        <button
                            type="button"
                            class="btn btn-primary"
                            @click="testRouting"
                            :disabled="testing || !testForm.path"
                        >
                            <i v-if="testing" class="fas fa-spinner fa-spin me-1" />
                            Test Routing
                        </button>
                    </div>
                </div>
            </div>
        </div>

        <!-- Modal Backdrop -->
        <div
            v-if="showTestModal"
            class="modal-backdrop fade show"
            @click="closeTestModal"
        />
    </div>
</template>

<script>
    import {ref, reactive, onMounted} from "vue"
    import {useToast} from "@/composables/toast"
    import {tenantRoutingApi} from "@/api/tenantRouting"

    export default {
        name: "TenantRoutingManager",
        setup() {
            const {showToast} = useToast()

            // Reactive state
            const loading = ref(false)
            const validating = ref(false)
            const testing = ref(false)
            const config = ref({})
            const stats = ref({})
            const validationInput = ref("")
            const validationResult = ref(null)
            const showTestModal = ref(false)
            const testResult = ref(null)

            // Test form state
            const testForm = reactive({
                method: "GET",
                path: "",
                headersList: [
                    {name: "X-Tenant-ID", value: ""}
                ],
                paramsList: [
                    {name: "tenant", value: ""}
                ]
            })

            // Methods
            const loadConfig = async () => {
                try {
                    const response = await tenantRoutingApi.getConfig()
                    config.value = response.data
                } catch (error) {
                    console.error("Error loading routing config:", error)
                    showToast("Error loading routing configuration", "error")
                }
            }

            const loadStats = async () => {
                try {
                    const response = await tenantRoutingApi.getStats()
                    stats.value = response.data
                } catch (error) {
                    console.error("Error loading routing stats:", error)
                    showToast("Error loading routing statistics", "error")
                }
            }

            const refreshData = async () => {
                loading.value = true
                try {
                    await Promise.all([
                        loadConfig(),
                        loadStats()
                    ])
                } finally {
                    loading.value = false
                }
            }

            const clearCaches = async () => {
                if (!confirm("Are you sure you want to clear all routing caches?")) {
                    return
                }

                loading.value = true
                try {
                    await tenantRoutingApi.clearCaches()
                    showToast("Routing caches cleared successfully", "success")
                    await loadStats() // Refresh stats to show updated cache sizes
                } catch (error) {
                    console.error("Error clearing caches:", error)
                    showToast("Error clearing routing caches", "error")
                } finally {
                    loading.value = false
                }
            }

            const validateTenantId = async () => {
                if (!validationInput.value.trim()) {
                    return
                }

                validating.value = true
                try {
                    const response = await tenantRoutingApi.validateTenant(validationInput.value.trim())
                    validationResult.value = response.data
                } catch (error) {
                    console.error("Error validating tenant ID:", error)
                    showToast("Error validating tenant ID", "error")
                } finally {
                    validating.value = false
                }
            }

            const testRouting = async () => {
                testing.value = true
                try {
                    // Convert form data to API format
                    const headers = {}
                    const queryParams = {}

                    testForm.headersList.forEach(header => {
                        if (header.name && header.value) {
                            headers[header.name] = header.value
                        }
                    })

                    testForm.paramsList.forEach(param => {
                        if (param.name && param.value) {
                            queryParams[param.name] = param.value
                        }
                    })

                    const testRequest = {
                        method: testForm.method,
                        path: testForm.path,
                        headers,
                        queryParams
                    }

                    const response = await tenantRoutingApi.testRouting(testRequest)
                    testResult.value = response.data
                } catch (error) {
                    console.error("Error testing routing:", error)
                    showToast("Error testing routing", "error")
                } finally {
                    testing.value = false
                }
            }

            const addHeader = () => {
                testForm.headersList.push({name: "", value: ""})
            }

            const removeHeader = (index) => {
                testForm.headersList.splice(index, 1)
            }

            const addParam = () => {
                testForm.paramsList.push({name: "", value: ""})
            }

            const removeParam = (index) => {
                testForm.paramsList.splice(index, 1)
            }

            const closeTestModal = () => {
                showTestModal.value = false
                testResult.value = null

                // Reset form
                testForm.method = "GET"
                testForm.path = ""
                testForm.headersList = [{name: "X-Tenant-ID", value: ""}]
                testForm.paramsList = [{name: "tenant", value: ""}]
            }

            // Utility methods
            const formatStrategy = (strategy) => {
                if (!strategy) return "Unknown"

                const strategies = {
                    HEADER_FIRST: "Header First",
                    PATH_FIRST: "Path First",
                    SUBDOMAIN_FIRST: "Subdomain First",
                    HEADER_ONLY: "Header Only",
                    PATH_ONLY: "Path Only",
                    SUBDOMAIN_ONLY: "Subdomain Only"
                }

                return strategies[strategy] || strategy
            }

            const formatMode = (mode) => {
                if (!mode) return "Unknown"

                const modes = {
                    STRICT: "Strict",
                    LENIENT: "Lenient",
                    OPTIONAL: "Optional"
                }

                return modes[mode] || mode
            }

            // Lifecycle
            onMounted(async () => {
                await refreshData()
            })

            return {
                // State
                loading,
                validating,
                testing,
                config,
                stats,
                validationInput,
                validationResult,
                showTestModal,
                testResult,
                testForm,

                // Methods
                refreshData,
                clearCaches,
                validateTenantId,
                testRouting,
                addHeader,
                removeHeader,
                addParam,
                removeParam,
                closeTestModal,
                formatStrategy,
                formatMode
            }
        }
    }
</script>

<style scoped>
.tenant-routing-manager {
  padding: 1.5rem;
}

.routing-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #dee2e6;
}

.routing-title {
  margin: 0;
  color: #495057;
  font-weight: 600;
}

.routing-title i {
  margin-right: 0.5rem;
  color: #6c757d;
}

.routing-actions {
  display: flex;
  gap: 0.5rem;
}

.status-cards {
  margin-bottom: 2rem;
}

.status-card {
  background: #fff;
  border: 1px solid #dee2e6;
  border-radius: 0.5rem;
  padding: 1.5rem;
  height: 100%;
  display: flex;
  align-items: center;
  transition: box-shadow 0.2s ease;
}

.status-card:hover {
  box-shadow: 0 0.125rem 0.25rem rgba(0, 0, 0, 0.075);
}

.status-icon {
  font-size: 2rem;
  margin-right: 1rem;
  min-width: 3rem;
  text-align: center;
}

.status-info h5 {
  margin: 0 0 0.25rem 0;
  font-weight: 600;
  color: #495057;
}

.status-info p {
  margin: 0;
  font-size: 0.875rem;
  font-weight: 500;
}

.config-section,
.cache-section,
.paths-section,
.subdomains-section,
.validation-section {
  margin-bottom: 2rem;
}

.config-section h3,
.cache-section h3,
.paths-section h3,
.subdomains-section h3,
.validation-section h3 {
  color: #495057;
  font-weight: 600;
  margin-bottom: 1rem;
}

.config-section h3 i,
.cache-section h3 i,
.paths-section h3 i,
.subdomains-section h3 i,
.validation-section h3 i {
  margin-right: 0.5rem;
  color: #6c757d;
}

.config-card,
.cache-card {
  background: #fff;
  border: 1px solid #dee2e6;
  border-radius: 0.5rem;
  padding: 1.5rem;
  height: 100%;
}

.config-card h5,
.cache-card h5 {
  margin-bottom: 1rem;
  color: #495057;
  font-weight: 600;
}

.cache-metric {
  text-align: center;
  margin-bottom: 1rem;
}

.metric-value {
  display: block;
  font-size: 2rem;
  font-weight: 700;
  color: #007bff;
}

.metric-label {
  display: block;
  font-size: 0.875rem;
  color: #6c757d;
}

.cache-status {
  text-align: center;
  font-size: 0.875rem;
  font-weight: 500;
}

.cache-status i {
  margin-right: 0.25rem;
}

.paths-list,
.subdomains-list {
  background: #f8f9fa;
  border-radius: 0.5rem;
  padding: 1rem;
}

.path-item,
.subdomain-item {
  background: #fff;
  border: 1px solid #dee2e6;
  border-radius: 0.25rem;
  padding: 0.5rem;
  text-align: center;
}

.path-item code,
.subdomain-item code {
  background: none;
  color: #495057;
  font-size: 0.875rem;
}

.validation-tool {
  background: #f8f9fa;
  border-radius: 0.5rem;
  padding: 1.5rem;
}

.validation-result {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

.header-inputs,
.param-inputs {
  max-height: 200px;
  overflow-y: auto;
}

.test-result {
  background: #f8f9fa;
  border-radius: 0.5rem;
  padding: 1rem;
}

.modal.show {
  display: block !important;
}

.modal-backdrop.show {
  opacity: 0.5;
}

.table-sm td {
  padding: 0.5rem;
  vertical-align: middle;
}

.table-sm code {
  background: #f8f9fa;
  color: #e83e8c;
  padding: 0.125rem 0.25rem;
  border-radius: 0.25rem;
  font-size: 0.875rem;
}

@media (max-width: 768px) {
  .routing-header {
    flex-direction: column;
    align-items: stretch;
    gap: 1rem;
  }

  .routing-actions {
    justify-content: center;
  }

  .status-card {
    text-align: center;
    flex-direction: column;
  }

  .status-icon {
    margin-right: 0;
    margin-bottom: 0.5rem;
  }

  .config-card,
  .cache-card {
    margin-bottom: 1rem;
  }
}
</style>
