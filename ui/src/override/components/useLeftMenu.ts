import {computed, shallowRef} from "vue";
import {useStore} from "vuex";
import {useRouter} from "vue-router";
import {useI18n} from "vue-i18n";

import FileTreeOutline from "vue-material-design-icons/FileTreeOutline.vue";
import ContentCopy from "vue-material-design-icons/ContentCopy.vue";
import TimelineClockOutline from "vue-material-design-icons/TimelineClockOutline.vue";
import TimelineTextOutline from "vue-material-design-icons/TimelineTextOutline.vue";
import ChartTimeline from "vue-material-design-icons/ChartTimeline.vue";
import BallotOutline from "vue-material-design-icons/BallotOutline.vue";
import ShieldAccountVariantOutline from "vue-material-design-icons/ShieldAccountVariantOutline.vue";
import ViewDashboardVariantOutline from "vue-material-design-icons/ViewDashboardVariantOutline.vue";
import Connection from "vue-material-design-icons/Connection.vue";
import DotsSquare from "vue-material-design-icons/DotsSquare.vue";
import DatabaseOutline from "vue-material-design-icons/DatabaseOutline.vue";
import AccountMultiple from "vue-material-design-icons/AccountMultiple.vue";
import Security from "vue-material-design-icons/Security.vue";
import OfficeBuilding from "vue-material-design-icons/OfficeBuilding.vue";
import MonitorDashboard from "vue-material-design-icons/MonitorDashboard.vue";
import FileDocumentOutline from "vue-material-design-icons/FileDocumentOutline.vue";

export function useLeftMenu() {
    const {t} = useI18n({useScope: "global"});
    const $router = useRouter();
    const store = useStore();

    /**
     * Returns all route names that start with the given route
     * @param route
     * @returns
     */
    function routeStartWith(route: string) {
        return $router
            ?.getRoutes()
            .filter(
                (r) => typeof r.name === "string" && r.name.startsWith(route),
            )
            .map((r) => r.name);
    }

    const configs = computed(() => store.state.misc.configs);

    // This object seems to be a good candidate for a computed value
    // but cannot be. When it becomes a computed, the hack to set current
    // route as active in the blueprints activates pages forever.
    const generateMenu = () => {
        return [
            {
                href: {name: "home"},
                title: t("homeDashboard.title"),
                icon: {
                    element: shallowRef(ViewDashboardVariantOutline),
                    class: "menu-icon",
                },
            },
            {
                href: {name: "flows/list"},
                routes: routeStartWith("flows"),
                title: t("flows"),
                icon: {
                    element: shallowRef(FileTreeOutline),
                    class: "menu-icon",
                },
                exact: false,
            },

            {
                href: {name: "templates/list"},
                routes: routeStartWith("templates"),
                title: t("templates"),
                icon: {
                    element: shallowRef(ContentCopy),
                    class: "menu-icon",
                },
                hidden: !configs.value.isTemplateEnabled,
            },
            {
                href: {name: "executions/list"},
                routes: routeStartWith("executions"),
                title: t("executions"),
                icon: {
                    element: shallowRef(TimelineClockOutline),
                    class: "menu-icon",
                },
            },
            {
                href: {name: "taskruns/list"},
                routes: routeStartWith("taskruns"),
                title: t("taskruns"),
                icon: {
                    element: shallowRef(ChartTimeline),
                    class: "menu-icon",
                },
                hidden: !configs.value.isTaskRunEnabled,
            },
            {
                href: {name: "logs/list"},
                routes: routeStartWith("logs"),
                title: t("logs"),
                icon: {
                    element: shallowRef(TimelineTextOutline),
                    class: "menu-icon",
                },
            },
            {
                href: {name: "namespaces/list"},
                routes: routeStartWith("namespaces"),
                title: t("namespaces"),
                icon: {
                    element: shallowRef(DotsSquare),
                    class: "menu-icon",
                },
            },
            {
                href: {name: "kv/list"},
                routes: routeStartWith("kv"),
                title: t("kv.name"),
                icon: {
                    element: shallowRef(DatabaseOutline),
                    class: "menu-icon",
                },
            },
            {
                routes: routeStartWith("blueprints"),
                title: t("blueprints.title"),
                icon: {
                    element: shallowRef(BallotOutline),
                    class: "menu-icon",
                },
                child: [
                    {
                        title: t("blueprints.flows"),
                        routes: routeStartWith("blueprints/flow"),
                        href: {
                            name: "blueprints",
                            params: {kind: "flow", tab: "community"},
                        },
                    },
                    {
                        title: t("blueprints.dashboards"),
                        routes: routeStartWith("blueprints/dashboard"),
                        href: {
                            name: "blueprints",
                            params: {kind: "dashboard", tab: "community"},
                        },
                    },
                ],
            },
            {
                href: {name: "plugins/list"},
                routes: routeStartWith("plugins"),
                title: t("plugins.names"),
                icon: {
                    element: shallowRef(Connection),
                    class: "menu-icon",
                },
            },
            {
                title: t("rbac.title"),
                routes: routeStartWith("rbac"),
                icon: {
                    element: shallowRef(AccountMultiple),
                    class: "menu-icon",
                },
                child: [
                    {
                        href: {name: "rbac/users"},
                        routes: routeStartWith("rbac/users"),
                        title: t("rbac.users"),
                    },
                    {
                        href: {name: "rbac/roles"},
                        routes: routeStartWith("rbac/roles"),
                        title: t("rbac.roles"),
                    },
                    {
                        href: {name: "rbac/groups"},
                        routes: routeStartWith("rbac/groups"),
                        title: t("rbac.groups"),
                    },
                    {
                        href: {name: "rbac/bindings"},
                        routes: routeStartWith("rbac/bindings"),
                        title: t("rbac.bindings"),
                    },
                ],
            },
            {
                title: t("auth.title"),
                routes: routeStartWith("auth"),
                icon: {
                    element: shallowRef(Security),
                    class: "menu-icon",
                },
                child: [
                    {
                        href: {name: "auth/sso"},
                        routes: routeStartWith("auth/sso"),
                        title: t("auth.sso"),
                    },
                    {
                        href: {name: "auth/mfa"},
                        routes: routeStartWith("auth/mfa"),
                        title: t("auth.mfa"),
                    },
                    {
                        href: {name: "auth/tokens"},
                        routes: routeStartWith("auth/tokens"),
                        title: t("auth.tokens"),
                    },
                ],
            },
            {
                title: t("tenants.title"),
                routes: routeStartWith("tenants"),
                icon: {
                    element: shallowRef(OfficeBuilding),
                    class: "menu-icon",
                },
                child: [
                    {
                        href: {name: "tenants/list"},
                        routes: routeStartWith("tenants/list"),
                        title: t("tenants.management"),
                    },
                    {
                        href: {name: "tenants/dashboard"},
                        routes: routeStartWith("tenants/dashboard"),
                        title: t("tenants.dashboard"),
                    },
                ],
            },
            {
                title: t("monitoring.title"),
                routes: routeStartWith("monitoring"),
                icon: {
                    element: shallowRef(MonitorDashboard),
                    class: "menu-icon",
                },
                child: [
                    {
                        href: {name: "monitoring/system"},
                        routes: routeStartWith("monitoring/system"),
                        title: t("monitoring.system"),
                    },
                    {
                        href: {name: "monitoring/custom"},
                        routes: routeStartWith("monitoring/custom"),
                        title: t("monitoring.custom"),
                    },
                ],
            },
            {
                href: {name: "audit/logs"},
                routes: routeStartWith("audit"),
                title: t("audit.title"),
                icon: {
                    element: shallowRef(FileDocumentOutline),
                    class: "menu-icon",
                },
            },
            {
                title: t("administration"),
                routes: routeStartWith("admin"),
                icon: {
                    element: shallowRef(ShieldAccountVariantOutline),
                    class: "menu-icon",
                },
                child: [
                    {
                        href: {name: "admin/triggers"},
                        routes: routeStartWith("admin/triggers"),
                        title: t("triggers"),
                    },
                    {
                        href: {name: "admin/stats"},
                        routes: routeStartWith("admin/stats"),
                        title: t("stats"),
                    },
                ],
            }
        ];
    };

    return {generateMenu};
}