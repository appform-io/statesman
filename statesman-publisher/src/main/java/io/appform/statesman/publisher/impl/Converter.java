package io.appform.statesman.publisher.impl;

import io.appform.statesman.publisher.model.Event;
import io.appform.statesman.publisher.model.KMessage;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author shashank.g
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Converter {

//    public static List<Event> toMessages(final List<Event> events){
//        return events.stream()
//                .map(event -> KMessage.builder()
//                        .partitionKey(event.getGroupingKey())
//                        .message(event.getEventData())
//                        .build())
//                .collect(Collectors.toList());
//    }
}
