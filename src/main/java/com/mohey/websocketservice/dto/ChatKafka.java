package com.mohey.websocketservice.dto;import java.util.List;import lombok.AllArgsConstructor;import lombok.Data;import lombok.NoArgsConstructor;@NoArgsConstructor@AllArgsConstructor@Datapublic class ChatKafka {	private String groupUuid;	private String groupName;	private String senderName;	private String message;	private String messageType;	private List<GroupMember> groupMembers;}