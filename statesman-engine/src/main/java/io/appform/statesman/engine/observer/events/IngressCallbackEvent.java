package io.appform.statesman.engine.observer.events;

import io.appform.statesman.engine.observer.ObservableEvent;
import io.appform.statesman.engine.observer.ObservableEventType;
import io.appform.statesman.engine.observer.ObservableEventVisitor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class IngressCallbackEvent extends ObservableEvent {

    private String callbackType;
    private String ivrProvider;
    private String translatorId;
    private String queryString;
    private String bodyString;
    private String formDataString;
    private String errorMessage;
    private String workflowId;
    private boolean smEngineTriggered;

    public IngressCallbackEvent() {
        super(ObservableEventType.INGRESS_CALLBACK);
    }

    @Builder
    public IngressCallbackEvent(String callbackType,
                                String ivrProvider,
                                String queryString,
                                String formDataString,
                                String bodyString,
                                String translatorId,
                                String workflowId,
                                boolean smEngineTriggered,
                                String errorMessage) {
        this();
        this.callbackType = callbackType;
        this.ivrProvider = ivrProvider;
        this.translatorId = translatorId;
        this.queryString = queryString;
        this.formDataString = formDataString;
        this.bodyString = bodyString;
        this.workflowId = workflowId;
        this.smEngineTriggered = smEngineTriggered;
        this.errorMessage = errorMessage;
    }

    @Override
    public <T> T accept(ObservableEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

