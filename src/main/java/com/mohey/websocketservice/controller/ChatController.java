package com.mohey.websocketservice.controller;import java.util.List;import org.springframework.context.event.EventListener;import org.springframework.http.HttpStatus;import org.springframework.http.ResponseEntity;import org.springframework.messaging.handler.annotation.MessageMapping;import org.springframework.messaging.simp.stomp.StompHeaderAccessor;import org.springframework.web.bind.annotation.CrossOrigin;import org.springframework.web.bind.annotation.GetMapping;import org.springframework.web.bind.annotation.PathVariable;import org.springframework.web.bind.annotation.PostMapping;import org.springframework.web.bind.annotation.RequestBody;import org.springframework.web.bind.annotation.RestController;import org.springframework.web.socket.messaging.SessionConnectEvent;import org.springframework.web.socket.messaging.SessionDisconnectEvent;import com.mohey.websocketservice.dto.ChatMessage;import com.mohey.websocketservice.dto.FrontRoom;import com.mohey.websocketservice.dto.Location;import com.mohey.websocketservice.dto.ReceiveGroup;import com.mohey.websocketservice.service.ChatService;import lombok.RequiredArgsConstructor;import lombok.extern.slf4j.Slf4j;@RequiredArgsConstructor@RestController@Slf4jpublic class ChatController {	// private final SimpMessageSendingOperations messagingTemplate;	private final ChatService chatService;	@CrossOrigin	@MessageMapping("/chats/message") //websocket "/pub/chats/message"로 들어오는 메시지 처리	public void message(ChatMessage message) {		chatService.broadcasting(message);	}	@PostMapping("/location")	public ResponseEntity<String> receiveLocation(@RequestBody Location location) { //위치 정보 받아오면 뿌려주기		chatService.shareLocation(location);		return new ResponseEntity<>(HttpStatus.OK);	}	@EventListener	public void handleWebSocketConnectListener(SessionConnectEvent event) { //웹소켓이 연결될 때		log.info("connection");		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());		// 사용자 아이디 정보 얻기		String userUuid  = accessor.getFirstNativeHeader("userUuid");		String groupUuid  = accessor.getFirstNativeHeader("groupUuid");		if (userUuid != null && groupUuid != null) { //header가 있으면(chat) 세션에 저장			// 사용자 아이디 정보를 WebSocket 세션 속성에 저장			accessor.getSessionAttributes().put("userUuid", userUuid);			accessor.getSessionAttributes().put("groupUuid", groupUuid);			log.info(groupUuid);			log.info(userUuid);		}	}	@EventListener	public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) { //웹소켓 연결이 끊길 때		log.info("disconnection");		StompHeaderAccessor  accessor = StompHeaderAccessor.wrap(event.getMessage());		String destination = accessor.getDestination(); // 구독 주소 가져오기		String userUuid = (String) accessor.getSessionAttributes().get("userUuid");		String groupUuid = (String) accessor.getSessionAttributes().get("groupUuid");		if (userUuid != null && groupUuid != null) { //session에 저장되어 있으면(chat)			chatService.connectTime(userUuid, groupUuid); //마지막 접속 시간 update			log.info(userUuid);			log.info(groupUuid);		}	}	@PostMapping("/chats/create")	public ResponseEntity<String> create(@RequestBody ReceiveGroup receive) { //모임 생성		chatService.create(receive);		return new ResponseEntity<>(HttpStatus.OK);	}	@PostMapping("/chats/accept")	public ResponseEntity<String> accept(@RequestBody ReceiveGroup receive) { //모임 가입		chatService.accept(receive);		return new ResponseEntity<>(HttpStatus.OK);	}	@PostMapping("/chats/modify")	public ResponseEntity<String> modify(@RequestBody ReceiveGroup receive) { //모임 정보 수정		chatService.modify(receive);		return new ResponseEntity<>(HttpStatus.OK);	}	@PostMapping("/chats/exit")	public ResponseEntity<String> exit(@RequestBody ReceiveGroup receive) { //모임 퇴장		chatService.exit(receive);		return new ResponseEntity<>(HttpStatus.OK);	}	@GetMapping("/chats/{member-id}")	public ResponseEntity<List<FrontRoom>> roomList(@PathVariable("member-id") String memberUuid)  { //모임 목록 가져오기		List<FrontRoom> frontRooms = chatService.roomList(memberUuid);		return new ResponseEntity<>(frontRooms, HttpStatus.OK);	}	@GetMapping("/chats/messages/{chat-id}")	public ResponseEntity<List<ChatMessage>> chatList(@PathVariable("chat-id") String groupId)  { //채팅 내용 가져오기		List<ChatMessage> chatMessages = chatService.chatList(groupId);		return new ResponseEntity<>(chatMessages, HttpStatus.OK);	}}