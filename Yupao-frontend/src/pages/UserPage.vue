<template>
  <div id="userPage">
    <!-- 用户信息卡片 -->
    <van-cell
        v-if="user && user.username"
        center
        is-link
        @click="toUpdate"
    >
      <template #icon>
        <van-image
            round
            width="60"
            height="60"
            :src="user.avatarUrl || 坤lemon.jpeg"
            style="margin-right: 12px;"
        />
      </template>
      <template #title>
        <div style="font-size: 18px; font-weight: bold;">{{ user.username }}</div>
      </template>
      <template #label>
        <div style="margin-top: 4px;">{{ user.userAccount }}</div>
      </template>
    </van-cell>

    <!-- 功能列表 -->
    <van-cell-group v-if="user && user.username" style="margin-top: 16px;" title="我的">
      <van-cell title="修改个人信息" is-link to="/user/update" icon="edit" />
      <van-cell title="我创建的队伍" is-link to="/user/team/create" icon="friends-o" />
      <van-cell title="我加入的队伍" is-link to="/user/team/join" icon="manager-o" />
    </van-cell-group>

    <!-- 未登录状态 -->
    <div v-if="!user || !user.username" style="text-align: center; padding: 40px 20px;">
      <van-empty description="请先登录" />
      <van-button type="primary" to="/user/login" style="margin-top: 16px;">去登录</van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import {useRouter} from "vue-router";
import { ref, onMounted } from 'vue';
import { getCurrentUser } from "../services/user.ts";
import iKun from '../assets/坤lemon.jpg';


const user = ref();

const router = useRouter();

onMounted(async () => {
  console.log('用户详情页面加载完毕')
  // 调用Service中的user封装函数获取用户信息
  user.value  = await getCurrentUser();
})


const toUpdate = () => {
  router.push('/user/update');
};

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