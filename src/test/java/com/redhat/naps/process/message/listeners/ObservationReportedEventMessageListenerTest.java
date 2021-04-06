package com.redhat.naps.process.message.listeners;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Map;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import org.hamcrest.CoreMatchers;
import org.jbpm.services.api.ProcessService;
import org.junit.Before;
import org.junit.Test;
import org.kie.internal.process.CorrelationKey;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Encounter;

public class ObservationReportedEventMessageListenerTest {

    private ObservationReportedEventMessageListener messageListener;

    @Mock
    private PlatformTransactionManager ptm;

    @Mock
    private TransactionStatus transactionStatus;

    @Mock
    private ProcessService processService;

    @Mock
    private Acknowledgment ack;

    @Captor
    private ArgumentCaptor<String> processIdCaptor;

    @Captor
    private ArgumentCaptor<CorrelationKey> correlationKeyCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> parametersCaptor;

    private final String processId = "pneumonia_patient_processing.Pneumonia_Case";

    @Before
    public void init() {
        initMocks(this);
        messageListener = new ObservationReportedEventMessageListener();
        setField(messageListener, null, ptm, PlatformTransactionManager.class);
        setField(messageListener, null, processService, ProcessService.class);
        setField(messageListener, "processId", processId, String.class);

        when(ptm.getTransaction(any())).thenReturn(transactionStatus);
        when(processService.startProcess(any(), any(), any(), any())).thenReturn(100L);
    }

    @Test
    public void testProcessIncidentReportedEventMessage() {
        String json = "{\"id\":\"incident123\"," +
                "\"lat\": \"34.14338\"," +
                "\"lon\": \"-77.86569\"," +
                "\"numberOfPeople\": 3," +
                "\"medicalNeeded\": true," +
                "\"timestamp\": 1521148332350," +
                "\"victimName\":\"John Doe\"," +
                "\"victimPhoneNumber\":\"111-222-333\"," +
                "\"status\":\"REPORTED\"" +
                "}";

        CloudEvent event = CloudEventBuilder.v1()
                .withId("000")
                .withType("ObservationReportedEvent")
                .withSource(URI.create("http://example.com"))
                .withDataContentType("application/json")
                .withData(json.getBytes())
                .build();

        messageListener.processMessage(event, "observation123", "topic1", 1, ack);
        
        verify(processService).startProcess(any(), processIdCaptor.capture(), correlationKeyCaptor.capture(), parametersCaptor.capture());
        assertThat(processIdCaptor.getValue(), equalTo(processId));
        CorrelationKey correlationKey = correlationKeyCaptor.getValue();
        assertThat(correlationKey.getName(), equalTo("observation123"));
        Map<String, Object> parameters = parametersCaptor.getValue();
        assertThat(parameters.size(), equalTo(1));
        verify(ack).acknowledge();
    }
}
