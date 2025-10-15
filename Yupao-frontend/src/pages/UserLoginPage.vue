<template>
  <!-- 用户登录 -->
  <van-form @submit="onSubmit">
    <van-cell-group inset>
      <van-field
          v-model="userAccount"
          name="userAccount"
          label="账号"
          placeholder="请输入账号"
          :rules="[{ required: true, message: '请填写用户名' }]"
      />
      <van-field
          v-model="userPassword"
          type="password"
          name="userPassword"
          label="密码"
          placeholder="请输入密码"
          :rules="[{ required: true, message: '请填写密码' }]"
      />
    </van-cell-group>
    <div style="margin: 16px;">
      <van-button round block type="primary" native-type="submit">
        提交
      </van-button>
    </div>
  </van-form>

</template>

<script setup lang="ts">
import { useRouter,useRoute } from "vue-router";
import { ref } from 'vue';
import myAxios from '../plugins/myAxios.ts';
import { Toast } from 'vant';

const router = useRouter(); // Router是控制页面跳转
const route = useRoute(); // Route是获取当前页面的参数

// 表单数据
const userAccount = ref('');
const userPassword = ref('');


const onSubmit = async (values) => {
  // 连接后端登录界面
  const res = await myAxios.post('/user/login', {
    userAccount: userAccount.value,
    userPassword: userPassword.value,
  })
  console.log(res,'用户登录');
  if (res.code === 0 && res.data) {
    Toast.success('登录成功')
    const redirectUrl = route.query?.redirect as string ?? '/'; // 通过route查询到redirect参数，如果没有就跳转到主页 (??表示前面为真取前面，否则取后面)
    // 成功就路由跳转到主页
    router.replace(redirectUrl);
  } else {
    Toast.fail('登录失败')
  }
};

</script>


<style scoped>

</style>