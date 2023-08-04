package com.mohey.websocketservice.controller;import org.springframework.http.HttpStatus;import org.springframework.http.ResponseEntity;import org.springframework.messaging.handler.annotation.MessageMapping;import org.springframework.messaging.simp.SimpMessageSendingOperations;import org.springframework.web.bind.annotation.CrossOrigin;import org.springframework.web.bind.annotation.PostMapping;import org.springframework.web.bind.annotation.RequestBody;import org.springframework.web.bind.annotation.RestController;import com.mohey.websocketservice.dto.ChatMember;import com.mohey.websocketservice.dto.ChatMessage;import com.mohey.websocketservice.dto.Group;import com.mohey.websocketservice.dto.ReceiveGroup;import com.mohey.websocketservice.service.ChatService;import lombok.RequiredArgsConstructor;import lombok.extern.slf4j.Slf4j;@RequiredArgsConstructor@RestController@Slf4jpublic class ChatController {	// private final SimpMessageSendingOperations messagingTemplate;	private final ChatService chatService;	@CrossOrigin	@MessageMapping("/chats/message") //websocket "/pub/chat/message"로 들어오는 메시지 처리	public void message(ChatMessage message) {		chatService.broadcasting(message);	}	@PostMapping("/chats/create")	public ResponseEntity<String> create(@RequestBody ReceiveGroup receive){		log.info(receive.toString());		chatService.create(receive);		return new ResponseEntity<>(HttpStatus.OK);	}}