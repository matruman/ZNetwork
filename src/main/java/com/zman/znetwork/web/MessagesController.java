package com.zman.znetwork.web;

import com.zman.znetwork.auth.UserHandler;
import com.zman.znetwork.models.messages.Message;
import com.zman.znetwork.models.messages.MessageDAO;
import com.zman.znetwork.models.users.AppUser;
import com.zman.znetwork.models.users.AppUserDAO;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class MessagesController {

    @Autowired
    private AppUserDAO appUserDAO;

    @Autowired
    private MessageDAO messageDAO;

    @GetMapping("chats")
    public String chats(Model model) {
        AppUser appUser = UserHandler.getAuthorizedUser().getAppUser();
        List<AppUser> users = messageDAO.getChatUsers(appUser.getId(), 0);
        model.addAttribute("users", users);
        model.addAttribute("appUser", appUser);
        return "chats";
    }

    @GetMapping("chat")
    public String chat(@RequestParam int id, Model model) {
        AppUser appUser = UserHandler.getAuthorizedUser().getAppUser();
        AppUser user = appUserDAO.getById(id);
        List<Message> messages = messageDAO.getMessagesForChat(appUser.getId(), id, 0);
        model.addAttribute("messages", messages)
                .addAttribute("title", user.getUsername())
                .addAttribute("action", "/chat")
                .addAttribute("id", id);
        return "chat";
    }

    @PostMapping("chat")
    public ResponseEntity<String> chatController(@RequestBody String payload) {
        AppUser user = UserHandler.getAuthorizedUser().getAppUser();
        JSONObject json = new JSONObject(payload);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalDateTime now = LocalDateTime.now();
        ResponseEntity.BodyBuilder bodyBuilder = ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON);
        JSONObject response = new JSONObject();
        List<Message> messages;
        String target = json.getString("target");
        if (target.equals("update")) {
            String str = json.getString("last").replaceAll("\\D", "");
            long last = Long.parseLong(str);
            messages = messageDAO.getMessagesForUpdate(user.getId(),
                    json.getInt("user_id"), last);
            JSONArray messagesArray = generateMessagesArray(messages);
            response.put("result", "ok");
            response.put("messages", messagesArray);
            return bodyBuilder.body(response.toString());
        } else if (target.equals("send")) {
            String text = json.getString("text");
            long id = messageDAO.insert(user.getId(), text, user.getUsername(), json.getInt("user_id"));
            response.put("result", "ok");
            response.put("message_id", id);
            response.put("message", generateMessage(new Message(id, user.getUsername(), text, formatter.format(now))));
            return bodyBuilder.body(response.toString());
        } else if (target.equals("load")) {
            messages = messageDAO.getMessagesForChat(user.getId(), json.getInt("user_id"), json.getInt("offset"));
            JSONArray messagesArray = generateMessagesArray(messages);
            response.put("result", "ok");
            response.put("messages", messagesArray);
            return bodyBuilder.body(response.toString());
        } else {
            response.put("result", "error");
            response.put("error", "Unknown target");
            return bodyBuilder.body(response.toString());
        }
    }

    public JSONObject generateMessage(Message message) {
        JSONObject jsonMessage = new JSONObject();
        jsonMessage.put("message_id", message.getId());
        jsonMessage.put("username", message.getUsername());
        jsonMessage.put("date", message.getDate());
        jsonMessage.put("text", message.getText());
        return jsonMessage;
    }

    public JSONArray generateMessagesArray(List<Message> messageList) {
        JSONArray messages = new JSONArray();
        for (Message message : messageList)
            messages.put(generateMessage(message));
        return messages;
    }
}
