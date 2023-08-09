package com.mohey.websocketservice.dto;import java.time.LocalDateTime;import org.springframework.data.annotation.CreatedDate;import org.springframework.data.annotation.Id;import org.springframework.data.mongodb.core.index.Indexed;import org.springframework.data.mongodb.core.mapping.Document;import org.springframework.data.mongodb.core.mapping.Field;import lombok.AccessLevel;import lombok.AllArgsConstructor;import lombok.Builder;import lombok.Data;import lombok.Getter;import lombok.NoArgsConstructor;import lombok.ToString;//@Getter@NoArgsConstructor(access = AccessLevel.PRIVATE)@AllArgsConstructor//@Builder//@ToString@Data@Document(collection = "chatting_message")	//(collection = "chatting_message")public class ChatMessage {	//@Id	//private int id;	@Field("group_id")	private String groupId;	@Field("sender_name")	private String senderName;	@Field("sender_uuid")	private String senderUuid;	@Field("type")	private String type;	@Field("message")	private String message;	@Field("image_url")	private String imageUrl;	// @Field("content_id")	// private String contenId;	@Field("send_time")	@Indexed	private LocalDateTime sendTime;}