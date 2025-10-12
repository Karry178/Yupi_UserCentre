<template>
  <div v-if="user">
    <van-cell title="昵称" is-link to="/user/edit" :value="user.username" @click="toEdit('username', '昵称', user.username)"/>
    <van-cell title="账号" :value="user.userAccount" />
    <van-cell title="头像" is-link :value="user.avatarUrl" />
    <van-cell title="性别" is-link to="/user/edit" :value="user.gender" @click="toEdit('gender', '性别', user.gender)"/>
    <van-cell title="电话" is-link to="/user/edit" :value="user.phone" @click="toEdit('phone', '电话', user.phone)"/>
    <van-cell title="邮箱" is-link to="/user/edit" :value="user.email" @click="toEdit('email', '邮箱', user.email)"/>
    <van-cell title="星球编号" :value="user.planetCode" />
    <van-cell title="注册时间" :value="user.createTime" />
  </div>
  <div v-else>
    加载中...
  </div>
</template>

<script setup lang="ts">
import {useRouter} from "vue-router";
import { ref, onMounted } from 'vue';
import { getCurrentUser } from "../services/user.ts";


const router = useRouter();
// const user = computed(() => getCurrentUserState()); // 使用全局状态

const user = ref({
  username: '',
  userAccount: '',
  avatarUrl: '',
  gender: '',
  phone: '',
  email: '',
  planetCode: '',
  createTime: ''
});


onMounted(async () => {
  console.log('用户详情页面加载完毕')
  // 调用Service中的user封装函数获取用户信息
  user.value  = await getCurrentUser();
})


const toEdit = (editKey: string, editName: string, currentValue: string) => {
  router.push({
    // 跳转到主页
    path: '/user/edit',
    // 查询传参
    query: {
      editKey,
      editName,
      currentValue,
    }
  })
}
</script>


<style scoped>

</style>