<template>
    <top-nav-bar :title="routeInfo.title" />
    <section class="container" v-show="ready">
        <Usages class="mb-2" @loaded="ready = true" />
        <BasicAuthPrompt class="mt-4 mb-2" />
    </section>
</template>
<script>
    import RouteContext from "../../../../mixins/routeContext";
    import TopNavBar from "../../../../components/layout/TopNavBar.vue";
    import Usages from "../../../../components/admin/stats/Usages.vue";
    import BasicAuthPrompt from "../../../../components/admin/stats/BasicAuthPrompt.vue";

    export default {
        mixins: [RouteContext],
        components: {TopNavBar, Usages, BasicAuthPrompt},
        data() {
            return {
                ready: false
            };
        },
        computed: {
            routeInfo() {
                return {
                    title: this.$t("stats")
                }
            }
        }
    };
</script>