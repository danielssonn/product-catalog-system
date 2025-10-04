package com.bank.product.workflow.temporal.activity;

import com.bank.product.events.WorkflowCompletedEvent;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Temporal activity for publishing events to Kafka
 * Activities can be retried by Temporal if they fail
 */
@ActivityInterface
public interface EventPublisherActivity {

    /**
     * Publish WorkflowCompletedEvent to Kafka
     * Temporal will retry this if it fails (based on retry policy)
     */
    @ActivityMethod
    void publishWorkflowCompletedEvent(WorkflowCompletedEvent event);
}
