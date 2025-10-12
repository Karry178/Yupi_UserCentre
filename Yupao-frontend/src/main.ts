// src/main.ts

import { createApp } from 'vue'
import App from './App.vue'
import './style.css'

// 全部导入 Vant 组件 (后期优化再精简也可以)
import Vant from 'vant';

// 手动导入 Vant 组件
// import { Button, NavBar, Icon,Tabbar,TabbarItem, Search } from 'vant'

// 导入 Vant 的完整样式
import 'vant/lib/index.css'
import * as VueRouter from 'vue-router';
import routes from "./config/route.ts";


const app = createApp(App)
// 引入全局组件
app.use(Vant);

// 全局注册组件
// app.use(Button) // 引入按钮组件
// app.use(NavBar) // 引入顶部导航栏组件
// app.use(Icon) // 引入图标组件
// // 引入底部导航栏组件
// app.use(Tabbar);
// app.use(TabbarItem);
// app.use(Search); // 引入搜索组件



//创建路由实例并传递routes配置
// 你可以在这里输入更多的配置，但我们在这里暂时保持简单
const router = VueRouter.createRouter({
    //4.内部提供了 history模式的实现。为了简单起见，我们在这里使用 hash 模式。
    history: VueRouter.createWebHashHistory(),
    routes, //routes:routes′的缩写
 })
// 添加路由
app.use(router);
app.mount('#app')


