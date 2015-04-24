package com.yidumen.service.controller;

import com.aliyun.openservices.oss.OSSClient;
import com.yidumen.service.dao.RunSQL;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;

/**
 * @author 蔡迪旻
 *         2015年04月21日
 */
@Controller
public class Video {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @Qualifier("dao")
    @Autowired
    private RunSQL dao;
    @Autowired
    private OSSClient ossClient;
    @Autowired
    private String bucket;


    @RequestMapping(value = "podcast", produces = MediaType.APPLICATION_XML_VALUE)
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

        for (final Map<String, Object> video : dao.findAllVideos()) {
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
            item.addElement("guid").setText("http://www.yidumen.com/video/" + video.get("file"));
            item.addElement("pubDate").setText(video.get("pubDate").toString());
            item.addElement(QName.get("duration", rss.getNamespaceForPrefix("itunes"))).setText("::0");

        }
        final StringWriter out = new StringWriter();
        final OutputFormat format = OutputFormat.createPrettyPrint();
        format.setEncoding("UTF-8");
        final XMLWriter xmlWriter = new XMLWriter(out, format);
        try {
            xmlWriter.write(document);
        } catch (IOException e) {
            return new ResponseEntity<String>(e.getLocalizedMessage(), HttpStatus.OK);
        }
        return new ResponseEntity<String>(out.toString(), HttpStatus.OK);
    }

    @RequestMapping(value = "/video/{resolution}/{filename}.{ext}", produces = "video/mp4")
    @ResponseBody
    public void onlineVideo(@PathVariable final String resolution,
                            @PathVariable final String filename,
                            @PathVariable final  String ext,
                            final WebRequest request,
                            final HttpServletResponse response) throws IOException {
        final String url = new StringBuilder(ossClient.getEndpoint().toString()).append("/").append(bucket).append("/video/")
                .append(resolution).append("/").append(filename).append(".").append(ext).toString();
        LOG.debug(url);
        final HttpResponse httpResponse = getHttpResponse(request, response, url);
        response.setContentType("video/mp4");
        final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT-0"));
        cal.set(2020, 11, 31, 23, 59, 59);
        response.setDateHeader("Expires", cal.getTimeInMillis());
        final HttpEntity entity = httpResponse.getEntity();
        if (entity == null) {
            return;
        }
        entity.writeTo(response.getOutputStream());
    }


    @RequestMapping("/video_dl/{resolution}/{file}_{title}_{resolution}.{extand}")
    public void downloadVideo(@PathVariable String resolution,
                              @PathVariable String file,
                              @PathVariable final String extand,
                              WebRequest request,
                              HttpServletResponse response) throws IOException {
        final String url = new StringBuilder(ossClient.getEndpoint().toString()).append("/").append(bucket).append("/video/")
                .append(resolution).append("/").append(file).append("_").append(resolution).append(".").append(extand).toString();
        LOG.debug(url);
        final HttpResponse httpResponse = getHttpResponse(request, response, url);
        response.setContentType("application/octet-stream");
        final HttpEntity entity = httpResponse.getEntity();
        if (entity == null) {
            return;
        }
        entity.writeTo(response.getOutputStream());
    }

    private HttpResponse getHttpResponse(WebRequest request, HttpServletResponse response, String url) throws IOException {
        final HttpClient client = new DefaultHttpClient();
        final HttpGet httpGet = new HttpGet(url);
        final Iterator<String> headerNames = request.getHeaderNames();
        while (headerNames.hasNext()) {
            final String name = headerNames.next();
            httpGet.setHeader(name, request.getHeader(name));
        }
        final HttpResponse httpResponse = client.execute(httpGet);
        for (Header header : httpResponse.getAllHeaders()) {
            response.setHeader(header.getName(), header.getValue());
        }
        response.setStatus(httpResponse.getStatusLine().getStatusCode());
        return httpResponse;
    }

    @RequestMapping("/{dir}/{filename}.{ext}")
    public void otherResources(@PathVariable String dir,
                               @PathVariable String filename,
                               @PathVariable final  String ext,
                               WebRequest request,
                               HttpServletResponse response) throws IOException {
        final String url = new StringBuilder(ossClient.getEndpoint().toString()).append("/").append(bucket).append("/").append(dir).append("/").append(filename).append(".").append(ext).toString();
        LOG.debug(url);
        final HttpResponse httpResponse = getHttpResponse(request, response, url);
        final HttpEntity entity = httpResponse.getEntity();
        if (entity == null) {
            return;
        }
        entity.writeTo(response.getOutputStream());
    }

}
