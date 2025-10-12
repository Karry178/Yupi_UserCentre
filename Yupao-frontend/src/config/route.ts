import IndexPage from "../pages/IndexPage.vue";
import TeamPage from "../pages/TeamPage.vue";
import UserPage from "../pages/UserPage.vue";
import SearchPage from "../pages/SearchPage.vue";
import UserEditPage from "../pages/UserEditPage.vue";
import SearchResultPage from "../pages/SearchResultPage.vue";
import UserLoginPage from "../pages/UserLoginPage.vue";
import TeamAddPage from "../pages/TeamAddPage.vue";
import TeamUpdatePage from "../pages/TeamUpdatePage.vue";
import UserUpdatePage from "../pages/UserUpdatePage.vue";
import UserTeamCreatePage from "../pages/UserTeamCreatePage.vue";
import UserTeamJoinPage from "../pages/UserTeamJoinPage.vue";

// 定义一些路由
//每个路由都需要映射到一个组件。我们后面再讨论嵌套路由。
const routes = [
    { path: '/', component: IndexPage },
    { path:'/team',component: TeamPage },
    { path: '/team/add',component: TeamAddPage },
    { path: '/team/update',component: TeamUpdatePage },
    { path: '/user',component: UserPage },
    { path: '/search',component: SearchPage },
    { path: '/user/list',component: SearchResultPage },
    { path: '/user/edit',component: UserEditPage },
    { path: '/user/login',component: UserLoginPage },
    { path: '/user/update',component: UserUpdatePage },
    { path: '/user/team/create',component: UserTeamCreatePage },
    { path: '/user/team/join',component: UserTeamJoinPage },

]

export default routes;