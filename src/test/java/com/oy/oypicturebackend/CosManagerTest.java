package com.oy.oypicturebackend;

import com.oy.oypicturebackend.manager.CosManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class CosManagerTest {

    @Autowired
    private CosManager cosManager;

    @Test
    public void testGetImageMainColor() {
        String key = "public/1948825777511940098/2025-08-13_jKEkIwxiREwh0bHb.webp";

        String imageMainColor = cosManager.getImageMainColor(key);

        System.out.println("主色调是" + imageMainColor);

    }
}
