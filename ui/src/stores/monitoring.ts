// Monitoring Store - Mock implementation for testing
export const useMonitoringStore = () => {
    return {
        fetchSystemMetrics: async () => {
            return {
                cpu: {
                    usage: Math.random() * 100,
                    cores: 8,
                    loadAverage: [1.2, 1.5, 1.8]
                },
                memory: {
                    usage: Math.random() * 100,
                    total: 16 * 1024 * 1024 * 1024, // 16GB
                    available: 8 * 1024 * 1024 * 1024 // 8GB
                },
                disk: {
                    usage: Math.random() * 100,
                    total: 500 * 1024 * 1024 * 1024, // 500GB
                    available: 250 * 1024 * 1024 * 1024 // 250GB
                },
                network: {
                    bytesIn: Math.random() * 1000000,
                    bytesOut: Math.random() * 1000000,
                    packetsIn: Math.random() * 10000,
                    packetsOut: Math.random() * 10000
                }
            };
        },
        
        fetchServiceStatus: async () => {
            return [
                {
                    name: "DataFlare Core",
                    status: "healthy",
                    uptime: "15d 8h 32m",
                    version: "1.0.0",
                    lastCheck: new Date().toISOString()
                },
                {
                    name: "Database",
                    status: "healthy",
                    uptime: "30d 12h 15m",
                    version: "PostgreSQL 14.2",
                    lastCheck: new Date().toISOString()
                },
                {
                    name: "Redis Cache",
                    status: "healthy",
                    uptime: "25d 6h 45m",
                    version: "Redis 7.0",
                    lastCheck: new Date().toISOString()
                },
                {
                    name: "Message Queue",
                    status: "warning",
                    uptime: "10d 2h 20m",
                    version: "RabbitMQ 3.11",
                    lastCheck: new Date().toISOString()
                }
            ];
        },
        
        fetchAlerts: async () => {
            return [
                {
                    id: "1",
                    title: "High CPU Usage",
                    description: "CPU usage has exceeded 80% for the last 10 minutes",
                    severity: "warning",
                    status: "active",
                    createdAt: "2024-01-15T10:20:00Z",
                    service: "DataFlare Core"
                },
                {
                    id: "2",
                    title: "Disk Space Low",
                    description: "Available disk space is below 20%",
                    severity: "critical",
                    status: "active",
                    createdAt: "2024-01-15T09:45:00Z",
                    service: "Database"
                }
            ];
        },
        
        fetchPerformanceMetrics: async (_timeRange: string = "24h") => {
            // Generate mock time series data
            const now = new Date();
            const points = 24; // 24 hours
            const data = [];
            
            for (let i = points; i >= 0; i--) {
                const timestamp = new Date(now.getTime() - i * 60 * 60 * 1000);
                data.push({
                    timestamp: timestamp.toISOString(),
                    cpu: Math.random() * 100,
                    memory: Math.random() * 100,
                    disk: Math.random() * 100,
                    network: Math.random() * 1000
                });
            }
            
            return data;
        },
        
        // Custom Dashboard
        fetchDashboards: async () => {
            return [
                {
                    id: "1",
                    name: "Executive Dashboard",
                    description: "High-level metrics for executives",
                    isDefault: true,
                    layout: [
                        {id: "cpu-widget", type: "metric", x: 0, y: 0, w: 6, h: 4},
                        {id: "memory-widget", type: "metric", x: 6, y: 0, w: 6, h: 4},
                        {id: "performance-chart", type: "chart", x: 0, y: 4, w: 12, h: 8}
                    ],
                    createdAt: "2024-01-01T00:00:00Z"
                }
            ];
        },
        
        saveDashboard: async (dashboard: any) => {
            return {success: true, id: dashboard.id || Date.now().toString()};
        },
        
        deleteDashboard: async (_id: string) => {
            return {success: true};
        }
    };
};
