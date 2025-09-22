import type { ProColumns } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import { Space, Tag, message } from 'antd';
import { searchUsers } from '@/services/ant-design-pro/api';
import { useModel } from 'umi';

const columns: ProColumns<API.CurrentUser>[] = [
  {
    dataIndex: 'id',
    valueType: 'indexBorder',
    width: 48,
  },
  {
    title: '用户名',
    dataIndex: 'username',
    copyable: true,
    ellipsis: true,
  },
  {
    title: '头像',
    dataIndex: 'avatarUrl',
    copyable: true,
    render: (_, r) => <img src={r.avatarUrl} style={{ width: 32, height: 32, borderRadius: 16 }} />,
  },
  {
    title: '用户账号',
    dataIndex: 'userAccount',
    copyable: true,
  },
  {
    title: '性别',
    dataIndex: 'gender',
    copyable: true,
  },
  {
    title: '电话',
    dataIndex: 'phone',
    copyable: true,
  },
  {
    title: '邮箱',
    dataIndex: 'email',
    copyable: true,
  },
  {
    title: '用户状态',
    dataIndex: 'userStatus',
    copyable: true,
  },
  {
    title: '用户角色',
    dataIndex: 'userRole',
    copyable: true,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    copyable: true,
    valueType: 'dateTime',
  },

  {
    title: '角色',
    dataIndex: 'userRole',
    valueType: 'select',
    valueEnum: {
      0: { text: '普通用户', status: 'Default' },
      1: { text: '管理员', status: 'Success' },
    },
  },
  {
    title: '编程导航编号',
    dataIndex: 'planetCode',
    copyable: true,
    valueType: 'dateTime',
  },

  // {
  //   title: '用户账户',
  //   dataIndex: 'userAccount',
  //   filters: true,
  //   onFilter: true,
  //   valueType: 'select',
  //   formItemProps: {
  //     rules: [
  //       {
  //         required: true,
  //         message: '此项为必填项',
  //       },
  //     ],
  //   },
  //   valueEnum: {
  //     all: { text: '全部', status: 'Default' },
  //     open: {
  //       text: '未解决',
  //       status: 'Error',
  //     },
  //     closed: {
  //       text: '已解决',
  //       status: 'Success',
  //       disabled: true,
  //     },
  //     processing: {
  //       text: '解决中',
  //       status: 'Processing',
  //     },
  //   },
  // },

  // {
  //   title: '标签',
  //   dataIndex: 'labels',
  //   search: false,
  //   formItemProps: {
  //     rules: [
  //       {
  //         required: true,
  //         message: '此项为必填项',
  //       },
  //     ],
  //   },
  //   renderFormItem: (_, { defaultRender }) => {
  //     return defaultRender(_);
  //   },
  //   render: (_, record) => (
  //     <Space>
  //       {record.labels.map(({ name, color }) => (
  //         <Tag color={color} key={name}>
  //           {name}
  //         </Tag>
  //       ))}
  //     </Space>
  //   ),
  // },
];

export default () => {
  const { initialState } = useModel('@@initialState');
  const currentUser = initialState?.currentUser;

  // 检查管理员权限 (假设 userRole = 1 为管理员)
  if (!currentUser || currentUser.userRole !== 1) {
    message.error('您没有权限访问此页面');
    return <div>权限不足</div>;
  }

  return (
    <>
      <ProTable<API.CurrentUser>
        rowKey="id"
        columns={columns}
        request={async (params, sort, filter) => {
          console.log('Request params:', params, sort, filter);
          console.log('WARNING: If you see duplicate data being created, check your backend /api/user/search endpoint');
          try {
            const res = await searchUsers();
            console.log('searchUsers response:', res);

            // 检查响应数据结构
            if (Array.isArray(res)) {
              // 如果后端直接返回数组
              return {
                data: res,
                success: true,
                total: res.length
              };
            } else if (res && typeof res === 'object') {
              // 如果后端返回包含data字段的对象
              const list = res.data || res.records || [];
              const total = res.total || res.size || list.length;
              return {
                data: Array.isArray(list) ? list : [],
                success: true,
                total: total
              };
            } else {
              console.error('Unexpected response format:', res);
              return {
                data: [],
                success: false,
                total: 0
              };
            }
          } catch (error) {
            console.error('Failed to fetch users:', error);
            return {
              data: [],
              success: false,
              total: 0
            };
          }
        }}
        search={{
          labelWidth: 'auto',
        }}
        form={{
          ignoreRules: false,
        }}
        dateFormatter="string"
        headerTitle="高级表格"
      />
    </>
  );
};
