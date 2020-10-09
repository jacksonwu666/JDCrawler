package wuxiang.jd.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import wuxiang.jd.pojo.Item;
import wuxiang.jd.service.ItemService;
import wuxiang.jd.util.HttpUtils;

import javax.lang.model.util.Elements;
import javax.swing.text.Document;
import javax.swing.text.Element;
import java.util.Date;
import java.util.List;


@Component
public class ItemTask {

    @Autowired
    private HttpUtils httpUtils;
    @Autowired
    private ItemService itemService;

    public static final ObjectMapper MAPPER = new ObjectMapper();


    //设置定时任务执行完成后，再间隔100秒执行一次
    @Scheduled(fixedDelay = 1000 * 100)
    public void process() throws Exception {
        //分析页面发现访问的地址,页码page从1开始，下一页oage加2
        String url = "https://search.jd.com/Search?keyword=%E6%89%8B%E6%9C%BA&enc=utf-8&qrst=1&rt=1&stop=1&vt=2&cid2=653&cid3=655&s=5760&click=0&page=";

        //遍历执行，获取所有的数据
        for (int i = 1; i < 10; i = i + 2) {
            //发起请求进行访问，获取页面数据,先访问第一页
            String html = this.httpUtils.getHtml(url + i);

            //解析页面数据，保存数据到数据库中
            this.parseHtml(html);

        }
        System.out.println("执行完成");
    }


    //解析页面，并把数据保存到数据库中
    private void parseHtml(String html) throws Exception {
        //使用jsoup解析页面
        org.jsoup.nodes.Document doc = Jsoup.parse(html);

        //获取商品数据
        org.jsoup.select.Elements spuEles = doc.select("div#J_goodsList > ul > li");

        //遍历商品spu数据
        for (org.jsoup.nodes.Element spuEle : spuEles) {
            //获取商品spu
            long spuId = Long.parseLong(spuEle.attr("data-spu"));

            //获取商品sku数据
            org.jsoup.select.Elements skuEls = spuEle.select("li.ps-item img");
            for (org.jsoup.nodes.Element skuEle : skuEls) {
                //获取商品sku
                Long skuId = Long.parseLong(skuEle.attr("data-sku"));

                //判断商品是否被抓取过，可以根据sku判断
                Item param = new Item();
                param.setSku(skuId);
                List<Item> list = this.itemService.findAll(param);
                //判断是否查询到结果
                if (list.size() > 0) {
                    //如果有结果，表示商品已下载，进行下一次遍历
                    continue;
                }

                //保存商品数据，声明商品对象
                Item item = new Item();

                //商品spu
                item.setSpu(spuId);
                //商品sku
                item.setSku(skuId);
                //商品url地址
                item.setUrl("https://item.jd.com/" + skuId + ".html");
                //创建时间
                item.setCreated(new Date());
                //修改时间
                item.setUpdated(item.getCreated());


                //获取商品标题
                String itemHtml = this.httpUtils.getHtml(item.getUrl());
                String title = Jsoup.parse(itemHtml).select("div.sku-name").text();
                item.setTitle(title);

                //获取商品价格
                String priceUrl = "https://p.3.cn/prices/mgets?skuIds=J_"+skuId;
                String priceJson = this.httpUtils.getHtml(priceUrl);
                //解析json数据获取商品价格
                double price = MAPPER.readTree(priceJson).get(0).get("p").asDouble();
                item.setPrice(price);

                //获取图片地址
                String pic = "https:" + skuEle.attr("data-lazy-img").replace("/n9/","/n1/");
                System.out.println(pic);
                //下载图片
                String picName = this.httpUtils.getImage(pic);
                item.setPic(picName);

                //保存商品数据
                this.itemService.save(item);
            }
        }
    }
}
