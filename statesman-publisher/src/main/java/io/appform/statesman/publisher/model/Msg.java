package io.appform.statesman.publisher.model;

import lombok.*;

/**
 * @author shashank.g
 */
@Data
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Msg {
    private String partitionKey;
    private Object message;
}
