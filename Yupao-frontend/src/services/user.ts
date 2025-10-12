import myAxios from "../plugins/myAxios.ts";
import {getCurrentUserState, setCurrentUserState} from "../states/user.ts";

// 将获取当前用户信息封装成函数，需要获取当前用户信息时，调用该函数即可
export const getCurrentUser = async () => {
    // 获取当前用户信息
    /*const currentUser = getCurrentUserState();
    // 判断用户是否存在
    if (currentUser){
        return currentUser;
    }*/

    // 如果用户不存在，则从服务器获取用户信息
    const res = await myAxios.get('/user/current')
    // 如果res.code是0，说明用户存在，才可以取到用户信息
    if (res.code === 0) {
        setCurrentUserState(res.data);
        return res.data;
    }
    return null;
}
