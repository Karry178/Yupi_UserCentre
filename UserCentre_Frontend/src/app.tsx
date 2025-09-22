import Footer from '@/components/Footer';
import RightContent from '@/components/RightContent';
import { BookOutlined, LinkOutlined } from '@ant-design/icons';
import type { Settings as LayoutSettings } from '@ant-design/pro-components';
import { PageLoading } from '@ant-design/pro-components';
import type { RunTimeLayoutConfig } from 'umi';
import { history, Link } from 'umi';
import defaultSettings from '../config/defaultSettings';
import { currentUser as queryCurrentUser } from './services/ant-design-pro/api';
import type { RequestConfig } from 'umi';

const isDev = process.env.NODE_ENV === 'development';
const loginPath = '/user/login';
// const whiteList =['/user/register',loginPath];

/** 获取用户信息比较慢的时候会展示一个 loading */
export const initialStateConfig = {
  loading: <PageLoading />,
};

// 运行时的自定义配置
export const request: RequestConfig = {
  timeout: 1000000,
  requestInterceptors: [
    (config: any) => {
      // 请求拦截器
      return config;
    },
  ],
  responseInterceptors: [
    (response: Response) => {
      // 响应拦截器 - 确保返回正确的Response对象
      return response;
    },
  ],
  errorConfig: {
    errorHandler: (error: any) => {
      console.warn('Request error:', error);
      // 处理错误但不阻塞
      return Promise.resolve(undefined);
    },
  },
};

/**
 * @see  https://umijs.org/zh-CN/plugins/plugin-initial-state
 * */
export async function getInitialState(): Promise<{
  settings?: Partial<LayoutSettings>;
  currentUser?: API.CurrentUser;
  loading?: boolean;
  fetchUserInfo?: () => Promise<API.CurrentUser | undefined>;
}> {
  const fetchUserInfo = async () => {
    try {
      return await queryCurrentUser();
    } catch (error) {
      console.warn('Failed to fetch user info:', error);
      return undefined;
    }
  };

  let currentUser: API.CurrentUser | undefined = undefined;
  const saved = localStorage.getItem('currentUser');
  if (saved) {
    try {
      currentUser = JSON.parse(saved);
    } catch {}
  }
  return {
    fetchUserInfo,
    settings: defaultSettings,
    currentUser,
  };
}

// ProLayout 支持的api https://procomponents.ant.design/components/layout
export const layout: RunTimeLayoutConfig = ({ initialState, setInitialState }) => {
  return {
    rightContentRender: () => <RightContent />,
    disableContentMargin: false,
    waterMarkProps: {
      content: initialState?.currentUser?.username,
    },
    footerRender: () => <Footer />,
    onPageChange: () => {
      const { location } = history;
      const publicRoutes = ['/user/login', '/user/register'];

      // initialState 还没准备好时不做重定向（避免白屏后回登录）
      if (!initialState) return;

      // 未登录且访问非公共页，才跳转登录
      if (!publicRoutes.includes(location.pathname) && !initialState.currentUser) {
        history.push(loginPath);
      }
    },
    links: isDev
      ? [
          <Link key="openapi" to="/umi/plugin/openapi" target="_blank">
            <LinkOutlined />
            <span>OpenAPI 文档</span>
          </Link>,
          <Link to="/~docs" key="docs">
            <BookOutlined />
            <span>业务组件文档</span>
          </Link>,
        ]
      : [],
    menuHeaderRender: undefined,
    // 暂时禁用 SettingDrawer 来测试是否是它导致的问题
    childrenRender: (children: React.ReactNode, props: any) => {
      return <>{children}</>;
    },
    ...initialState?.settings,
  };
};
