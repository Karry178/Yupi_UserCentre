<template>
  <user-card-list :user-list="userList"  />

  <!-- 空状态：如果userList不存在，则提示无符合的 -->
  <van-empty v-if="!userList || userList.length < 1" description="数据为空" />

</template>


<script setup lang="ts">
import {onMounted, ref} from 'vue';
import {useRoute} from "vue-router";
import myAxios from "../plugins/myAxios.ts";
import {Toast} from "vant";
import UserCardList from "../components/UserCardList.vue";

const route = useRoute();

const userList = ref([]);

onMounted(async () => {
  const userListData = await myAxios.get('/user/recommend',{
    params: {
      pageSize: 8,
      pageNum: 1,
    },
  })
      .then(function (response){
        console.log('/user/recommend succeed',response);
        Toast.success('请求成功');
        return response?.data?.records; // 获取分页对象
      })
      .catch(function (error){
        console.error('user/recommend error', error);
        Toast.fail('请求失败');
      })

  // 拿到userListData后，要对标签进行解析
  console.log(userListData)
  if (userListData) {
    userListData.forEach(user => {
      if (user.tags){
        // 对每一个user的tags通过JSON转换为数组
        user.tags = JSON.parse(user.tags);
      }
    })
    // 如果userListData有值，就赋给userList
    userList.value = userListData;
  }
})



</script>

<style scoped>

</style>