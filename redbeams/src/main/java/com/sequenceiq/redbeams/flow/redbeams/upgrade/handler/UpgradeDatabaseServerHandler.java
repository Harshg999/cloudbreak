package com.sequenceiq.redbeams.flow.redbeams.upgrade.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.RedbeamsUpgradeFailedEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.UpgradeDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.UpgradeDatabaseServerSuccess;
import com.sequenceiq.redbeams.service.stack.DBStackService;

import reactor.bus.Event;

@Component
public class UpgradeDatabaseServerHandler extends ExceptionCatcherEventHandler<UpgradeDatabaseServerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeDatabaseServerHandler.class);

    @Inject
    private DBStackService dbStackService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpgradeDatabaseServerRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeDatabaseServerRequest> event) {
        RedbeamsUpgradeFailedEvent failure = new RedbeamsUpgradeFailedEvent(resourceId, e);
        LOGGER.warn("Error restoring the database server:", e);
        return failure;
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpgradeDatabaseServerRequest> handlerEvent) {
        Event<UpgradeDatabaseServerRequest> event = handlerEvent.getEvent();
        LOGGER.debug("Received event: {}", event);
        UpgradeDatabaseServerRequest request = event.getData();
        DBStack dbStack = dbStackService.getById(request.getResourceId());
        Selectable response;
        try {
            // TODO add the restore code

            response = new UpgradeDatabaseServerSuccess(request.getResourceId());
            LOGGER.debug("Successfully upgraded the database server {}", dbStack);
        } catch (Exception e) {
            response = new RedbeamsUpgradeFailedEvent(request.getResourceId(), e);
            LOGGER.warn("Error upgrading the database server {}:", dbStack, e);
        }

        return response;
    }

}