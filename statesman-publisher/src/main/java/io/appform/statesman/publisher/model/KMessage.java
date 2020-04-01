package io.appform.statesman.publisher.model;

import lombok.*;

import java.util.List;

/**
 * @author shashank.g
 */
@Data
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KMessage {
    private List<Msg> messages;
}
