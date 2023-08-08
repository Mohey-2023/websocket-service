package com.mohey.websocketservice.controller;import java.util.List;import org.springframework.context.event.EventListener;import org.springframework.http.HttpStatus;import org.springframework.http.ResponseEntity;import org.springframework.messaging.MessageHeaders;import org.springframework.messaging.handler.annotation.MessageMapping;import org.springframework.messaging.simp.SimpMessageSendingOperations;import org.springframework.messaging.simp.stomp.StompHeaderAccessor;import org.springframework.web.bind.annotation.CrossOrigin;import org.springframework.web.bind.annotation.GetMapping;import org.springframework.web.bind.annotation.PathVariable;import org.springframework.web.bind.annotation.PostMapping;import org.springframework.web.bind.annotation.RequestBody;import org.springframework.web.bind.annotation.RestController;import org.springframework.web.socket.messaging.SessionConnectEvent;import org.springframework.web.socket.messaging.SessionDisconnectEvent;import com.mohey.websocketservice.dto.ChatMessage;import com.mohey.websocketservice.dto.FrontRoom;import com.mohey.websocketservice.dto.Location;import com.mohey.websocketservice.dto.ReceiveGroup;import com.mohey.websocketservice.service.ChatService;import lombok.RequiredArgsConstructor;import lombok.extern.slf4j.Slf4j;@RequiredArgsConstructor@RestController@Slf4jpublic class ChatController {	// private final SimpMessageSendingOperations messagingTemplate;	private final ChatService chatService;	@CrossOrigin	@MessageMapping("/chats/message") //websocket "/pub/chats/message"로 들어오는 메시지 처리	public void message(ChatMessage message) {		chatService.broadcasting(message);	}	@PostMapping("/location")	public ResponseEntity<String> receiveLocation(@RequestBody Location location) {		chatService.shareLocation(location);		return new ResponseEntity<>(HttpStatus.OK);	}	@EventListener	public void handleWebSocketConnectListener(SessionConnectEvent event) {		log.info("connection");		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());		// 사용자 아이디 정보 얻기		String userUuid  = accessor.getFirstNativeHeader("userUuid");		String groupUuid  = accessor.getFirstNativeHeader("groupUuid");		if (userUuid != null && groupUuid != null) {			// 사용자 아이디 정보를 WebSocket 세션 속성에 저장			accessor.getSessionAttributes().put("userUuid", userUuid);			accessor.getSessionAttributes().put("groupUuid", groupUuid);			log.info(groupUuid);			log.info(userUuid);		}	}	@EventListener	public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {		log.info("disconnection");		StompHeaderAccessor  accessor = StompHeaderAccessor.wrap(event.getMessage());		String destination = accessor.getDestination(); // 구독 주소 가져오기		String userUuid = (String) accessor.getSessionAttributes().get("userUuid");		String groupUuid = (String) accessor.getSessionAttributes().get("groupUuid");		if (userUuid != null && groupUuid != null) {			chatService.connectTime(userUuid,groupUuid);			log.info(userUuid);			log.info(groupUuid);		}	}	@PostMapping("/chats/create")	public ResponseEntity<String> create(@RequestBody ReceiveGroup receive){		chatService.create(receive);		return new ResponseEntity<>(HttpStatus.OK);	}	@PostMapping("/chats/accept")	public ResponseEntity<String> accept(@RequestBody ReceiveGroup receive){		chatService.accept(receive);		return new ResponseEntity<>(HttpStatus.OK);	}	@PostMapping("/chats/modify")	public ResponseEntity<String> modify(@RequestBody ReceiveGroup receive){		chatService.modify(receive);		return new ResponseEntity<>(HttpStatus.OK);	}	@PostMapping("/chats/exit")	public ResponseEntity<String> exit(@RequestBody ReceiveGroup receive){		chatService.exit(receive);		return new ResponseEntity<>(HttpStatus.OK);	}	@GetMapping("/chats/{member-id}")	public ResponseEntity<List<FrontRoom>> roomList(@PathVariable("member-id") String memberUuid)  {		List<FrontRoom> frontRooms = chatService.roomList(memberUuid);		return new ResponseEntity<>(frontRooms, HttpStatus.OK);	}	@GetMapping("/chats/messages/{chat-id}")	public ResponseEntity<List<ChatMessage>> chatList(@PathVariable("chat-id") String groupId)  {		List<ChatMessage> chatMessages = chatService.chatList(groupId);		return new ResponseEntity<>(chatMessages, HttpStatus.OK);	}	@PostMapping("/chats/{member-id}")	public ResponseEntity<String> disconnection(@RequestBody ReceiveGroup receive){		chatService.exit(receive);		return new ResponseEntity<>(HttpStatus.OK);	}}