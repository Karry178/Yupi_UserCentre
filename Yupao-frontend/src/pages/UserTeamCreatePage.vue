<template>
  <div id="userTeamCreatePage">
    <van-nav-bar title="我创建的队伍" left-arrow @click-left="onClickLeft" />
    
    <div v-if="teamList.length === 0" style="text-align: center; padding: 20px;">
      <van-empty description="暂无创建的队伍" />
      <van-button type="primary" to="/team/add" style="margin-top: 16px;">创建队伍</van-button>
    </div>
    
    <team-card-list v-else :teamList="teamList" />
  </div>
</template>

<script setup lang="ts">
import {ref, onMounted} from "vue";
import {useRouter} from "vue-router";
import TeamCardList from "../components/TeamCardList.vue";
import myAxios from "../plugins/myAxios.js";
import {Toast} from "vant";
import {getCurrentUser} from "../services/user.ts";

const router = useRouter();
const teamList = ref([]);

const onClickLeft = () => {
  router.back();
};

// 页面加载时获取我创建的队伍
onMounted(async () => {
  const currentUser = await getCurrentUser();
  if (!currentUser) {
    Toast.fail("请先登录");
    router.push("/user/login");
    return;
  }
  
  // 查询当前用户创建的队伍
  const res: any = await myAxios.get("/team/list", {
    params: {
      userId: currentUser.id
    }
  });
  
  if (res?.code === 0) {
    teamList.value = res.data;
  } else {
    Toast.fail("加载队伍失败");
  }
});
</script>

<style scoped>
</style>
