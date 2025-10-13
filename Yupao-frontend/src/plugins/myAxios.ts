import axios from "axios";

/**
 * 创建一个实例
 * @type {axios.AxiosInstance}
 */
const myAxios = axios.create({
    baseURL: 'http://localhost:8080/api'
});

// 允许向后台发请求时携带cookie
myAxios.defaults.withCredentials = true;

// 添加请求拦截器
myAxios.interceptors.request.use(function (config) {
    console.log("我要发请求了！",config)
    // 在发送请求之前做些什么
    return config;
}, function (error) {
    // 对请求错误做些什么
    return Promise.reject(error);
});

// 添加全局响应拦截器
myAxios.interceptors.response.use(function (response) {
    console.log("我收到你的响应了！",response)
    // 2xx 范围内的状态码都会触发该函数。
    // 对响应数据做点什么,全部都返回其data

    // 40100:未登录，如果40100，则跳转到登录页面
    if (response?.data?.code === 40100){
        window.location.href = '/user/login';
    }
    return response.data;
}, function (error) {
    // 超出 2xx 范围的状态码都会触发该函数。
    // 对响应错误做点什么
    return Promise.reject(error);
});

export default myAxios;


