package com.zman.znetwork.models.messages;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;

@Repository
public class MessageDAO {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public MessageDAO () {

    }

    public ArrayList<Message> selectItems(String key, int value, int offset) {

        String sql = "SELECT message_id,parent_id,receiver,username,text,DATE_FORMAT(`date`, '%H:%i') FROM messages WHERE " + key
                + "=? ORDER BY message_id DESC LIMIT ?, 50";

        try {
            ArrayList<Message> items = (ArrayList) jdbcTemplate.query(sql, new Object[]{value, offset}, new MessageMapper());
            Collections.reverse(items);
            return items;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean insert (int parent_id, String text, String username, int receiver) {

        int update = jdbcTemplate.update("INSERT INTO messages(parent_id,receiver,text,username) VALUES(?,?,?,?)", parent_id, receiver, text, username);

        return update > 0;
    }
}
