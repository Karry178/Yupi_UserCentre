<template>
  <van-nav-bar
      :title="title"
      left-arrow
      @click-left="onClickLeft"
      @click-right="onClickRight"
  >
    <!-- 自定义顶部导航栏右侧图标 -->
    <template #right>
      <van-icon name="search" size="18" />
    </template>
  </van-nav-bar>

  <!-- 路由切换 -->
  <div id="content">
    <router-view />
  </div>

  <!-- RouterLInk：超链接 -->
  <!--  <RouterLink to="/">Go to Home</RouterLink>-->
  <!--  <RouterLink to="/team">队伍</RouterLink>-->

   <!-- 引入底部导航栏——监听切换事件 -->
  <van-tabbar route @change="onChange">
    <van-tabbar-item to="/" icon="home-o" name="index">主页</van-tabbar-item>
    <van-tabbar-item to="/team" icon="search" name="team">队伍</van-tabbar-item>
    <van-tabbar-item to="/user" icon="friends-o" name="user">个人</van-tabbar-item>
  </van-tabbar>

</template>


<script setup lang="ts">
  import { Toast } from "vant";
  import { ref } from "vue";
  import { useRouter, useRoute } from "vue-router";
  import routes from "../config/route.ts";
  import {getCurrentUser} from "../services/user.ts";  // ✅ 导入 routes


  // 顶部导航栏
  const router = useRouter()
  const route = useRoute()
  const DEFAULT_TITLE = "伙伴匹配";
  const title = ref(DEFAULT_TITLE);

  /**
   * 路由监听的钩子函数
   * 功能：根据路由配置，切换标题
    */
  router.beforeEach(async (to, from, next) => {
    console.log('路由跳转', to.path)

    // 查找路由配置
    const toPath = to.path;
    const targetRoute = routes.find((route) => {
      return toPath === route.path;
    })

    // 1.添加登录验证逻辑
    // 白名单
    const whiteList = ['/user/login','/user/register']
    if (whiteList.includes(to.path)) {
      next() // 允许通过
      return
    }
    // 检查用户是否登录
    const currentUser = await getCurrentUser()
    if (!currentUser) {
      // 未登录，跳转(重定向)到登录页面,并记录原本要访问的界面
      Toast.fail('请先登录')
      next({
        path: '/user/login',
        query: { redirect: to.path } // 添加一个查询参数，记录原本要访问的界面
      })
      return
    }


    // 2.更新队伍页的标题
    if (targetRoute?.title) { // 如果route存在或者有title，就使用路由配置的标题
      title.value = targetRoute.title;
    } else {
      // 否则返回默认标题
      title.value = DEFAULT_TITLE;
    }
    next() // 允许导航继续
  })


  const onClickLeft = () => {
    router.back();
  };
  const onClickRight = () => {
    router.push('/search')
  };

</script>


<style scoped>
#content {
  padding-bottom: 50px;
}
</style>