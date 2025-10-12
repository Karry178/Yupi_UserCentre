<script setup >
import {useRouter} from "vue-router";
import {ref, onMounted} from "vue";
import TeamCardList from "../components/TeamCardList.vue";
import myAxios from "../plugins/myAxios.js";
import {Toast} from "vant";

const router = useRouter();
// 定义队伍列表的响应式变量
const teamList = ref([]);
// const myJoinTeamList = ref([]);
const searchText = ref('');

const doJoinTeam = () => {
  // 点击对应的队伍 -> 直接跳转到加入队伍页面
  router.push({
    path: "/team/add"
  })
}

/**
 * 搜索队伍
 *首次进入/team/list界面时，如果没传入字符串(val = '')，就搜索全部队伍
 */
const listTeam = async (val = '') => {
  const res = await myAxios.get("/team/list", {
    params: {
      searchText: val,
      pageNum: 1,
    }
  });
  if (res?.code === 0) {
    teamList.value = res.data;
  } else {
    Toast.fail("加载队伍失败，请刷新重试")
  }
}

/**
 * 搜索用户加入的队伍
 *首次进入/team/list界面时，如果没传入字符串(val = '')，就搜索全部队伍
 */
/*const listMyJoinTeam = async (val = '') => {
  const res = await myAxios.get("/team/list", {
    params: {
      searchText: val,
      pageNum: 1,
    }
  });
  if (res?.code === 0) {
    myJoinTeamList.value = res.data;
  } else {
    Toast.fail("加载队伍失败，请刷新重试")
  }
}*/

// 只会在页面加载时执行一次
onMounted(async() => {
  listTeam();
})

// 执行搜索
const onSearch = async (val) => {
  listTeam(val);
}
</script>

<template>
  <div id="teamPage">
    <van-search v-model="searchText" placeholder="搜索队伍" @search="onSearch" />
    <van-button type="primary" @click="doJoinTeam">加入队伍</van-button>
    <team-card-list :teamList="teamList" ></team-card-list>
  </div>

</template>

<style scoped>

</style>