import { PageHeaderWrapper } from '@ant-design/pro-components';
import React from 'react';

const Admin: React.FC = (props) => {
  const {children} = props;
  // const intl = useIntl();
  return (
    <PageHeaderWrapper content={'这个页面只有admin才有权限查看'}>
      {children}
    </PageHeaderWrapper>
  );
};

export default Admin;
