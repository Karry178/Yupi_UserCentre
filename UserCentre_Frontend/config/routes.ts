export default [
  {
    path: '/user',
    layout: false,
    routes: [
      {
        name: 'register',
        path: '/user/register',
        component: './user/Register',
      },
      {
        name: 'login',
        path: '/user/login',
        component: './user/Login',
      },
      { component: './404' },
    ],
  },
  {
    path: '/welcome',
    name: 'welcome',
    icon: 'smile',
    component: './Welcome',
  },

  {
    path: '/admin',
    name: '管理页',
    icon: 'crown',
    access: 'canAdmin',
    routes: [
      {
        path: '/admin/sub-page',
        name: 'sub-page',
        icon: 'smile',
        component: './Welcome',
      },

      {
        path: '/admin/user-manage',
        name: '用户管理',
        icon: 'smile',
        component: './Admin/UserManage',
      },
      {
        component: './404',
      },
    ],
  },
  {
    name: 'list.table-list',
    icon: 'table',
    path: '/list',
    component: './TableList',
  },
  {
    path: '/',
    redirect: '/welcome',
  },
  {
    name: '分析页',
    icon: 'smile',
    path: '/yupi',
    component: './DashboardAnalysis',
  },
  {
    component: './404',
  },
];
