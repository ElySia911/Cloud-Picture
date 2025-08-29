package com.oy.oypicturebackend.exception.utils;

import java.awt.*;

/**
 * 工具类：计算颜色相似度，通过比较两个颜色的RGB值计算他们的接近程度
 */
public class ColorSimilarUtils {

    //私有的构造方法，外部 不能new这个类
    private ColorSimilarUtils() {
    }

    /**
     * 计算两个颜色的相似度
     *
     * @param color1 第一个颜色
     * @param color2 第二个颜色
     * @return 相似度（0到1之间，1为完全相同）
     */
    public static double calculateSimilarity(Color color1, Color color2) {
        //从颜色对象中提取R G B分量，范围都是0-255
        int r1 = color1.getRed();
        int g1 = color1.getGreen();
        int b1 = color1.getBlue();

        int r2 = color2.getRed();
        int g2 = color2.getGreen();
        int b2 = color2.getBlue();

        // 计算欧氏距离  (r1-r2)²+(g1-g2)²+(b1-b2)²的和开根号
        double distance = Math.sqrt(Math.pow(r1 - r2, 2) + Math.pow(g1 - g2, 2) + Math.pow(b1 - b2, 2));

        // distance是实机颜色之间的距离，值越大说明颜色差异越大。
        // Math.sqrt(3*Math.pow(255,2))是颜色之间最大可能的距离（红和青完全反色）
        // 用 1 - 实际距离  / 最大距离 就可以将距离转为”相似度“。越接近1越相似
        return 1 - distance / Math.sqrt(3 * Math.pow(255, 2));
    }

    /**
     * 根据十六进制颜色代码计算相似度（重载）
     *
     * @param hexColor1 第一个颜色的十六进制代码（如 0xFF0000）
     * @param hexColor2 第二个颜色的十六进制代码（如 0xFE0101）
     * @return 相似度（0到1之间，1为完全相同）
     */
    public static double calculateSimilarity(String hexColor1, String hexColor2) {
        //使用Color.decode方法讲十六进制转换成Color对象，然后复用第一个方法
        Color color1 = Color.decode(hexColor1);
        Color color2 = Color.decode(hexColor2);
        return calculateSimilarity(color1, color2);
    }

    // 示例代码
    public static void main(String[] args) {
        // 测试颜色
        Color color1 = Color.decode("0xFF0000");//红
        Color color2 = Color.decode("0xFE0101");//非常接近的红
        double similarity = calculateSimilarity(color1, color2);

        System.out.println("颜色相似度为：" + similarity);

        // 测试十六进制方法
        double hexSimilarity = calculateSimilarity("0xFF0000", "0xFE0101");
        System.out.println("十六进制颜色相似度为：" + hexSimilarity);
    }
}
