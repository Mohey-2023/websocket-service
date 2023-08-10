package com.mohey.websocketservice.dto;import org.springframework.data.annotation.Id;import org.springframework.data.mongodb.core.mapping.Document;import org.springframework.data.mongodb.core.mapping.Field;import lombok.AllArgsConstructor;import lombok.Data;import lombok.NoArgsConstructor;@Data@Document(collection = "chatting_Image")@NoArgsConstructor@AllArgsConstructorpublic class ChatImage {	@Id	@Field("location")	private String location; //url	@Field("bucket")	private String bucket; //버킷 이름	@Field("key")	private String key;	@Field("content_length")	private String contentLength;	@Field("sender_name")	private String senderName;	@Field("sender_uuid")	private String senderUuid;	@Field("group_uuid")	private String groupUuid;}