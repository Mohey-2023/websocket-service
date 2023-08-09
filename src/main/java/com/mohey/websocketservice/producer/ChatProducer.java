package com.mohey.websocketservice.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mohey.websocketservice.dto.ChatKafka;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class ChatProducer {

    private KafkaTemplate<String, String> kafkaTemplate;

    public ChatKafka send(String topic, ChatKafka chatKafka) {
        ObjectMapper mapper = new ObjectMapper();
        String jsonInString = "";
        try {
            jsonInString = mapper.writeValueAsString(chatKafka);
            log.info("json : " + jsonInString);
        } catch (JsonProcessingException ex) {
            ex.printStackTrace();
        }
        kafkaTemplate.send(topic, jsonInString);
        log.info("fcm 푸시 서비스에 데이터를 보냈습니다 : " + chatKafka);
        return chatKafka;
    }
}
