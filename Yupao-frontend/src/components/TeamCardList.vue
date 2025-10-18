<template>
  <div
      id="teamCardList"
  >
  <van-card
      v-for="team in props.teamList"
      :thumb="iKun"
      :desc="team.description"
      :title="`${team.name}`"
  >
    <template #tags>
      <van-tag plain type="danger" style="margin-right: 8px;margin-top: 8px;">
        <!-- 因为TS不允许number类型直接索引，需要明确TS这个对象的有效key -->
        {{ teamStatusEnum[team.status as keyof typeof teamStatusEnum] }}
      </van-tag>
    </template>
    <template #bottom>
      <div>
        {{ '最大人数: ' + team.maxNum }}
      </div>
      <div v-if="team.expireTime">
        {{ '过期时间' + team.expireTime }}
      </div>
      <div>
        {{ '创建时间' + team.createTime }}
      </div>
    </template>
    <template #footer>
      <!-- 加入队伍：仅非创建者与未加入的人可见 -->
      <van-button size="small" type="primary" v-if="team.userId !== currentUser?.id && !team.hasJoin" plain
                  @click="doJoinTeam(team)">加入队伍</van-button>
      <!-- 更新队伍：仅创建者可见 -->
      <van-button v-if="team.userId === currentUser?.id" size="small" plain
                  @click="doUpdateTeam(team.id)">更新队伍</van-button>

      <!-- 退出队伍：仅加入队伍的人可见，创建人不可见 -->
      <van-button size="small" v-if="team.hasJoin" plain
                  @click="doQuitTeam(team.id)">退出队伍</van-button>

      <!-- 解散队伍：仅创建者可见 -->
      <van-button size="small" v-if="team.userId === currentUser?.id" type="danger" plain
                  @click="doDeleteTeam(team.id)">解散队伍</van-button>
    </template>
    {{ '当前用户' + currentUser }}
  </van-card>




    <!-- 空状态：如果teamList不存在，则提示无符合的 -->
  <van-empty v-if="!teamList || teamList.length < 1" description="搜索结果为空" />
  </div>
</template>


<script setup lang="ts">
import type { TeamType } from "../models/team";
import {teamStatusEnum} from "../constants/team.ts";
import iKun from '../assets/iKun.jpg';
import myAxios from "../plugins/myAxios.ts";
import {Toast} from "vant";
import {getCurrentUser} from "../services/user.ts";
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";

interface TeamCardListProps {
  teamList: TeamType[];
}
const props = withDefaults(defineProps<TeamCardListProps>(), {
  // 使用@ts-ignore忽略ts的类型检查
  // @ts-ignore
  // 添加一个默认值
  teamList: [] as TeamType[],
});

// 页面跳转的路由
const router = useRouter();

// 密码弹窗相关变量
const showPasswordDialog = ref(false);
console.log('[调试] showPasswordDialog 初始值:',showPasswordDialog.value)
const password = ref('');
const joinTeamId = ref(0); // 记录加入的队伍id
// 获取当前用户的信息
const currentUser = ref()
onMounted(async () => {
  currentUser.value = await getCurrentUser()
})


// 点击加入队伍
const doJoinTeam = async (team: TeamType) => {
  const { id, status } = team;

  // 加密队伍 - 显示密码输入框
  if (status === 2) {
    /*showPasswordDialog.value = true;
    joinTeamId.value = id;*/
    const inputPassword = prompt('请输入队伍密码：');
    if (!inputPassword) {
      return;
    }
  }

  // 3.私有队伍不允许加入
  if (status === 1) {
    Toast.fail("私有队伍不可加入");
    return;
  }

  // 公开队伍 - 直接加入 -> 加入any类型是为了避免类型检查，告诉TS这个对象有code属性
  const res: any = await myAxios.post("/team/join", {
    teamId: id
  });
  
  if (res?.code === 0) {
    Toast.success('加入成功');
  } else {
    Toast.fail('加入失败');
  }
}

/**
 * 确认密码并加入队伍
 */
const onConfirmPassword = async () => {
  // 验证密码是否为空
  if (!password.value) {
    Toast.fail('请输入密码')
    return;
  }
  
  // 调用加入队伍接口
  const res: any = await myAxios.post("/team/join", {
    teamId: joinTeamId.value,
    password: password.value
  })

  // 处理结果
  if (res?.code === 0) {
    Toast.success('加入成功');
    showPasswordDialog.value = false;
    password.value = '';  // 清空密码
  } else {
    // 加入失败，显示错误原因description 或者''是指如果description为空，则显示''
    Toast.fail('加入失败' + (res?.description || ''));
  }
}

/**
 * 点击跳转到更新队伍页面
 */
const doUpdateTeam = (id: number) => {
  router.push({
    path: '/team/update',
    query: {
      id: id,
    }
  })
}

/**
 * 点击退出队伍
 */
const doQuitTeam = async (id: number) => {
  const res: any = await myAxios.post("/team/quit", {
    teamId: id
  });
  if (res?.code === 0 ) {
    Toast.success('操作成功');
  } else {
    Toast.fail('操作失败' + (res?.description ? `,${res.description}` : ''));
  }
}

/**
 * 点击解散队伍
 */
const doDeleteTeam = async (id: number) => {
  const res: any = await myAxios.post("/team/delete", id);
  if (res?.code === 0 ) {
    Toast.success('操作成功');
  } else {
    Toast.fail('操作失败' + (res?.description ? `,${res.description}` : ''));
  }
}
</script>


<style scoped>
#teamCardList :deep(.van-image__img) {
  object-fit: unset;
}
</style>