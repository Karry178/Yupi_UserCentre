<script setup >
import {useRouter} from "vue-router";
import { ref } from "vue";
import myAxios from "../plugins/myAxios.js";
import {Toast} from "vant";

const router = useRouter();

// 定义一个初始表单数据
const initFormData = {
  "description": "",
  "expireTime": null,
  "maxNum": 3,
  "name": "",
  "password": "",
  "status": 0,
  "userId": 0
}

// 需要用户填写表单数据
const addTeamData = ref({...initFormData})
const result = ref('');
const showPicker = ref(false); // 显示日期选择器(false:默认不显示)
const value = ref(1);
const checked = ref('1');
const minDate = new Date(); // 默认进网页的时间为最小时间

// 点击触发加入队伍
const doJoinTeam = () => {
  // 点击对应的队伍 -> 直接跳转到加入队伍页面
  router.push({
    path: "/team/add"
  })
}

// 点击添加队伍 传递数据给后端
const onSubmit = async() => {
  // postData是添加队伍的状态参数
  const postData = {
    ...addTeamData.value,
    status: Number(addTeamData.value.status)
  }
  // todo 需要补充前端参数校验
  const res = await myAxios.post("/team/add", postData);
  if (res?.code === 0 && res.data) {
    Toast.success('添加成功');
    // 跳转到team页
    router.push({
      path: "/team",
      replace: true
    });
  } else {
    Toast.fail('添加失败');
  }
}

</script>

<template>
  <div id="teamAddPage">
    <!-- 用户新建队伍 -->
    <van-form @submit="onSubmit">
      <van-cell-group inset>
        <van-field
            v-model="addTeamData.name"
            name="name"
            label="队伍名"
            placeholder="请输入队伍名"
            :rules="[{ required: true, message: '请填写队伍名' }]"
        />

        <!-- 队伍描述 -->
        <van-field
            v-model="addTeamData.description"
            rows="4"
            autosize
            label="队伍描述"
            type="textarea"
            placeholder="请输入队伍描述"
        />

          <!-- 过期时间 -->
        <van-field
            is-link
            readonly
            name="datetimePicker"
            label="过期时间"
            :placeholder="addTeamData.expireTime ?? '点击选择过期时间'"
            @click="showPicker = true"
        />
        <van-popup v-model:show="showPicker" position="bottom">
          <van-datetime-picker
              v-model="addTeamData.expireTime"
              @confirm="showPicker = false"
              type="datetime"
              title="请选择过期时间"
              :min-date="minDate"
          />
        </van-popup>

        <!-- 使用步进器，选择最大队伍人数,最大值为10 -->
        <van-field name="stepper" label="队伍最大人数">
          <template #input>
            <van-stepper v-model="addTeamData.maxNum" :max="10" :min="3" />
          </template>
        </van-field>

        <!-- 单选框:选择队伍状态 -->
        <van-field name="radio" label="队伍状态">
          <template #input>
            <van-radio-group v-model="addTeamData.status" direction="horizontal">
              <van-radio name="0">公开</van-radio>
              <van-radio name="1">私有</van-radio>
              <van-radio name="2">加密</van-radio>
            </van-radio-group>
          </template>
        </van-field>

        <!-- 在队伍选择加密状态后，才能输入密码 -->
        <van-field
            v-if="addTeamData.status === '2'"
            v-model="addTeamData.password"
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
    <!-- 开发时打印出参数各自属性，对照开发 -->
    {{
      addTeamData
    }}
  </div>

</template>

<style scoped>

</style>