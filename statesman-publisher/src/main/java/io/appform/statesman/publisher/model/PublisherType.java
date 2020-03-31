package io.appform.statesman.publisher.model;

/**
 * @author shashank.g
 */
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