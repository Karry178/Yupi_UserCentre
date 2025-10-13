<template>
  <van-cell center title="心动模式">
    <template #right-icon>
      <van-switch v-model="isMatchMode" size="24" />
    </template>
  </van-cell>

  <user-card-list :user-list="userList" :loading="loading" />




  <!-- 空状态：如果userList不存在，则提示无符合的 -->
  <van-empty v-if="!userList || userList.length < 1" description="数据为空" />

</template>


<script setup lang="ts">
import { ref, watchEffect, onMounted } from 'vue';
import myAxios from "../plugins/myAxios.ts";
import {Toast} from "vant";
import UserCardList from "../components/UserCardList.vue";
import type {UserType} from "../models/user";


const isMatchMode = ref<boolean>(false); // 定义一个状态变量，用于表示当前模式是否匹配，默认不匹配
const userList = ref([]);
const loading = ref(true); // 默认显示加载中


/**
 * 匹配模式 与 推荐模式
 */
const loadData = async () => {
  let userListData;
  loading.value = true;

  // 匹配(心动)模式
  if (isMatchMode.value) {
    const num = 4;
    // 调用后端接口
    userListData = await myAxios.get("/user/match", {
      params: {
        num,
      },
    })

        .then(function (response) {
          console.log('/user/match successd', response);
          // Toast.success('请求成功');
          // return response?.data?.records; // 后端返回的直接是数组，没有这层records，直接到data即可
          return response?.data;
        })
        .catch(function (error) {
          console.error('/user/match error', error);
          Toast.fail('请求失败');
        })
  } else {
    // 推荐模式/普通模式
    userListData = await myAxios.get('/user/recommend',{
      params: {
        pageSize: 8,
        pageNum: 1,
      },
    })
        .then(function (response){
          console.log('/user/recommend succeed',response);
          // Toast.success('请求成功');
          return response?.data?.records; // 获取分页对象
        })
        .catch(function (error){
          console.error('user/recommend error', error);
          Toast.fail('请求失败');
        })
  }

  // 拿到userListData后，要对标签进行解析
  console.log(userListData)
  if (userListData) {
    userListData.forEach((user: UserType) => {
      if (user.tags){
        // 对每一个user的tags通过JSON转换为数组
        user.tags = JSON.parse(user.tags);
      }
    })
    // 如果userListData有值，就赋给userList
    userList.value = userListData;
  }
  // 所有模式加载完成，都要关闭加载中
  loading.value = false;
}

// 使用钩子函数：页面加载时调用
onMounted(() => {
  loadData();
})

// 监听模式切换
watchEffect(() => {
  // alert('Yupao' + isMatchMode.value) // 要加.value,因为是响应式数据

  // 每次改变模式，都要重新加载数据
  loadData();
})


</script>

<style scoped>

</style>