# Kestra 多租户改造计划

## 当前状态分析

### 登录功能
- **认证机制**: 基于 HTTP Basic 认证
- **核心组件**:
  - `AuthenticationFilter`: 拦截请求并验证用户凭据
  - `BasicAuthService`: 管理基本认证配置（用户名、加盐密码、开放URL）
  - 前端通过 Vuex store 的 `auth` 模块管理用户状态

### 权限功能
- **权限模型**: 基于资源类型和操作的权限系统
  - 资源类型: `FLOW`, `EXECUTION`, `NAMESPACE`, `TEMPLATE` 等
  - 操作类型: `READ`, `CREATE`, `UPDATE`, `DELETE`
- **当前状态**: 权限检查方法存在但未完全实现（大部分返回 `true`）
- **访问控制**: 部分日志和执行相关功能支持访问控制参数

### 多租户功能
- **当前实现**: 
  - 开源版本仅支持单租户（`MAIN_TENANT = "main"`）
  - 数据库已支持多租户架构（所有表都有 `tenant_id` 字段）
  - 存储系统支持租户隔离
  - 企业版（EE）支持真正的多租户功能
- **限制**: OSS版本的 `TenantService.resolveTenant()` 始终返回 "main"

## 改造目标

将 Kestra OSS 版本改造为支持多租户的系统，包括：
1. 完善的用户认证和授权机制
2. 基于角色的访问控制（RBAC）
3. 租户隔离和管理
4. 细粒度的权限控制

## 改造方案

### 阶段一：用户管理和认证增强

#### 1.1 用户模型设计
```java
// 新增用户实体
public class User {
    private String id;
    private String username;
    private String email;
    private String passwordHash;
    private String tenantId;
    private Set<String> roles;
    private boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;
}

// 新增角色实体
public class Role {
    private String id;
    private String name;
    private String tenantId;
    private Set<Permission> permissions;
    private String description;
}

// 权限实体
public class Permission {
    private String resource; // FLOW, EXECUTION, etc.
    private String action;   // READ, CREATE, UPDATE, DELETE
    private String namespace; // 可选，命名空间级别权限
}
```

#### 1.2 认证服务重构
- 扩展 `BasicAuthService` 支持数据库用户验证
- 实现 JWT Token 认证机制
- 添加用户会话管理
- 支持密码加密和验证

#### 1.3 数据库迁移
```sql
-- 用户表
CREATE TABLE users (
    id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(250) NOT NULL,
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 角色表
CREATE TABLE roles (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    tenant_id VARCHAR(250) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(name, tenant_id)
);

-- 用户角色关联表
CREATE TABLE user_roles (
    user_id VARCHAR(255),
    role_id VARCHAR(255),
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- 权限表
CREATE TABLE permissions (
    id VARCHAR(255) PRIMARY KEY,
    resource VARCHAR(100) NOT NULL,
    action VARCHAR(100) NOT NULL,
    namespace VARCHAR(250),
    tenant_id VARCHAR(250) NOT NULL
);

-- 角色权限关联表
CREATE TABLE role_permissions (
    role_id VARCHAR(255),
    permission_id VARCHAR(255),
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id),
    FOREIGN KEY (permission_id) REFERENCES permissions(id)
);
```

### 阶段二：多租户核心功能

#### 2.1 租户管理
```java
// 租户实体
public class Tenant {
    private String id;
    private String name;
    private String description;
    private boolean enabled;
    private Map<String, Object> settings;
    private Instant createdAt;
    private Instant updatedAt;
}

// 租户服务重构
@Singleton
public class TenantService {
    private final TenantRepository tenantRepository;
    private final SecurityContext securityContext;
    
    public String resolveTenant() {
        // 从安全上下文获取当前用户的租户ID
        User currentUser = securityContext.getCurrentUser();
        return currentUser != null ? currentUser.getTenantId() : MAIN_TENANT;
    }
    
    public List<Tenant> findAll() {
        return tenantRepository.findAll();
    }
    
    public Optional<Tenant> findById(String tenantId) {
        return tenantRepository.findById(tenantId);
    }
}
```

#### 2.2 安全上下文
```java
// 安全上下文
@Singleton
public class SecurityContext {
    private final ThreadLocal<User> currentUser = new ThreadLocal<>();
    
    public void setCurrentUser(User user) {
        currentUser.set(user);
    }
    
    public User getCurrentUser() {
        return currentUser.get();
    }
    
    public void clear() {
        currentUser.remove();
    }
}
```

#### 2.3 权限检查服务
```java
@Singleton
public class PermissionService {
    private final SecurityContext securityContext;
    private final UserRepository userRepository;
    
    public boolean hasPermission(String resource, String action, String namespace) {
        User user = securityContext.getCurrentUser();
        if (user == null) return false;
        
        return user.getRoles().stream()
            .flatMap(role -> role.getPermissions().stream())
            .anyMatch(permission -> 
                permission.getResource().equals(resource) &&
                permission.getAction().equals(action) &&
                (permission.getNamespace() == null || permission.getNamespace().equals(namespace))
            );
    }
    
    public boolean canAccessNamespace(String namespace) {
        return hasPermission("NAMESPACE", "READ", namespace);
    }
    
    public boolean canExecuteFlow(String namespace) {
        return hasPermission("EXECUTION", "CREATE", namespace);
    }
}
```

### 阶段三：前端改造

#### 3.1 认证状态管理
```javascript
// auth.ts 重构
export class Me {
    constructor(user) {
        this.id = user.id;
        this.username = user.username;
        this.email = user.email;
        this.tenantId = user.tenantId;
        this.roles = user.roles || [];
        this.permissions = this.extractPermissions();
    }
    
    hasAny(permissions) {
        return permissions.some(permission => this.hasPermission(permission));
    }
    
    hasAnyAction(resource, actions, namespace = null) {
        return actions.some(action => this.hasPermission(resource, action, namespace));
    }
    
    isAllowed(resource, action, namespace = null) {
        return this.hasPermission(resource, action, namespace);
    }
    
    hasPermission(resource, action, namespace = null) {
        return this.permissions.some(permission => 
            permission.resource === resource &&
            permission.action === action &&
            (permission.namespace === null || permission.namespace === namespace)
        );
    }
    
    extractPermissions() {
        return this.roles.flatMap(role => role.permissions || []);
    }
}
```

#### 3.2 路由保护
```javascript
// 添加路由守卫
router.beforeEach((to, from, next) => {
    const user = store.getters['auth/user'];
    
    if (to.meta.requiresAuth && !store.getters['auth/isLogged']) {
        next('/login');
        return;
    }
    
    if (to.meta.permissions) {
        const hasPermission = to.meta.permissions.some(permission => 
            user.hasPermission(permission.resource, permission.action, permission.namespace)
        );
        
        if (!hasPermission) {
            next('/unauthorized');
            return;
        }
    }
    
    next();
});
```

### 阶段四：API 层改造

#### 4.1 控制器权限注解
```java
// 权限注解
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {
    String resource();
    String action();
    boolean namespaceFromPath() default false;
}

// 使用示例
@Controller("/api/v1/flows")
public class FlowController {
    
    @Get("/{namespace}")
    @RequiresPermission(resource = "FLOW", action = "READ", namespaceFromPath = true)
    public List<Flow> list(@PathVariable String namespace) {
        return flowService.findByNamespace(tenantService.resolveTenant(), namespace);
    }
    
    @Post("/{namespace}")
    @RequiresPermission(resource = "FLOW", action = "CREATE", namespaceFromPath = true)
    public Flow create(@PathVariable String namespace, @Body Flow flow) {
        return flowService.create(tenantService.resolveTenant(), flow);
    }
}
```

#### 4.2 权限拦截器
```java
@Singleton
public class PermissionInterceptor implements HttpServerFilter {
    private final PermissionService permissionService;
    
    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        return Publishers.fromCompletableFuture(
            CompletableFuture.supplyAsync(() -> {
                // 检查方法权限注解
                RequiresPermission annotation = getPermissionAnnotation(request);
                if (annotation != null) {
                    String namespace = extractNamespace(request, annotation);
                    if (!permissionService.hasPermission(annotation.resource(), annotation.action(), namespace)) {
                        throw new UnauthorizedException("Insufficient permissions");
                    }
                }
                return chain.proceed(request);
            })
        ).flatMap(Function.identity());
    }
}
```

### 阶段五：管理界面

#### 5.1 租户管理页面
- 租户列表和详情
- 租户创建和编辑
- 租户启用/禁用

#### 5.2 用户管理页面
- 用户列表和搜索
- 用户创建和编辑
- 密码重置
- 角色分配

#### 5.3 角色权限管理
- 角色列表和管理
- 权限矩阵配置
- 预定义角色模板

## 实施计划

### 第1周：基础架构
- 设计数据库表结构
- 创建基础实体类
- 实现数据库迁移脚本

### 第2-3周：认证服务
- 重构认证过滤器
- 实现用户服务和仓储
- 添加JWT支持
- 实现密码加密

### 第4-5周：权限系统
- 实现权限服务
- 添加权限注解和拦截器
- 重构现有控制器
- 更新前端权限检查

### 第6-7周：多租户核心
- 重构TenantService
- 实现安全上下文
- 更新所有服务层代码
- 测试租户隔离

### 第8-9周：前端改造
- 重构认证状态管理
- 添加路由保护
- 实现权限相关UI组件
- 更新现有页面权限检查

### 第10-11周：管理界面
- 实现租户管理页面
- 实现用户管理页面
- 实现角色权限管理页面
- 添加系统设置页面

### 第12周：测试和优化
- 全面测试多租户功能
- 性能优化
- 安全性测试
- 文档编写

## 风险评估

### 高风险
- **数据迁移**: 现有数据需要分配到默认租户
- **向后兼容**: 确保现有API不受影响
- **性能影响**: 权限检查可能影响系统性能

### 中风险
- **复杂性增加**: 系统复杂度显著提升
- **测试覆盖**: 需要大量的权限组合测试

### 低风险
- **UI/UX变化**: 用户界面需要适应新的权限模型

## 成功标准

1. **功能完整性**: 支持完整的多租户、用户管理和权限控制
2. **安全性**: 通过安全测试，无权限绕过漏洞
3. **性能**: 权限检查开销小于100ms
4. **兼容性**: 现有功能正常工作
5. **可用性**: 管理界面易于使用

## 后续优化

1. **单点登录（SSO）**: 集成LDAP、OAuth2等
2. **审计日志**: 详细的操作审计
3. **API限流**: 基于租户的API限流
4. **资源配额**: 租户级别的资源限制
5. **多数据库**: 租户数据库隔离