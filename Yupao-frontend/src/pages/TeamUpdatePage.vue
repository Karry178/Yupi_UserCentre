<script setup >
import {useRouter} from "vue-router";
import {useRoute} from "vue-router";
import {ref, onMounted} from "vue";
import myAxios from "../plugins/myAxios.js";
import {Toast} from "vant";


const router = useRouter(); // 全局路由对象
const route = useRoute(); // 获取当前界面的路由信息

// 需要用户填写表单数据
const addTeamData = ref({})
const result = ref('');
const showPicker = ref(false); // 显示日期选择器(false:默认不显示)
const value = ref(1);
const checked = ref('1');
const minDate = new Date(); // 默认进网页的时间为最小时间
const id = route.query.id;

// 根据队伍id查询队伍信息
onMounted(async () => {
  if (!id || id <= 0) {
    Toast.fail("加载队伍失败，请刷新重试")
    return;
  }
  // 否则，拿到队伍id，发送请求获取队伍信息
  const res = await myAxios.get("/team/get", {
    params: {
      id,  // 使用从路由获取的 id 变量
    }
  });
  if (res?.code === 0) {
    addTeamData.value = res.data; // 添加队伍的数据
  } else {
    Toast.fail("加载队伍失败，请刷新重试")
  }
})

// 点击更新队伍 传递数据给后端
const onSubmit = async() => {
  // postData是更新队伍的状态参数
  const postData = {
    id: id,  // 必须传递队伍id
    ...addTeamData.value,
    status: Number(addTeamData.value.status)
  }
  // todo 需要补充前端参数校验
  const res = await myAxios.post("/team/update", postData);
  if (res?.code === 0 && res.data) {
    Toast.success('更新成功');
    // 跳转到team页
    router.push({
      path: "/team",
      replace: true
    });
  } else {
    Toast.fail('更新失败');
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
            v-if="addTeamData.status === 2 || addTeamData.status === '2'"
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