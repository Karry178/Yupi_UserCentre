<template>
  <form action="/">
    <van-search
        v-model="searchText"
        show-action
        placeholder="请输入要搜索的标签"
        @search="onSearch"
        @cancel="onCancel"
    />
  </form>

  <!-- Divider：分割线 -->
  <van-divider content-position="left">已选标签</van-divider>
  <div v-if="activeIds.length === 0">请选择标签</div>

  <!-- Layout 组件提供列间距 -->
  <van-row gutter="16">
    <!-- van-col -->
    <van-col v-for="tag in activeIds">
      <!-- Tag：可关闭标签 -->
      <van-tag  closeable size="small" type="primary" @close="doClose(tag)">
        {{ tag }}
      </van-tag>
    </van-col>
  </van-row>

  <van-divider content-position="left">选择标签</van-divider>
    <!-- TreesSelect 多选模式 -->
    <van-tree-select
        v-model:active-id="activeIds"
        v-model:main-active-index="activeIndex"
        :items="tagList"
    />

  <!-- 顶部搜索按钮 -->
  <div style="padding: 20px">
    <van-button block type="primary" @click="doSearchResult" style="margin: 12px">搜索</van-button>
  </div>

</template>


<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from "vue-router";
const searchText = ref('');
const router = useRouter();

// 源标签列表
const originTagList = [
    {
      text: '性别',
      children: [
        { text: '男', id: '男' },
        { text: '女', id: '女' },
        { text: '莫名其妙性别', id: '莫名其妙性别', disabled: true },
      ],
    },
    {
      text: '年级',
      children: [
        { text: '研一', id: '研一' },
        { text: '研二', id: '研二' },
        { text: '研三', id: '研三' },
      ],
    },
    { text: '福建', disabled: true },
    ];

// 多选模式分组 - 标签列表
let tagList = ref(originTagList);


/**
 * 搜索过滤
 * @param val
 */
const onSearch = (val) => {
  // 先对父级标签列表遍历获取，每一次查询都要复制一份原始数组去查询，然后再根据子标签内容过滤(item)
  tagList.value = originTagList.map(parentTag => {
    // 复制一份原始数组的children标签页给temp的children标签页
    const tempChildren = [...(parentTag.children || [])];
    // 复制一份原始数据给temp父级
    const tempParentTag = {...parentTag};
    tempParentTag.children = tempChildren.filter(item => item.text.includes(searchText.value));
    return tempParentTag;
  });
}

// 取消搜索
const onCancel = () => {
  searchText.value = '';
  tagList.value = originTagList;
}

// 已选中的标签
const activeIds = ref([]);
const activeIndex = ref(0);

// 关闭/移除标签的函数
const doClose = (tag) => {
  activeIds.value = activeIds.value.filter(item => {
    return item !== tag;
  })
}

/**
 * 执行搜索后，跳转搜索结果
 */
const doSearchResult = () => {
  router.push({
    path: '/user/list',
    query: {
      tags: activeIds.value
    }
  })
}

</script>

<style scoped>

</style>