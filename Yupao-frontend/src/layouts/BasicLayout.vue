<template>
  <van-nav-bar
      title="标题"
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


<script setup>
  import { Toast } from "vant";
  import { useRouter } from "vue-router";

  // 顶部导航栏
  const router = useRouter()
  const onClickLeft = () => {
    router.back();
  };
  const onClickRight = () => {
    router.push('/search')
  };

  // 底部导航栏
  // const active = ref(0);
  const onChange = (index) => Toast(`标签 ${index}`);
</script>


<style scoped>
#content {
  padding-bottom: 50px;
}
</style>