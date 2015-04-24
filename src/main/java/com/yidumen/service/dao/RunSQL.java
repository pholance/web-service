package com.yidumen.service.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

/**
 * @author 蔡迪旻
 *         2015年04月22日
 */
public class RunSQL {


    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    public List<Map<String, Object>> findAllVideos() {
        return jdbcTemplate.queryForList("SELECT video.* FROM video JOIN tag_video ON video.id = tag_video.videos_id JOIN tag ON tag.id = tag_video.tags_id AND tag.tagname = '聊天室' WHERE video.status = 0 ORDER BY video.pubDate DESC");
    }
    
    public String getVideoTitle(String file) {
        return jdbcTemplate.queryForObject("SELECT title FROM video WHERE file = ?", String.class, file);
    }

}
