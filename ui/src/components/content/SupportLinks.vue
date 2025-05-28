<template>
    <h2 class="support-title">
        {{ title }}
    </h2>
    <div class="support-links-row">
        <a v-for="button in buttons" :key="button.label" :href="button.href" class="support-link">
            <component :is="button.icon" />
            <h3>{{ button.label }}</h3>
            <p>{{ button.description }}</p>
        </a>
    </div>
</template>

<script setup lang="ts">
    import GithubIcon from "vue-material-design-icons/Github.vue";
    import BugIcon from "vue-material-design-icons/Bug.vue";
    import LifebuoyIcon from "vue-material-design-icons/Lifebuoy.vue";

    defineProps<{
        title: string
    }>()

    const buttons = [{
                         label: "报告问题",
                         description: "报告Bug、提出功能请求或寻求帮助",
                         href: "#",
                         icon: BugIcon,
                         action: "report-issue"
                     },
                     {
                         label: "GitHub",
                         description: "给我们的开源项目点个星",
                         href: "https://github.com/kestra-io/kestra",
                         icon: GithubIcon,
                     },
                     {
                         label: "帮助中心",
                         description: "联系技术支持获取企业版账户帮助",
                         href: "https://dataflare.io/support",
                         icon: LifebuoyIcon,
                     }]
</script>

<style lang="scss" scoped>
$font-size-lg: 1.5rem;
$font-size-sm: 1rem;
$black-3: #2B313E;
$white: #FFFFFF;
$white-1: #E0E0E0;
$primary-1: #3F51B5;
h2.support-title{
    border: none;
    font-size: $font-size-lg;
    margin-top: 3rem;
    margin-bottom: 1rem;
    padding: 0;
    font-weight: 400;
}
.support-links-row{
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 1rem;
    margin-bottom: 3rem;
}
.support-link{
    padding: 1rem;
    border-radius: .5rem;
    border: 1px solid $black-3;
    span{
        font-size: 24px;
        color: $white;
        margin-bottom: 1rem;
    }
    h3{
        font-size: $font-size-lg;
        padding: 0;
        margin: 0
    }
    p{
        margin-top: .5rem;
        font-size: $font-size-sm;
        color: $white-1;
        line-height: 1.6em;
    }
    &:hover{
        border-color: $primary-1;
    }
}
</style>