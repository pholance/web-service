package com.yidumen.service;

import com.aliyun.openservices.oss.OSSClient;
import com.aliyun.openservices.oss.model.GetObjectRequest;
import com.aliyun.openservices.oss.model.OSSObject;
import com.aliyun.openservices.oss.model.ObjectMetadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.TimeZone;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 为部署在ACE上的资源服务而创建，目的是为了与之前的服务兼容
 *
 * @author 蔡迪旻 <yidumen.com>
 */
public final class ResourcesService extends HttpServlet {

    private final static Logger logger = LoggerFactory.getLogger(ResourcesService.class);
    private OSSClient client;

    @Override
    public void init() throws ServletException {
        super.init();
        client = new OSSClient("http://oss-internal.aliyuncs.com",
                "your-key",
                "your-secret");
    }

    @Override
    protected void doGet(final HttpServletRequest request,
                         final HttpServletResponse response)
            throws ServletException, IOException {

//        response.setContentType("application/octet-stream");
//        logger.debug("-------request headers---------");
//        for (Enumeration<String> e = request.getHeaderNames(); e.hasMoreElements();) {
//            String headerName = e.nextElement();
//            logger.debug("{}:{}", headerName, request.getHeader(headerName));
//        }
//        logger.debug("-------header end---------");

        final String path = request.getRequestURI().replaceFirst("/", "");
//        final StoreService service = StoreServiceFactory.getStoreService("yidumen");
//        if (service.isFileExist(path)) {

        //支持断点续传逻辑，否则观看视频时进度条无法拖动
        final GetObjectRequest objectRequest = new GetObjectRequest("yidumen", path);
        final String header = request.getHeader("Range");
        long start = 0;
        long end = -1;
        if (header != null && header.length() > 1) {
            final StringBuilder range = new StringBuilder(header);
            range.delete(0, 6);
            int split = range.indexOf("-");
            final String startStr = range.substring(0, split);
            start = startStr.isEmpty() ? -1 : Long.parseLong(startStr);
            final String endStr = range.substring(split + 1);
            end = endStr.isEmpty() ? -1 : Long.parseLong(endStr);
            if (start >= 0 || end >= 0) {
                objectRequest.setRange(start, end);
                logger.debug("set object range:{} - {}", start, end);
            }
            logger.debug("response 206 partial content");
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        }
        response.setHeader("Accept-Ranges", "bytes");
        final OSSObject object = client.getObject(objectRequest);
        final ObjectMetadata objectMetadata = object.getObjectMetadata();
        final Long contentLength = objectMetadata.getContentLength();
        logger.debug("contentLength is {}", contentLength);
        long objectLength = client.getObjectMetadata("yidumen", path).getContentLength();
        if (end < 0) {
            end = objectLength - 1L;
        }
        if (start < 0) {
            start = objectLength - end + 1L;
            end = objectLength - 1L;
        }
        final StringBuilder contentRange = new StringBuilder();
        contentRange.append("bytes ")
                .append(start)
                .append("-")
                .append(end)
                .append("/")
                .append(objectLength);
        response.setHeader("Content-Range", contentRange.toString());
        if (path.startsWith("video/")) {
            response.setContentType("video/mp4");
        } else {
            response.setContentType(objectMetadata.getContentType());
        }
        response.setContentLength(contentLength.intValue());

        final String eTag = objectMetadata.getETag();
        response.setHeader("ETag", "\"" + eTag + "\"");
        final long lastModified = objectMetadata.getLastModified().getTime();
//            final Long age = System.currentTimeMillis() / 1000 - lastModified / 1000;
//            response.setIntHeader("Age", age.intValue());
        response.setDateHeader("Last-Modified", lastModified);

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT-0"));
        cal.set(2020, 11, 31, 23, 59, 59);
        response.setDateHeader("Expires", cal.getTimeInMillis());

        //支持QuickTime
        if (request.getHeader("accept-encoding") != null) {
            response.setHeader("content-encoding", "identity");
        }

//            logger.debug("-------response headers----------");
//            Collection<String> headerNames = response.getHeaderNames();
//            for (String headerName : headerNames) {
//                logger.debug("{}:{}", headerName, response.getHeader(headerName));
//            }
//            logger.debug("----------response headers end-----------");
        OutputStream os = response.getOutputStream();
        InputStream is = object.getObjectContent();
        try {
            if (contentLength > Integer.MAX_VALUE) {
                IOUtils.copyLarge(is, os);
            } else {
                IOUtils.copy(is, os);
            }
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);
        }

//        } else {
//            response.sendError(HttpServletResponse.SC_NOT_FOUND);
//        }

        logger.debug("-------end-------------");
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    @Override
    protected long getLastModified(final HttpServletRequest req) {
        final String path = req.getRequestURI().replaceFirst("/", "");
        return client.getObjectMetadata("yidumen", path).getLastModified().getTime();
    }

}
