package com.mohey.websocketservice.dto;import java.time.LocalDateTime;import java.util.List;import lombok.AccessLevel;import lombok.AllArgsConstructor;import lombok.Data;import lombok.NoArgsConstructor;@NoArgsConstructor@AllArgsConstructor@Datapublic class Group {	private String groupUuid;	private LocalDateTime connect_time;	private List<String> groupMembers;}