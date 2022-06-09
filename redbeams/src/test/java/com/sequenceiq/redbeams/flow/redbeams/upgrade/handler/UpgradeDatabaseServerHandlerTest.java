package com.sequenceiq.redbeams.flow.redbeams.upgrade.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.UpgradeDatabaseServerRequest;
import com.sequenceiq.redbeams.service.stack.DBStackService;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
public class UpgradeDatabaseServerHandlerTest {

    @Mock
    private DBStackService dbStackService;

    @InjectMocks
    private UpgradeDatabaseServerHandler underTest;

    @Test
    void testSelector() {
        assertEquals("UPGRADEDATABASESERVERREQUEST", underTest.selector());
    }

    @Test
    void testDoAccept() {
        HandlerEvent<UpgradeDatabaseServerRequest> event = getHandlerEvent();

        Selectable nextFlowStepSelector = underTest.doAccept(event);

        assertEquals("UPGRADEDATABASESERVERSUCCESS", nextFlowStepSelector.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        UpgradeDatabaseServerRequest upgradeDatabaseServerRequest = new UpgradeDatabaseServerRequest(null, null, null, null);

        Selectable defaultFailureEvent = underTest.defaultFailureEvent(1L, new RuntimeException(), Event.wrap(upgradeDatabaseServerRequest));

        assertEquals("REDBEAMSUPGRADEFAILEDEVENT", defaultFailureEvent.selector());
    }

    private HandlerEvent<UpgradeDatabaseServerRequest> getHandlerEvent() {
        UpgradeDatabaseServerRequest upgradeDatabaseServerRequest = new UpgradeDatabaseServerRequest(null, null, null, null);
        HandlerEvent<UpgradeDatabaseServerRequest> handlerEvent = mock(HandlerEvent.class);
        when(handlerEvent.getEvent()).thenReturn(Event.wrap(upgradeDatabaseServerRequest));
        return handlerEvent;
    }

}
