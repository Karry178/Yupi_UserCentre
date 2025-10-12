<template>
  <van-card
      v-for="user in userList"
      :desc="user.profile"
      :title="`${user.username}(${user.planetCode})`"
      thumb="https://fastly.jsdelivr.net/npm/@vant/assets/ipad.jpeg"
  >
    <template #tags>
      <van-tag plain type="danger" v-for="tag in user.tags" style="margin-right: 8px;margin-top: 8px;">
        {{tag}}
      </van-tag>
    </template>
    <template #footer>
      <van-button size="mini">联系我</van-button>
    </template>
  </van-card>

  <!-- 空状态：如果userList不存在，则提示无符合的 -->
    <van-empty v-if="!userList || userList.length < 1" description="搜索结果为空" />

</template>


<script setup lang="ts">
import {onMounted, ref} from 'vue';
import {useRoute} from "vue-router";
import myAxios from "../plugins/myAxios.ts";
import {Toast} from "vant";
import qs from 'qs'; // qs:一个查询字符串的解析器

const route = useRoute();
const {tags} = route.query;

const userList = ref([]);

onMounted(async () => {
  const userListData = await myAxios.get('user/search/tags',{
    params: {
      tagNameList: tags
    },
    paramsSerializer: params => {
      // paramsSerializer是什么？用于告诉Axios如何序列化参数
      // qs 是一个查询字符串的解析器
      return qs.stringify(params, { indices: false })
    }
  })
  .then(function (response){
    console.log('/user/search/tags succeed',response);
    Toast.success('请求成功');
    return response?.data;
  })
      .catch(function (error){
        console.error('user/search/tags error', error);
        Toast.fail('请求失败');
      })
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



// 方便测试，做一个假的用户
// const mockUser = {
//   id: 123,
//   username: '猪猪侠',
//   userAccount: 'dogyupi',
//   avatarUrl: '',
//   gender: '男',
//   phone: '18273654321',
//   email: '12435@qq.com',
//   createTime: new Date(),
//   tags: ['java','emo','打工牛马'],
//   planetCode: '12345',
//   profile:'biubiubiu啊尽快发货几十块打工魂发卡机三打哈饭卡受',
// }
// 定义一个假的用户列表
// const userList = ref([mockUser]);



</script>

<style scoped>

</style>