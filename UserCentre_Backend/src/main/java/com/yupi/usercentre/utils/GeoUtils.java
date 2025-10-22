package com.yupi.usercentre.utils;

/**
 * 地理位置工具类
 *
 * 实现 Haversine 公式 -> 计算两个经纬度之间的距离
 * 功能：
 * 1. 计算两点间的球面距离（Haversine 公式）
 * 2. 验证经纬度坐标的有效性
 * 3. 格式化距离展示
 *
 * 注意：
 * - 所有方法都是静态方法
 * - 无状态，线程安全
 * - 不依赖任何外部服务
 */
public class GeoUtils {

    // 私有构造函数，防止实例化
    private GeoUtils() {
        throw new UnsupportedOperationException("工具类不能被实例化");
    }

    // 定义常量，用于表示地球的半径
    private static final double EARTH_RADIUS = 6371.0;


    /**
     * 计算两点间的球面距离(Haversine公式) -> 即计算两点间经纬度距离
     * @param lon1 纬度1
     * @param lat1 经度1
     * @param lon2 纬度2
     * @param lat2 经度2
     * @return 距离，单位：千米
     */
    public static double calculateDistance(double lon1, double lat1, double lon2, double lat2) {

        // 1.验证坐标有效性
        if (!isValidCoordinate(lon1, lat1) || !isValidCoordinate(lon2, lat2)) {
            // 如果坐标无效，则返回-1
            return -1;
        }

        // 2.如果是同一个点，距离为0
        if (lon1 == lon2 && lat1 == lat2) {
            return 0;
        }

        // 3.计算 Haversine 公式前面工作 --> 角度转弧度, 弧度 = 角度 × π / 180
        double radLat1 = Math.toRadians(lat1);
        double radLon1 = Math.toRadians(lon1);
        double radLat2 = Math.toRadians(lat2);
        double radLon2 = Math.toRadians(lon2);

        // 4.计算纬度差和精度差
        double dLat = radLat2 - radLat1;
        double dLon = radLon2 - radLon1;

        // 5.计算半正矢（Haversine公式核心）
        // a = sin²(dLat/2) + cos(lat1) × cos(lat2) × sin²(dLon/2)
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(radLat1) * Math.cos(radLat2)
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        // 6.计算中心角 --> 弧度 = 2 × arcsin( √a )
        double c = 2 * Math.asin(Math.sqrt(a));

        // 7.计算距离 --> distance = R × c
        double distance = EARTH_RADIUS * c;

        // 8.格式化返回值，保留两位小数
        return Math.round(distance * 100) / 100.0;
    }


    /**
     * 验证经纬度坐标的有效性
     *
     * @param longitude
     * @param latitude
     * @return true:有效，false:无效
     */
    public static boolean isValidCoordinate(double longitude, double latitude) {

        // 1.检查是否为有效数字(不是NaN 或 无穷大), NaN不是数字
        if (Double.isNaN(longitude) || Double.isNaN(latitude)) {
            return false;
        }
        if (Double.isInfinite(longitude) || Double.isInfinite(latitude)) {
            return false;
        }

        // 2.检查经度范围：-180° ~ 180°
        if (longitude < -180 || longitude > 180) {
            return false;
        }

        // 3.检查纬度范围：-90° ~ 90°
        if (latitude < -90 || latitude > 90) {
            return false;
        }

        // 4.只有所有检查都过了，才返回true
        return true;
    }


    public static String formatDistance(double distanceKm) {
        // 1.参数校验
        if (distanceKm < 0) {
            return "未知距离";
        }

        // 2.小于1KM的自动返回xxx米
        if (distanceKm < 1) {
            int meters = (int) (distanceKm * 1000);
            return meters + "米";
        }

        // 3.大于1KM的返回xxxKM(保留一位小数)
        double roundedKm = Math.round(distanceKm * 10.0) / 10.0;
        return roundedKm + "千米";
    }
}
