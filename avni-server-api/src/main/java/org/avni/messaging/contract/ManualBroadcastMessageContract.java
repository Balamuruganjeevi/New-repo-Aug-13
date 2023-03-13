package org.avni.messaging.contract;

import org.avni.messaging.domain.ReceiverType;
import org.joda.time.DateTime;

public class ManualBroadcastMessageContract {
    private String[] receiverIds;
    private ReceiverType receiverType;
    private String messageTemplateId;
    private String[] parameters;
    private DateTime scheduledDateTime;

    public String[] getReceiverIds() {
        return receiverIds;
    }

    public void setReceiverIds(String[] receiverIds) {
        this.receiverIds = receiverIds;
    }

    public String getMessageTemplateId() {
        return messageTemplateId;
    }

    public void setMessageTemplateId(String messageTemplateId) {
        this.messageTemplateId = messageTemplateId;
    }

    public String[] getParameters() {
        return parameters;
    }

    public void setParameters(String[] parameters) {
        this.parameters = parameters;
    }

    public DateTime getScheduledDateTime() {
        return scheduledDateTime;
    }

    public void setScheduledDateTime(DateTime scheduledDateTime) {
        this.scheduledDateTime = scheduledDateTime;
    }

    public ReceiverType getReceiverType() {
        return receiverType;
    }

    public void setReceiverType(ReceiverType receiverType) {
        this.receiverType = receiverType;
    }
}
