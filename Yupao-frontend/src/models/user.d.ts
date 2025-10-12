/**
 * 用户类别
 */
export type UserType = {
    id: number;
    username?: string;
    userAccount?: string;
    avatarUrl?: string;
    gender: number;
    phone?: string;
    email?: string;
    userStatus?: number;
    createTime?: Date;
    userRole?: number;
    planetCode?: string;
    tags: string[];
    profile?: string;
};