package io.appform.statesman.model.action.data.impl;

/**
 * @author shashank.g
 */
public enum HttpMethod {

    POST {
        @Override
        public <T> T visit(final MethodTypeVisitor<T> visitor) throws Exception {
            return visitor.visitPost();
        }
    },

    GET {
        @Override
        public <T> T visit(final MethodTypeVisitor<T> visitor) throws Exception {
            return visitor.visitGet();
        }
    };

    public abstract <T> T visit(final MethodTypeVisitor<T> visitor) throws Exception;

    /**
     * Visitor
     *
     * @param <T>
     */
    public interface MethodTypeVisitor<T> {
        T visitPost() throws Exception;

        T visitGet() throws Exception;
    }
}