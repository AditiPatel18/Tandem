package com.tandem.tandem_backend.websocket;

import com.tandem.tandem_backend.dto.SyncOperationRequest;
import com.tandem.tandem_backend.dto.SyncOperationResponse;
import com.tandem.tandem_backend.service.SyncService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.json.JsonMapper;

@Component
public class SyncWebSocketHandler extends TextWebSocketHandler {

    private final JsonMapper jsonMapper;
    private final SyncService syncService;

    public SyncWebSocketHandler(
            JsonMapper jsonMapper,
            SyncService syncService
    ) {
        this.jsonMapper = jsonMapper;
        this.syncService = syncService;
    }

    @Override
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage message
    ) throws Exception {

        try {
            SyncOperationRequest request =
                    jsonMapper.readValue(
                            message.getPayload(),
                            SyncOperationRequest.class
                    );

            SyncOperationResponse response =
                    syncService.processOperation(request);

            String responseJson =
                    jsonMapper.writeValueAsString(response);

            session.sendMessage(
                    new TextMessage(responseJson)
            );

        } catch (Exception exception) {

            SyncOperationResponse errorResponse =
                    new SyncOperationResponse(
                            null,
                            false,
                            null,
                            exception.getMessage()
                    );

            session.sendMessage(
                    new TextMessage(
                            jsonMapper.writeValueAsString(errorResponse)
                    )
            );
        }
    }
}