package com.yupi.usercentre;

import com.yupi.usercentre.utils.GeoUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;


class GeoUtilsTest {

    @Test
    void testDistance_Beijing2Shanghai() {
        System.out.println("北京天安门:[116.397128, 39.916527]");
        System.out.println("上海人民广场：[121.473701, 31.230416]");

        double distance = GeoUtils.calculateDistance(
                116.397128, 39.916527,
                121.473701, 31.230416
        );

        // 验证：距离在1060 - 1075 km之间
        assertTrue(distance >= 1060 && distance <= 1075);
    }
}
