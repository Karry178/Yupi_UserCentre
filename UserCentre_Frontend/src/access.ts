/**
 * @see https://umijs.org/zh-CN/plugins/plugin-access
 * */
export default function access(initialState: { currentUser?: API.CurrentUser } | undefined) {
  const { currentUser } = initialState ?? {};
  return {
    // 这里按你的后端规范判断
    canAdmin: !!currentUser && currentUser.userRole === 1,
  };
}
