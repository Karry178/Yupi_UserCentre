<template>
  <van-card
      v-for="user in props.userList"
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
import type {UserType} from "../models/user";

interface UserCardListProps {
  userList: UserType[];
}
const props = withDefaults(defineProps<UserCardListProps>(), {
  // 使用@ts-ignore忽略ts的类型检查
  // @ts-ignore
  // 添加一个默认值
  userList: [] as UserType[],
});
</script>


<style scoped>

</style>