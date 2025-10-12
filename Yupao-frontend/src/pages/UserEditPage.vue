<template>
  <van-form @submit="onSubmit">
    <!-- 表单项 -->
    <van-cell-group inset>
      <van-field
          v-model="editUser.currentValue"
          :name="editUser.editKey"
          :label="editUser.editName"
          :placeholder="`请输入${editUser.editName}`"
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

import { useRoute,useRouter } from "vue-router";
import { ref } from "vue";
import myAxios from '../plugins/myAxios.ts'
import { Toast } from 'vant';
import { getCurrentUserState,setCurrentUserState } from "../states/user.ts";


// useRoute是查看当前页面路由信息,userRouter是跳转页面
const route = useRoute();
const router = useRouter();

const editUser = ref({
  editKey: route.query.editKey,
  editName: route.query.editName,
  currentValue: route.query.currentValue,
});



// async是异步函数，await是等待异步函数执行完成，返回结果
const onSubmit = async () => {
  const currentUser = getCurrentUserState();

  if (!currentUser){
    Toast.fail('用户未登录');
    return;
  }
  const res = await myAxios.post('/user/update',{
    'id': currentUser.id,
    // 对下面注释掉的对象修改，这句代码可以动态修改，而非静态写死
    [editUser.value.editKey as string]:editUser.value.currentValue,
  })

  console.log(res,'修改用户信息');
  if (res.code === 0 && res.data > 0){
    // 1.修改当前用户信息，并赋值给updatedUser
    const updatedUser = {
      ...currentUser,
      [editUser.value.editKey as string] : editUser.value.currentValue,
    }
    // 2.更新全局用户状态
    setCurrentUserState(updatedUser);
    // 3.提示用户修改成功
    Toast.success('修改成功');
    router.back(); // 返回上一页
  } else {
    Toast.fail('修改失败');
  }
}

console.log(route.query)
const toEdit = (editKey: string, currentValue: string) => {
}
</script>


<style scoped>

</style>