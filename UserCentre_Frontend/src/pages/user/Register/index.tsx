import Footer from '@/components/Footer';
import { register } from '@/services/ant-design-pro/api';
import { LockOutlined, UserOutlined } from '@ant-design/icons';
import React, { useState } from 'react';
import { FormattedMessage, history, SelectLang, useIntl } from 'umi';
import styles from './index.less';
import { SYSTEM_LOGO } from '@/constants';
import { ProFormText, LoginForm } from '@ant-design/pro-components';
import { message, Tabs } from 'antd';

const Register: React.FC = () => {
  // const [userRegister, setUserRegister] = useState<API.RegisterResult>({})];
  const [type, setType] = useState<string>('account');
  const intl = useIntl();

  // 表单提交
  const handleSubmit = async (values: API.RegisterParams) => {
    // 校验，把三个属性定义为一个value，更方便调用
    const { userPassword, checkPassword } = values;
    if (userPassword !== checkPassword) {
      message.error('密码不一致');
      return;
    }

    try {
      // 注册,后端接口返回BaseResponse<RegisterResult>
      const result = await register(values);
      console.log('注册API响应:', result);
      console.log('响应类型:', typeof result);
      console.log('响应内容:', JSON.stringify(result));

      // 根据BaseResponse结构判断成功
      // 检查是否有code字段且为0，或者有data字段且为有效值
      let isSuccess = false;
      let errorMessage = '注册失败，请重试！';

      if (result && typeof result === 'object') {
        // 如果返回的是BaseResponse格式
        if ('code' in result) {
          isSuccess = result.code === 0;
          errorMessage = result.message || result.description || errorMessage;
        }
        // 如果返回的是直接的用户ID（兼容旧版本）
        else if ('data' in result && typeof result.data === 'number' && result.data > 0) {
          isSuccess = true;
        }
      }
      // 如果直接返回数字ID（兼容处理）
      else if (typeof result === 'number' && result > 0) {
        isSuccess = true;
      }

      if (isSuccess) {
        const defaultLoginSuccessMessage = intl.formatMessage({
          id: 'pages.login.success',
          defaultMessage: '注册成功！',
        });
        message.success(defaultLoginSuccessMessage);

        if (!history) return;
        const { query } = history.location;
        const { redirect } = (query || {}) as { redirect?: string };

        // 将注册的用户名和密码传递给登录页面
        const loginParams = new URLSearchParams();
        loginParams.set('userAccount', values.userAccount as string);
        loginParams.set('userPassword', values.userPassword as string);
        if (redirect) {
          loginParams.set('redirect', redirect);
        }

        history.push(`/user/login?${loginParams.toString()}`);
        return;
      }
      throw new Error(result.description);
    } catch (error: any) {
      const defaultRegisterFailureMessage = intl.formatMessage({
        id: 'pages.register.failure',
        defaultMessage: '注册失败，请重试！',
      });
      message.error(error.message ?? defaultRegisterFailureMessage);
      // console.error('注册错误:', error);
    }
  };

  // const { status, type: loginType } = userLoginState;
  return (
    <div className={styles.container}>
      <div className={styles.lang} data-lang>
        {SelectLang && <SelectLang />}
      </div>
      <div className={styles.content}>
        <LoginForm
          // 调用submitText修改提交按钮文本
          submitter={{
            searchConfig: {
              submitText: '注册',
            },
          }}
          logo={<img alt="logo" src={SYSTEM_LOGO} />}
          title="业务管理系统demo"
          initialValues={{
            autoLogin: true,
          }}
          onFinish={async (values) => {
            await handleSubmit(values as API.RegisterParams);
          }}
        >
          <Tabs
            activeKey={type}
            onChange={setType}
            items={[
              {
                key: 'account',
                label: intl.formatMessage({
                  id: 'pages.register.accountRegister.tab',
                  defaultMessage: '账号密码注册',
                }),
              },
            ]}
          />

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
                  defaultMessage: '请输入账号',
                })}
                rules={[
                  {
                    required: true,
                    message: (
                      <FormattedMessage
                        id="pages.login.userAccount.required"
                        defaultMessage="请输入账号!"
                      />
                    ),
                  },
                ]}
              />

              <ProFormText.Password
                // 用户密码 userPassword
                name="userPassword"
                fieldProps={{
                  size: 'large',
                  prefix: <LockOutlined className={styles.prefixIcon} />,
                }}
                placeholder={intl.formatMessage({
                  id: 'pages.login.userPassword.placeholder',
                  defaultMessage: '请输入密码',
                })}
                rules={[
                  {
                    required: true,
                    message: <FormattedMessage
                      id="pages.register.userPassword.required"
                      defaultMessage="用户密码是必填项！"
                    />,
                  },
                  {
                    min: 8,
                    type: 'string',
                    message: '长度不能小于8位',
                  },
                ]}
              />

              <ProFormText.Password
                // 校验密码 checkPassword
                name="checkPassword"
                fieldProps={{
                  size: 'large',
                  prefix: <LockOutlined className={styles.prefixIcon} />,
                }}
                placeholder={intl.formatMessage({
                  id: 'pages.login.userPassword.placeholder',
                  defaultMessage: '请再次输入密码',
                })}
                rules={[
                  {
                    required: true,
                    message: (
                      <FormattedMessage
                        id="pages.login.userPassword.required"
                        defaultMessage="确认密码是必填项"
                      />
                    ),
                  },
                  {
                    min: 8,
                    type: 'string',
                    message: '长度不能小于8位',
                  },
                ]}
              />

              <ProFormText
                // 用户编程导航编号 planetCode
                name="planetCode"
                fieldProps={{
                  size: 'large',
                  prefix: <UserOutlined className={styles.prefixIcon} />,
                }}
                placeholder={intl.formatMessage({
                  id: 'pages.login.planetCode.placeholder',
                  defaultMessage: '请输入编程导航编号',
                })}
                rules={[
                  {
                    required: true,
                    message: <FormattedMessage
                      id="pages.register.planetCode.required"
                      defaultMessage="请输入编程导航编号!"
                    />,
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

export default Register;
