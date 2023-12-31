package com.mohey.websocketservice.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.mohey.websocketservice.dto.ChatImage;
import com.mohey.websocketservice.dto.ChatMessage;

import com.mohey.websocketservice.dto.FrontRoom;
import com.mohey.websocketservice.dto.Location;
import com.mohey.websocketservice.dto.ReceiveGroup;
import com.mohey.websocketservice.service.ChatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@RestController
@Slf4j
public class ChatController {

	// private final SimpMessageSendingOperations messagingTemplate;
	private final ChatService chatService;

	@CrossOrigin
	@MessageMapping("/chats/message") //websocket "/pub/chats/message"로 들어오는 메시지 처리
	public void message(ChatMessage message) throws IOException {
		chatService.broadcasting(message); //채팅방에 메시지 전송
	}


	@PostMapping("chats/image/request") // 이미지 전송 url 요청
	public ResponseEntity<String> requestImage(@RequestBody Map<String, String> payload) {
		String fileName = payload.get("fileName");
		String imageType = payload.get("imageType");
		log.info(fileName);
		log.info(imageType);

		try {
			String presignedUrl =chatService.requestImage(fileName, imageType);
			return ResponseEntity.ok(presignedUrl);
		} catch (Exception e) {
			log.error(e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error generating pre-signed URL");
		}
	}

	@PostMapping("chats/image") // S3 이미지 정보 전달
	public ResponseEntity<String> saveImage(@RequestBody Map<String, String> payload) throws IOException {
		String fileName = payload.get("fileName");
		String userName = payload.get("userName");
		String userUuid = payload.get("userUuid");
		String groupUuid = payload.get("groupUuid");
		chatService.saveImage(fileName, userName, userUuid, groupUuid);
		return new ResponseEntity<>(HttpStatus.OK);
	}


	@PostMapping("/location")
	public ResponseEntity<String> receiveLocation(@RequestBody Location location) { //위치 정보 받아오면 뿌려주기
		chatService.shareLocation(location);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@EventListener
	public void handleWebSocketConnectListener(SessionConnectEvent event) { //웹소켓이 연결될 때
		log.info("connection");

		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

		// 사용자 아이디 정보 얻기
		String userUuid  = accessor.getFirstNativeHeader("userUuid");
		String groupUuid  = accessor.getFirstNativeHeader("groupUuid");

		if (userUuid != null && groupUuid != null) { //header가 있으면(chat) 세션에 저장

			chatService.connectTime(userUuid, groupUuid);

			// 사용자 아이디 정보를 WebSocket 세션 속성에 저장
			accessor.getSessionAttributes().put("userUuid", userUuid);
			accessor.getSessionAttributes().put("groupUuid", groupUuid);
			// log.info(groupUuid);
			// log.info(userUuid);
		}
	}

	@EventListener
	public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) { //웹소켓 연결이 끊길 때
		log.info("disconnection");
		StompHeaderAccessor  accessor = StompHeaderAccessor.wrap(event.getMessage());

		String destination = accessor.getDestination(); // 구독 주소 가져오기

		String userUuid = (String) accessor.getSessionAttributes().get("userUuid");
		String groupUuid = (String) accessor.getSessionAttributes().get("groupUuid");

		if (userUuid != null && groupUuid != null) { //session에 저장되어 있으면(chat)
			chatService.connectTime(userUuid, groupUuid); //마지막 접속 시간 update

			// log.info(userUuid);
			// log.info(groupUuid);
		}

	}

	@PostMapping("/chats/create")
	public ResponseEntity<String> create(@RequestBody ReceiveGroup receive) { //모임 생성
		chatService.create(receive);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/chats/accept")
	public ResponseEntity<String> accept(@RequestBody ReceiveGroup receive) { //모임 가입
		chatService.accept(receive);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/chats/modify")
	public ResponseEntity<String> modify(@RequestBody ReceiveGroup receive) { //모임 정보 수정
		chatService.modify(receive);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/chats/exit")
	public ResponseEntity<String> exit(@RequestBody ReceiveGroup receive) { //모임 퇴장
		chatService.exit(receive);
		return new ResponseEntity<>(HttpStatus.OK);
	}

  	@GetMapping(value = "/chats/{member-id}", produces = "application/json;charset=UTF-8")
	public ResponseEntity<List<FrontRoom>> roomList(@PathVariable("member-id") String memberUuid)  { //모임 목록 가져오기
		List<FrontRoom> frontRooms = chatService.roomList(memberUuid);
		return new ResponseEntity<>(frontRooms, HttpStatus.OK);
	}

	@GetMapping(value ="/chats/messages/{chat-id}", produces = "application/json;charset=UTF-8")
	public ResponseEntity<List<ChatMessage>> chatList(@PathVariable("chat-id") String groupId)  { //채팅 내용 가져오기
		List<ChatMessage> chatMessages = chatService.chatList(groupId);
		return new ResponseEntity<>(chatMessages, HttpStatus.OK);
	}

	// @PostMapping("/image") //포스트맨 테스트용 이미지 업로드
	// public ResponseEntity<String> receiveLocation(@RequestBody MultipartFile image) throws IOException {
	// 	chatService.upload(image,"123");
	// 	return new ResponseEntity<>(HttpStatus.OK);
	// }

}
