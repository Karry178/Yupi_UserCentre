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
    userId: 0;
    createTime?: Date;
    updateTime?: Date;

    createUser?: UserType;
};