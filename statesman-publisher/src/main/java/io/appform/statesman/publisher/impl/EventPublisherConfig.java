package io.appform.statesman.publisher.impl;

import com.google.common.collect.Maps;
import io.appform.statesman.model.HttpClientConfiguration;
import lombok.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * @author shashank.g
 */
@Data
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EventPublisherConfig {

    @NotNull
    @Valid
    private HttpClientConfiguration httpClientConfiguration;

    //serviceLevel
    private String endpoint = "http://localhost:8080/events";
    private PublisherType type = PublisherType.sync;
    private String queuePath = "/tmp";
    private int batchSize = 50;





    //move it out
    public enum PublisherType {
        sync {
            @Override
            public <T> T visit(PublisherTypeVisitor<T> visitor) {
                return visitor.visitSync();
            }
        },
        queued {
            @Override
            public <T> T visit(PublisherTypeVisitor<T> visitor) {
                return visitor.visitQueued();
            }
        };

        public abstract <T> T visit(final PublisherTypeVisitor<T> visitor);

        /**
         * Visitor
         *
         * @param <T>
         */
        public interface PublisherTypeVisitor<T> {
            T visitSync();

            T visitQueued();
        }
    }
}
