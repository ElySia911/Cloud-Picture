package com.oy.oypicturebackend.api.imagesearch.sub;

import com.oy.oypicturebackend.exception.BusinessException;
import com.oy.oypicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 获取相似图片列表页面地址（step 2）
 * 通过jsoup爬取相似图片页面的HTML，提取其中包含firstUrl的js脚本，并返回图片列表的页面地址
 */
@Slf4j
public class GetImageFirstUrlApi {

    public static String getImageFirstUrl(String url) {
        //声明一个Document类型变量（Jsoup提供的HTML文档对象）
        Document document = null;
        try {
            //使用Jsoup连接到指定的url，获取HTML内容
            document = Jsoup.connect(url)
                    .timeout(5000)
                    .get();

            //从HTML文档里，获取所有<script标签>放进一个Elements集合
            Elements scriptElements = document.getElementsByTag("script");
            //从集合中每次取出一个<script>元素命名为script，在循环体中使用它
            for (Element script : scriptElements) {

                //.html是Jsoup提供的方法，用来获取标签内部的html内容，即js代码
                String scriptContent = script.html();

                //判断字符串是否包含"firstUrl"关键词，\"是表示引号的意思，即关键词是带引号的，并不是firstUrl
                if (scriptContent.contains("\"firstUrl\"")) {

                    //正则表达式，从JSON字符串中提取"firstUrl"对应的值
                    Pattern pattern = Pattern.compile("\"firstUrl\"\\s*:\\s*\"(.*?)\"");

                    //用正则pattern匹配脚本内容scriptContent得到一个Matcher对象
                    Matcher matcher = pattern.matcher(scriptContent);

                    //查找第一个符合正则的内容，返回true
                    if (matcher.find()) {
                        //匹配成功就通过group(1)取到括号里的内容，即"firstUrl"的值
                        String firstUrl = matcher.group(1);
                        //因为JSON中斜杠是转义的\/,还原成正常的/
                        firstUrl = firstUrl.replace("\\/", "/");
                        return firstUrl;
                    }
                }
            }
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未找到 url");
        } catch (Exception e) {
            log.error("搜索失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜索失败");
        }
    }

    //测试上述方法
    public static void main(String[] args) {
        String url = "https://graph.baidu.com/s?card_key=&entrance=GENERAL&extUiData[isLogoShow]=1&f=all&isLogoShow=1&session_id=3245127942267188312&sign=126473120d8b9e1b954ac01755691510&tpl_from=pc";
        String imageFirstUrl = getImageFirstUrl(url);
        System.out.println("成功获取相似图片页面的firstUrl：" + imageFirstUrl);
    }
}
