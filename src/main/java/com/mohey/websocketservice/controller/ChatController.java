package com.mohey.websocketservice.controller;import org.springframework.messaging.handler.annotation.MessageMapping;import org.springframework.messaging.simp.SimpMessageSendingOperations;import org.springframework.web.bind.annotation.CrossOrigin;import org.springframework.web.bind.annotation.RestController;import com.mohey.websocketservice.dto.ChatMessage;import com.mohey.websocketservice.service.ChatService;import lombok.RequiredArgsConstructor;import lombok.extern.slf4j.Slf4j;@RequiredArgsConstructor@RestController@Slf4jpublic class ChatController {	private final SimpMessageSendingOperations messagingTemplate;	private final ChatService chatService;	@CrossOrigin	@MessageMapping("/chat/message") //websocket "/pub/chat/message"로 들어오는 메시지 처리	public void message(ChatMessage message) {		//if(ChatMessage.MassageType.JOIN.equals(message.getType()))		//message.setMessage(message.getSender() + "님이 입장하셨습니다.");		if (message.getType().equals("message")) {			messagingTemplate.convertAndSend("/sub/chat/room/" + message.getRoomId(),				message); // /sub/chat/room/{roomId} - 구독		}		log.info(message.toString());		chatService.save(message);	}}