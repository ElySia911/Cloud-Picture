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
 * 获取相似图片列表的API（step 2）
 * 通过jsoup爬取相似图片页面的HTML，提取其中包含firstUrl的js脚本，并返回图片列表的页面地址
 */
@Slf4j
public class GetImageFirstUrlApi {

    public static String getImageFirstUrl(String url) {
        //初始化HTML文档对象，用于存储解析后的网页内容
        Document document = null;
        try {
            //使用Jsoup库连接目标URL，设置5秒超时，获取并解析HTML文档
            document = Jsoup.connect(url)
                    .timeout(5000)
                    .get();

            //从解析后的HTML中提取所有<script>标签的内容放进一个Elements集合
            Elements scriptElements = document.getElementsByTag("script");
            //遍历所有<script>元素
            for (Element script : scriptElements) {
                //.html是Jsoup提供的方法，获取当前<script>标签内的JS代码文本
                String scriptContent = script.html();
                //检查当前JS代码中是否包含"\"firstUrl\""（带双引号的firstUrl）。\"是表示引号的意思，即关键词是带引号的，并不是firstUrl
                if (scriptContent.contains("\"firstUrl\"")) {
                    //定义正则表达式，精准匹配 "firstUrl": "xxx" 格式的内容
                    /*正则说明： "firstUrl" → 匹配字段名（带双引号）
                     * \\s*:\\s* → 匹配冒号前后任意空白符（比如空格、制表符）
                     * \"(.*?)\" → 匹配带双引号的属性值，并捕获引号内的内容*/
                    Pattern pattern = Pattern.compile("\"firstUrl\"\\s*:\\s*\"(.*?)\"");

                    //用正则表达式去匹配当前script标签内的js文本
                    Matcher matcher = pattern.matcher(scriptContent);

                    //如果找到匹配的内容
                    if (matcher.find()) {
                        //通过group(1)提取到正则表达式中括号里的内容，即"firstUrl"的值
                        String firstUrl = matcher.group(1);
                        //处理JSON转义的斜杠（把\/还原成/，比如https:\/\/xxx → https://xxx）
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
        String url = "https://graph.baidu.com/s?card_key=&entrance=GENERAL&extUiData[isLogoShow]=1&f=all&isLogoShow=1&session_id=4226792212727440227&sign=126f577d58ea42ce005c301769953981&tpl_from=pc";
        String imageFirstUrl = getImageFirstUrl(url);
        System.out.println("成功获取相似图片页面的firstUrl：" + imageFirstUrl);
    }
}

//成功获取相似图片页面的firstUrl：https://graph.baidu.com/ajax/pcsimi?carousel=503&entrance=GENERAL&extUiData%5BisLogoShow%5D=1&inspire=general_pc&limit=30&next=2&render_type=card&session_id=4226792212727440227&sign=126f577d58ea42ce005c301769953981&tk=2eaf2&tpl_from=pc