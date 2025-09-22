import Footer from '@/components/Footer';
import { login } from '@/services/ant-design-pro/api';
import { LockOutlined, UserOutlined } from '@ant-design/icons';
import React, { useState, useEffect } from 'react';
import { FormattedMessage, history, SelectLang, useIntl, useModel } from 'umi';
import styles from './index.less';
import { SYSTEM_LOGO } from '@/constants';
import { ProFormText, LoginForm } from '@ant-design/pro-components';
import { Alert, message, Tabs } from 'antd';

const LoginMessage: React.FC<{
  content: string;
}> = ({ content }) => (
  <Alert
    style={{
      marginBottom: 24,
    }}
    message={content}
    type="error"
    showIcon
  />
);

const Login: React.FC = () => {
  const [userLoginState, setUserLoginState] = useState<API.LoginResult>({});
  const [type, setType] = useState<string>('account');
  const { initialState, setInitialState } = useModel('@@initialState');
  const [autoLoginCredentials, setAutoLoginCredentials] = useState<{
    userAccount?: string;
    userPassword?: string;
  }>({});

  const intl = useIntl();

  // 检查URL参数，如果有注册传递过来的用户名密码，则自动填充并登录
  useEffect(() => {
    const urlParams = new URLSearchParams(window.location.search);
    const userAccount = urlParams.get('userAccount');
    const userPassword = urlParams.get('userPassword');

    if (userAccount && userPassword) {
      setAutoLoginCredentials({ userAccount, userPassword });
      // 延迟一下自动登录，让用户看到填充的信息
      setTimeout(() => {
        handleSubmit({ userAccount, userPassword, type: 'account' });
      }, 1000);
    }
  }, []);

  const fetchUserInfo = async () => {
    console.log('fetchUserInfo函数被调用');
    console.log('initialState:', initialState);
    console.log('fetchUserInfo方法存在:', !!initialState?.fetchUserInfo);

    const userInfo = await initialState?.fetchUserInfo?.();
    console.log('获取到的用户信息:', userInfo);

    if (userInfo) {
      await setInitialState((s) => ({
        ...s,
        currentUser: userInfo,
      }));
      console.log('用户状态已更新');
    } else {
      console.warn('未获取到用户信息');
    }
  };

  const handleSubmit = async (values: API.LoginParams) => {
    try {
      // 登录
      const user = await login({ ...values, type });
      console.log('登录API响应:', user);
      console.log('用户数据类型:', typeof user);

      if (user) {
        const defaultLoginSuccessMessage = intl.formatMessage({
          id: 'pages.login.success',
          defaultMessage: '登录成功！',
        });
        message.success(defaultLoginSuccessMessage);

        // 直接使用登录返回的用户数据，不调用可能失败的fetchUserInfo
        console.log('设置用户状态...');
        const currentUser = { ...user, access: 'Admin' }; // access 用于 admin 权限
        localStorage.setItem('currentUser', JSON.stringify(currentUser));
        await setInitialState((s) => ({
          ...s,
          currentUser,
        }));
        console.log('用户状态设置完成，准备跳转...');

        /** 此方法会跳转到 redirect 参数所在的位置 */
        if (!history) {
          console.error('history对象不存在');
          return;
        }
        const { query } = history.location;
        const { redirect } = query as { redirect: string };
        const targetPath = redirect || '/welcome';
        console.log('跳转到:', targetPath);
        history.push(targetPath);
        return;
      }
      // 如果失败去设置用户错误信息
      console.log('登录失败，用户数据:', user);
      setUserLoginState(user);
    } catch (error) {
      const defaultLoginFailureMessage = intl.formatMessage({
        id: 'pages.login.failure',
        defaultMessage: '登录失败，请重试！',
      });
      message.error(defaultLoginFailureMessage);
    }
  };
  const { status, type: loginType } = userLoginState;

  return (
    <div className={styles.container}>
      <div className={styles.lang} data-lang>
        {SelectLang && <SelectLang />}
      </div>
      <div className={styles.content}>
        <LoginForm
          logo={<img alt="logo" src={SYSTEM_LOGO} />}
          title="业务管理系统demo"
          subTitle={intl.formatMessage({
            id: 'pages.layouts.userLayout.title',
            defaultMessage: '业务管理系统demo',
          })}
          initialValues={{
            autoLogin: true,
            userAccount: autoLoginCredentials.userAccount,
            userPassword: autoLoginCredentials.userPassword,
          }}
          actions={[
            <FormattedMessage
              key="loginWith"
              id="pages.login.loginWith"
              defaultMessage="其他登录方式"
            />,
            <a
              key="register"
              style={{
                float: 'right',
              }}
              onClick={() => {
                history.push('/user/register');
              }}
            >
              <FormattedMessage id="pages.login.register" defaultMessage="注册账户" />
            </a>,
          ]}
          onFinish={async (values) => {
            await handleSubmit(values as API.LoginParams);
          }}
        >
          <Tabs
            activeKey={type}
            onChange={setType}
            centered
            items={[
              {
                key: 'account',
                label: intl.formatMessage({
                  id: 'pages.login.accountLogin.tab',
                  defaultMessage: '账户密码登录',
                }),
              },
            ]}
          />

          {status === 'error' && loginType === 'account' && (
            <LoginMessage
              content={intl.formatMessage({
                id: 'pages.login.accountLogin.errorMessage',
                defaultMessage: '账户或密码错误(admin/ant.design)',
              })}
            />
          )}

          {type === 'account' && (
            <>
              <ProFormText
                name="userAccount"
                fieldProps={{
                  size: 'large',
                  prefix: <UserOutlined className={styles.prefixIcon} />,
                }}
                placeholder={intl.formatMessage({
                  id: 'pages.login.userAccount.placeholder',
                  defaultMessage: '用户名: admin or user',
                })}
                rules={[
                  {
                    required: true,
                    message: (
                      <FormattedMessage
                        id="pages.login.userAccount.required"
                        defaultMessage="请输入用户名!"
                      />
                    ),
                  },
                ]}
              />
              <ProFormText.Password
                name="userPassword"
                fieldProps={{
                  size: 'large',
                  prefix: <LockOutlined className={styles.prefixIcon} />,
                }}
                placeholder={intl.formatMessage({
                  id: 'pages.login.userPassword.placeholder',
                  defaultMessage: '密码: ant.design',
                })}
                rules={[
                  {
                    required: true,
                    message: (
                      <FormattedMessage
                        id="pages.login.userPassword.required"
                        defaultMessage="请输入密码！"
                      />
                    ),
                  },
                ]}
              />
            </>
          )}
        </LoginForm>
      </div>
      <Footer />
    </div>
  );
};

export default Login;
