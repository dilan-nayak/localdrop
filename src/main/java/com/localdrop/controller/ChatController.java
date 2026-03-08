package com.localdrop.controller;

import com.localdrop.model.ChatMessage;
import com.localdrop.service.ClipboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController
{
    private final ClipboardService clipboardService;

    public ChatController(ClipboardService clipboardService)
    {
        this.clipboardService = clipboardService;
    }

    @GetMapping("/history")
    public List<ChatMessage> getHistory()
    {
        return clipboardService.getMessageHistory();
    }

    @GetMapping("/sync")
    public List<ChatMessage> syncFrom(@RequestParam(name = "since", defaultValue = "0") long since)
    {
        return clipboardService.getMessagesSince(since);
    }
}
