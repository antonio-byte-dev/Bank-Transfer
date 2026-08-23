package com.antoniobytedev.transference.controller;

import com.antoniobytedev.transference.dto.TransferRequest;
import com.antoniobytedev.transference.dto.TransferResponse;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/transfers")
public class TransferController {

    private static final Logger LOG = LoggerFactory.getLogger(TransferController.class);

    // Must match the process id defined in your BPMN file (<bpmn:process id="...">)
    private static final String PROCESS_ID = "bank-transfer";

    private final CamundaClient camundaClient;

    public TransferController(CamundaClient camundaClient) {
        this.camundaClient = camundaClient;
    }

    @PostMapping
    public ResponseEntity<TransferResponse> startTransfer(@RequestBody TransferRequest request) {
        Map<String, Object> variables = Map.of(
                "fromAccount", request.fromAccount(),
                "toAccount", request.toAccount(),
                "amount", request.amount()
        );

        LOG.info("Starting transfer process for {}", request);

        ProcessInstanceEvent instance = camundaClient.newCreateInstanceCommand()
                .bpmnProcessId(PROCESS_ID)
                .latestVersion()
                .variables(variables)
                .send()
                .join();

        LOG.info("Started process instance {}", instance.getProcessInstanceKey());

        return ResponseEntity.ok(new TransferResponse(instance.getProcessInstanceKey(), "STARTED"));
    }
}