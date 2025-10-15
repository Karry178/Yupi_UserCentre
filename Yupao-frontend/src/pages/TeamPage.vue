<script setup >
import {useRouter} from "vue-router";
import {ref, onMounted} from "vue";
import TeamCardList from "../components/TeamCardList.vue";
import myAxios from "../plugins/myAxios.js";
import {Toast} from "vant";

const router = useRouter();

const active = ref('public')

// 定义队伍列表的响应式变量
const teamList = ref([]);
// const myJoinTeamList = ref([]);
const searchText = ref('');

const toAddTeam = () => {
  // 点击对应的队伍 -> 直接跳转到加入队伍页面
  router.push({
    path: "/team/add"
  })
}

/**
 * 搜索队伍
 *首次进入/team/list界面时，如果没传入字符串(val = '')，就搜索全部队伍
 */
const listTeam = async (val = '', status = 0) => {
  const res = await myAxios.get("/team/list", {
    params: {
      searchText: val,
      pageNum: 1,
      status,
    }
  });
  if (res?.code === 0) {
    teamList.value = res.data;
  } else {
    Toast.fail("加载队伍失败，请刷新重试")
  }
}

// 只会在页面加载时执行一次
onMounted(async() => {
  listTeam();
})

// 执行搜索
const onSearch = async (val) => {
  listTeam(val);
}

// 监听队伍标签切换
const onTabChange = async (name) => {
  if (name === 'public') {
    // 搜索标签为公开的队伍
    listTeam(searchText.value, 0);
  } else {
    // 搜索标签为加密的队伍
    listTeam(searchText.value, 2);
  }
}
</script>

<template>
  <div id="teamPage">
    <van-search v-model="searchText" placeholder="搜索队伍" @search="onSearch" />
    <!-- 队伍页面标签 -->
    <van-tabs v-model:active="active" @change="onTabChange">
      <van-tab title="公开" name="public" />
      <van-tab title="加密" name="private" />
    </van-tabs>
    <!-- 标签与下面内容之间添加间距 -->
    <div style="margin-bottom: 16px" />

    <van-button class="add-button" type="primary" icon="plus" @click="toAddTeam" />
    <team-card-list :teamList="teamList" ></team-card-list>
  </div>

</template>

<style scoped>
/*添加按钮样式 -> 先实现添加队伍按钮到屏幕右下角*/
.add-button {
  position: fixed !important;
  bottom: 80px !important;
  right: 16px !important;
  width: 56px !important;
  height: 56px !important;
  border-radius: 50% !important;
  z-index: 999 !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15) !important;
}

</style>