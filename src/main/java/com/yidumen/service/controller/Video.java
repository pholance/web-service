package com.yidumen.service.controller;

import com.yidumen.service.dao.RunSQL;
import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.TimeZone;

/**
 * @author 蔡迪旻
 *         2015年04月21日
 */
@RestController
public class Video {


    @Qualifier("dao")
    @Autowired
    private RunSQL dao;

    @RequestMapping(value = "/podcast", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> generatePodcastXML() {
        final Document document = DocumentHelper.createDocument();
        final Element rss = document.addElement("rss");
        rss.add(Namespace.get("itunes", "http://www.itunes.com/dtds/podcast-1.0.dtd"));
        rss.addAttribute("version", "2.0");
        final Element channel = rss.addElement("channel");
        channel.addElement("title").setText("易度门");
        channel.addElement("link").setText("http://www.yidumen.com/video/list");
        channel.addElement("copyright").setText("珠海玛雅文化传播有限公司 2012 版权所有 粤ICP备 12002739号");
        channel.addElement(QName.get("subtitle", rss.getNamespaceForPrefix("itunes"))).setText("易度门 - 聊天室");
        channel.addElement(QName.get("author", rss.getNamespaceForPrefix("itunes"))).setText("易度门");
        channel.addElement(QName.get("summary", rss.getNamespaceForPrefix("itunes"))).setText("杨宁老师佛学视频 在线观看 - 易度门 – 杨宁佛学视频 – 易度门");
        channel.addElement("description").setText("杨宁老师佛学视频 在线观看 - 易度门 – 杨宁佛学视频 – 易度门");
        final Element owner = channel.addElement(QName.get("owner", rss.getNamespaceForPrefix("itunes")));
        owner.addElement(QName.get("name", rss.getNamespaceForPrefix("itunes"))).setText("易度门");
        owner.addElement(QName.get("email", rss.getNamespaceForPrefix("itunes"))).setText("ianxi@yidumen.com");
        channel.addElement(QName.get("image", rss.getNamespaceForPrefix("itunes"))).setText("http://www.yidumen.com/resources/web/images/logo_rss.jpg");
        final Element category = channel.addElement(QName.get("category", rss.getNamespaceForPrefix("itunes")));
        category.addAttribute("text", "Religion &amp; Spirituality");
        category.addElement(QName.get("category", rss.getNamespaceForPrefix("itunes"))).addAttribute("text", "Buddhism");
        category.addElement(QName.get("category", rss.getNamespaceForPrefix("itunes"))).addAttribute("text", "Buddhist");
        channel.addElement(QName.get("category", rss.getNamespaceForPrefix("itunes"))).addAttribute("text", "TV &amp; Film");

        for (Map<String, Object> video : dao.findAllVideos()) {
            final Element item = channel.addElement("item");
            item.addElement("title").setText(video.get("title").toString());
            item.addElement(QName.get("author", rss.getNamespaceForPrefix("itunes"))).setText("易度门");
            item.addElement(QName.get("subtitle", rss.getNamespaceForPrefix("itunes"))).setText(video.get("title").toString());
            item.addElement(QName.get("summary", rss.getNamespaceForPrefix("itunes"))).setText("");
            item.addElement(QName.get("image", rss.getNamespaceForPrefix("itunes"))).setText("http://www.yidumen.com/resources/web/images/logo_rss.jpg");
            item.addElement("enclosure")
                    .addAttribute("url", "http://v3.yidumen.com/video_dl/360/" + video.get("file") + "_" + video.get("title") + "_360.mp4")
                    .addAttribute("length", "1000")
                    .addAttribute("type", "video/mp4");
            item.addElement("guid", "http://www.yidumen.com/video/" + video.get("file"));
            item.addElement("pubDate").setText(video.get("pubDate").toString());
            item.addElement(QName.get("duration", rss.getNamespaceForPrefix("itunes"))).setText("::0");

        }
        StringWriter out = new StringWriter();
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setEncoding("UTF-8");
        XMLWriter xmlWriter = new XMLWriter(out, format);
        try {
            xmlWriter.write(document);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ResponseEntity<String>(out.toString(), HttpStatus.OK);
    }
}
