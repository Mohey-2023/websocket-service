package com.mohey.websocketservice.service;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.mohey.websocketservice.dto.ChatImage;
import com.mohey.websocketservice.dto.ChatKafka;
import com.mohey.websocketservice.dto.ChatMember;
import com.mohey.websocketservice.dto.ChatMessage;
import com.mohey.websocketservice.dto.ChatRoom;
import com.mohey.websocketservice.dto.FrontRoom;
import com.mohey.websocketservice.dto.Group;
import com.mohey.websocketservice.dto.GroupMember;
import com.mohey.websocketservice.dto.Location;
import com.mohey.websocketservice.dto.ReceiveGroup;
import com.mohey.websocketservice.producer.ChatProducer;
import com.mohey.websocketservice.repository.ChatImageRepository;
import com.mohey.websocketservice.repository.ChatMemberRepository;
import com.mohey.websocketservice.repository.ChatMessageRepository;
import com.mohey.websocketservice.repository.ChatRoomRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

	//@Autowired
	private final ChatMessageRepository chatMessageRepository;
	private final ChatMemberRepository chatMemberRepository;
	private final ChatRoomRepository chatRoomRepository;
	private final ChatImageRepository chatImageRepository;
	private final MongoTemplate mongoTemplate;
	private final SimpMessageSendingOperations messagingTemplate;
	private final AmazonS3 amazonS3;

	private final int timeDiff = 9 * 60 * 60 * 1000;

	@Value("${cloud.aws.s3.bucket}/chat")
	private String bucket;
	private final ChatProducer chatProducer;

	// public String upload(MultipartFile multipartFile, String groupId) throws IOException {
	// 	String fileName = multipartFile.getOriginalFilename();
	// 	log.info(fileName);
	// 	String fileExtension = fileName.substring(fileName.lastIndexOf("."));
	// 	String imageUrl = groupId + "/" + UUID.randomUUID() + fileExtension;
	//
	// 	ObjectMetadata metadata = new ObjectMetadata();
	// 	metadata.setContentType(multipartFile.getContentType());
	// 	metadata.setContentLength(multipartFile.getSize());
	//
	// 	amazonS3.putObject(bucket, imageUrl, multipartFile.getInputStream(), metadata);
	// 	log.info(imageUrl);
	// 	return imageUrl;
	// }

	public String requestImage(String fileName, String imageType) {
		Instant expirationTime = Instant.now().plus(Duration.ofSeconds(30));
		GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, fileName)
			.withMethod(HttpMethod.PUT)
			.withExpiration(Date.from(expirationTime))
			.withContentType(imageType);

		URL presignedUrl = amazonS3.generatePresignedUrl(request);

		return presignedUrl.toString();
	}

	public void saveImage(String fileName, String userName, String userUuid, String groupUuid) throws IOException {
		// S3 객체 정보 조회
		GetObjectRequest getObjectRequest = new GetObjectRequest(bucket, fileName);
		S3Object s3Object = amazonS3.getObject(getObjectRequest);
		String imageUrl = s3Object.getObjectContent().getHttpRequest().getURI().toString();

		ChatMessage chatMessage = new ChatMessage();
		chatMessage.setGroupId(groupUuid);
		chatMessage.setImageUrl(imageUrl);
		chatMessage.setType("image");
		chatMessage.setMessage("[사진]");
		chatMessage.setSenderName(userName);
		chatMessage.setSenderUuid(userUuid);

		ChatImage chatImage = new ChatImage(chatMessage.getImageUrl(),
			bucket,
			fileName,
			Long.toString(s3Object.getObjectMetadata().getContentLength()),
			userName,
			userUuid,
			groupUuid);

		chatImageRepository.save(chatImage);

		broadcasting(chatMessage);

	}

	public void broadcasting(ChatMessage message) throws IOException { //받은 메시지 모두에게 보내주기

		// Date currentTime = new Date(new Date().getTime() + timeDiff);
		Date currentTime = new Date(new Date().getTime());

		message.setSendTime(currentTime);

		messagingTemplate.convertAndSend("/sub/chats/room/" + message.getGroupId(),
			message); // /sub/chats/room/{roomId} - 구독

		messageSave(message); //Mongo DB에 저장

		//if(ChatMessage.MassageType.JOIN.equals(message.getType()))
		//message.setMessage(message.getSender() + "님이 입장하셨습니다.");

		ChatRoom chatRoom = chatRoomRepository.findById(message.getGroupId()).orElse(null);

		List<String> members = chatRoom.getGroupMembers();
		List<GroupMember> groupMembers = new ArrayList<>(); //kafka에 담을 memberList

		for (String member : members) {
			ChatMember chatMember = chatMemberRepository.findById(member).orElse(null);
			GroupMember groupMember = new GroupMember(member, chatMember.getDeviceTokenList());
			groupMembers.add(groupMember);
		}

		ChatKafka chatKafka = new ChatKafka(message.getGroupId(),
			chatRoom.getGroupName(),
			message.getSenderUuid(),
			message.getSenderName(),
			message.getMessage(),
			message.getType(),
			message.getImageUrl(),
			groupMembers);

		log.info(chatKafka.toString());

		chatProducer.send("chat", chatKafka);
	}

	public void messageSave(ChatMessage message) { //메시지 저장
		try {
			chatMessageRepository.save(message);  //chatting_message collection에 저장
		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}

	public void shareLocation(Location location) { //받은 위치 모두에게 보내주기
		// log.info(location.toString());
		messagingTemplate.convertAndSend("/sub/location/room/" + location.getGroupUuid(),
			location); // /sub/location/room/{roomId} - 구독
	}

	public void connectTime(String userUuid, String groupUuid) { //마지막 접속 시간 update

		Query query = new Query(Criteria.where("memberUuid").is(userUuid));
		ChatMember chatMember = mongoTemplate.findOne(query, ChatMember.class);

		List<Group> groups = chatMember.getGroups();
		for (Group existingGroup : groups) {
			if (existingGroup.getGroupUuid().equals(groupUuid)) {
				// Date currentTime = new Date(new Date().getTime() + timeDiff);
				// Date currentTime = new Date(new Date().getTime() + timeDiff);
				Date currentTime = new Date(new Date().getTime());
				existingGroup.setConnect_time(currentTime);

				break;
			}
		}
		chatMember.setGroups(groups);
		mongoTemplate.save(chatMember); //chatting_member collection 저장
	}

	public void saveMember(ReceiveGroup receive) {
		Group group = new Group();
		group.setGroupUuid(receive.getGroupUuid());
		// Date currentTime = new Date(new Date().getTime() + timeDiff);
		// Date currentTime = new Date(new Date().getTime() + timeDiff);
		Date currentTime = new Date(new Date().getTime());

		group.setConnect_time(currentTime);

		ChatMember chatMember = chatMemberRepository.findById(receive.getMemberUuid()).orElse(null);

		if (chatMember == null) {
			chatMember = new ChatMember();
			chatMember.setMemberUuid(receive.getMemberUuid());
			chatMember.setGroups(new ArrayList<>());
		}
		chatMember.getGroups().add(group); //member의 groups에 새로운 group 추가
		chatMember.setDeviceTokenList(receive.getDeviceTokenList());
		chatMemberRepository.save(chatMember); //chatting_member collection 저장
	}

	public void create(ReceiveGroup receive) { //모임 생성
		try {
			//chatting_member collection에 저장
			saveMember(receive);

			List<String> groupMembers = new ArrayList<>();
			groupMembers.add(receive.getMemberUuid());
			ChatRoom chatRoom = new ChatRoom(receive.getGroupUuid(), receive.getGroupName(), receive.getGroupType(),
				groupMembers);
			chatRoomRepository.save(chatRoom); //chatting_room collection에 저장

		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}

	public void accept(ReceiveGroup receive) { //모임 가입
		try {
			//chatting_member collection에 저장
			saveMember(receive);

			//chatting_room collection에 저장
			ChatRoom chatRoom = chatRoomRepository.findById(receive.getGroupUuid()).orElse(null);

			List<String> groupMembers = chatRoom.getGroupMembers();

			// GroupMember groupMember = new GroupMember(receive.getMemberUuid(), receive.getDeviceTokenList());
			groupMembers.add(receive.getMemberUuid());

			ChatRoom newChatRoom = new ChatRoom(receive.getGroupUuid(), chatRoom.getGroupName(),
				chatRoom.getGroupType(), groupMembers);

			chatRoomRepository.save(newChatRoom); //chatting_room collection에 저장

		} catch (Exception e) {
			log.error(e.getMessage());
		}

	}

	public void modify(ReceiveGroup receive) { //모임 수정
		try {

			ChatRoom chatRoom = chatRoomRepository.findById(receive.getGroupUuid()).orElse(null);

			List<String> groupMembers = chatRoom.getGroupMembers();

			ChatRoom newChatRoom = new ChatRoom(receive.getGroupUuid(), receive.getGroupName(), receive.getGroupType(),
				groupMembers);

			chatRoomRepository.save(newChatRoom); //chatting_room collection에 저장
		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}

	public void exit(ReceiveGroup receive) { //모임 퇴장
		try {
			ChatMember chatMember = chatMemberRepository.findById(receive.getMemberUuid()).orElse(null);
			if (chatMember != null) {
				List<Group> groups = chatMember.getGroups();
				groups.removeIf(group -> group.getGroupUuid().equals(receive.getGroupUuid()));
				chatMember.setGroups(groups);
				chatMemberRepository.save(chatMember);
			}

			ChatRoom chatRoom = chatRoomRepository.findById(receive.getGroupUuid()).orElse(null);

			List<String> groupMembers = chatRoom.getGroupMembers();
			groupMembers.removeIf(member -> member.equals(receive.getMemberUuid()));
			chatRoom.setGroupMembers(groupMembers);

			chatRoomRepository.save(chatRoom); //chatting_room collection에 저장

		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}

	public List<FrontRoom> roomList(String memberUuid) { //모임 리스트 불러오기

		List<FrontRoom> frontRooms = new ArrayList<>();

		Query query = new Query(Criteria.where("memberUuid").is(memberUuid));
		ChatMember chatMember = mongoTemplate.findOne(query, ChatMember.class);

		List<Group> groups = chatMember.getGroups();
		for (Group group : groups) {
			FrontRoom frontRoom = new FrontRoom();
			String groupUuid = group.getGroupUuid();
			ChatRoom chatRoom = chatRoomRepository.findById(groupUuid).orElse(null);

			frontRoom.setGroupName(chatRoom.getGroupName());
			frontRoom.setGroupType(chatRoom.getGroupType());
			frontRoom.setGroupUuid(chatRoom.getGroupId());

			Query unreadQuery = new Query(Criteria.where("groupId").is(group.getGroupUuid())
				.and("sendTime").gt(group.getConnect_time()));

			frontRoom.setNoRead(mongoTemplate.count(unreadQuery, ChatMessage.class));

			Query lastQuery = new Query(Criteria.where("groupId").is(group.getGroupUuid()));
			lastQuery.with(PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "sendTime")));
			ChatMessage chat = mongoTemplate.findOne(lastQuery, ChatMessage.class);

			if (chat != null) {
				frontRoom.setLastMessage(chat.getMessage());
				frontRoom.setLastTime(chat.getSendTime());
			}

			frontRooms.add(frontRoom);

		}
		return frontRooms;
	}

	public List<ChatMessage> chatList(String groupId) { //모임 내용 불러오기

		Query query = new Query(Criteria.where("groupId").is(groupId));
		query.with(Sort.by(Sort.Direction.DESC, "sendTime"));
		List<ChatMessage> chatMessages = mongoTemplate.find(query, ChatMessage.class);

		return chatMessages;

	}

}