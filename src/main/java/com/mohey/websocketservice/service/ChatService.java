package com.mohey.websocketservice.service;import java.time.LocalDateTime;import java.time.ZoneId;import java.util.ArrayList;import java.util.List;import org.springframework.data.domain.Page;import org.springframework.data.domain.PageRequest;import org.springframework.data.domain.Sort;import org.springframework.data.mongodb.core.MongoTemplate;import org.springframework.data.mongodb.core.query.Criteria;import org.springframework.data.mongodb.core.query.Query;import org.springframework.messaging.simp.SimpMessageSendingOperations;import org.springframework.stereotype.Service;import com.mohey.websocketservice.dto.ChatMember;import com.mohey.websocketservice.dto.ChatMessage;import com.mohey.websocketservice.dto.ChatRoom;import com.mohey.websocketservice.dto.FrontRoom;import com.mohey.websocketservice.dto.Group;import com.mohey.websocketservice.dto.Location;import com.mohey.websocketservice.dto.ReceiveGroup;import com.mohey.websocketservice.repository.ChatMemberRepository;import com.mohey.websocketservice.repository.ChatMessageRepository;import com.mohey.websocketservice.repository.ChatRoomRepository;import lombok.RequiredArgsConstructor;import lombok.extern.slf4j.Slf4j;@Service@RequiredArgsConstructor@Slf4jpublic class ChatService {	//@Autowired	private final ChatMessageRepository chatMessageRepository;	private final ChatMemberRepository chatMemberRepository;	private final ChatRoomRepository chatRoomRepository;	private final MongoTemplate mongoTemplate;	private final SimpMessageSendingOperations messagingTemplate;	// @Autowired	// public ChatService(ChatRepository chatRepository) {	// 	this.chatRepository = chatRepository;	// }	public void broadcasting(ChatMessage message) { //받은 메시지 모두에게 보내주기		LocalDateTime currentTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"));		message.setSendTime(currentTime);		//if(ChatMessage.MassageType.JOIN.equals(message.getType()))		//message.setMessage(message.getSender() + "님이 입장하셨습니다.");		if (message.getType().equals("message")) {			messagingTemplate.convertAndSend("/sub/chats/room/" + message.getGroupId(),				message); // /sub/chats/room/{roomId} - 구독		}		log.info(message.toString());		messageSave(message);	}	public void shareLocation(Location location) { //받은 위치 모두에게 보내주기		// log.info(location.toString());		messagingTemplate.convertAndSend("/sub/location/room/" + location.getGroupUuid(),			location); // /sub/location/room/{roomId} - 구독	}	public void messageSave(ChatMessage message) { //메시지 저장		try {			chatMessageRepository.save(message);  //chatting_message collection에 저장		} catch (Exception e) {			log.error(e.getMessage());		}	}	public void connectTime(String userUuid, String groupUuid) { //마지막 접속 시간 update		Query query = new Query(Criteria.where("memberUuid").is(userUuid));		ChatMember chatMember = mongoTemplate.findOne(query, ChatMember.class);		List<Group> groups = chatMember.getGroups();		for (Group existingGroup : groups) {			if (existingGroup.getGroupUuid().equals(groupUuid)) {				LocalDateTime currentTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"));				existingGroup.setConnect_time(currentTime);				break;			}		}		chatMember.setGroups(groups);		mongoTemplate.save(chatMember); //chatting_member collection 저장	}	public void create(ReceiveGroup receive) { //모임 생성		try {			accept(receive); //chatting_member collection에 저장			modify(receive); //chatting_room collection에 저장		} catch (Exception e) {			log.error(e.getMessage());		}	}	public void accept(ReceiveGroup receive) { //모임 가입		try {			Group group = new Group();			group.setGroupUuid(receive.getGroupUuid());			LocalDateTime currentTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"));			group.setConnect_time(currentTime);			Query query = new Query(Criteria.where("memberUuid").is(receive.getMemberUuid()));			ChatMember chatMember = mongoTemplate.findOne(query, ChatMember.class);			if (chatMember == null) {				chatMember = new ChatMember();				chatMember.setMemberUuid(receive.getMemberUuid());				chatMember.setGroups(new ArrayList<>());			}			chatMember.getGroups().add(group); //member의 groups에 새로운 group 추가			mongoTemplate.save(chatMember); //chatting_member collection 저장		} catch (Exception e) {			log.error(e.getMessage());		}	}	public void modify(ReceiveGroup receive) { //모임 수정		try {			ChatRoom chatRoom = new ChatRoom(receive.getGroupUuid(), receive.getGroupName(), receive.getGroupType());			mongoTemplate.save(chatRoom); //chatting_room collection에 저장		} catch (Exception e) {			log.error(e.getMessage());		}	}	public void exit(ReceiveGroup receive) { //모임 퇴장		ChatMember chatMember = chatMemberRepository.findById(receive.getMemberUuid()).orElse(null);		if (chatMember != null) {			List<Group> groups = chatMember.getGroups();			groups.removeIf(group -> group.getGroupUuid().equals(receive.getGroupUuid()));			chatMember.setGroups(groups);			chatMemberRepository.save(chatMember);		}	}	public List<FrontRoom> roomList(String memberUuid) { //모임 리스트 불러오기		List<FrontRoom> frontRooms = new ArrayList<>();		Query query = new Query(Criteria.where("memberUuid").is(memberUuid));		ChatMember chatMember = mongoTemplate.findOne(query, ChatMember.class);		List<Group> groups = chatMember.getGroups();		for (Group group: groups) {			FrontRoom frontRoom = new FrontRoom();			String groupUuid = group.getGroupUuid();			ChatRoom chatRoom = chatRoomRepository.findById(groupUuid).orElse(null);			frontRoom.setGroupName(chatRoom.getGroupName());			frontRoom.setGroupType(chatRoom.getGroupType());			frontRoom.setGroupUuid(chatRoom.getGroupId());			Query unreadQuery = new Query(Criteria.where("groupId").is(group.getGroupUuid())				.and("sendTime").gt(group.getConnect_time()));			frontRoom.setNoRead(mongoTemplate.count(unreadQuery, ChatMessage.class));			Query lastQuery = new Query(Criteria.where("groupId").is(group.getGroupUuid()));			lastQuery.with(PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "sendTime")));			ChatMessage chat = mongoTemplate.findOne(lastQuery, ChatMessage.class);			if (chat != null) {				frontRoom.setLastMessage(chat.getMessage());				frontRoom.setLastTime(chat.getSendTime());			}			frontRooms.add(frontRoom);		}		return frontRooms;	}	public List<ChatMessage> chatList(String groupId) { //모임 내용 불러오기		Query query = new Query(Criteria.where("groupId").is(groupId));		query.with(Sort.by(Sort.Direction.DESC,"sendTime"));		List<ChatMessage> chatMessages = mongoTemplate.find(query, ChatMessage.class);		return chatMessages;	}}