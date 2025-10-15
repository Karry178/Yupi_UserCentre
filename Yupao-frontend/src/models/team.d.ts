/**
 * 队伍类别
 */
export type TeamType = {
    id: number;
    description: string;
    expireTime?: null;
    maxNum: number;
    name: string;
    password?: string;
    // todo 定义status的枚举值类型更规范
    status: number;
    userId: number;
    createTime?: Date;
    updateTime?: Date;

    createUser?: UserType;
    hasJoin?: boolean;  // 当前用户是否已加入
    hasJoinNum?: number;  // 已加入人数
};