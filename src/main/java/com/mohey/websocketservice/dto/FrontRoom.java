package com.mohey.websocketservice.dto;import java.time.LocalDateTime;import lombok.AccessLevel;import lombok.AllArgsConstructor;import lombok.Data;import lombok.NoArgsConstructor;@NoArgsConstructor@AllArgsConstructor@Datapublic class FrontRoom {	private String groupUuid;	private String groupName;	private String groupType;	private String lastMessage;	private LocalDateTime lastTime;	private Long noRead;}