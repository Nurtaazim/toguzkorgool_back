package com.toguzkorgool.controller;

import com.toguzkorgool.dto.request.ChatMessage;
import com.toguzkorgool.dto.request.DrawOfferMessage;
import com.toguzkorgool.dto.request.DrawResponseMessage;
import com.toguzkorgool.dto.request.MoveMessage;
import com.toguzkorgool.dto.request.NewGameMessage;
import com.toguzkorgool.dto.request.NewGameResponseMessage;
import com.toguzkorgool.dto.request.ResignMessage;
import com.toguzkorgool.dto.response.DrawEventMessage;
import com.toguzkorgool.dto.response.ErrorMessage;
import com.toguzkorgool.dto.response.NewGameEventMessage;
import com.toguzkorgool.service.GameService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class GameWebSocketController {

    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    public GameWebSocketController(GameService gameService, SimpMessagingTemplate messagingTemplate) {
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/game.move")
    public void handleMove(MoveMessage message, Principal principal) {
        try {
            gameService.makeMove(message.roomId(), message.playerId(), message.holeIndex());
        } catch (Exception e) {
            sendErrorToUser(principal, e.getMessage());
        }
    }

    @MessageMapping("/game.draw.offer")
    public void handleDrawOffer(DrawOfferMessage message, Principal principal) {
        try {
            messagingTemplate.convertAndSend("/topic/game/" + message.roomId() + "/draw",
                    DrawEventMessage.offer(message.playerId()));
        } catch (Exception e) {
            sendErrorToUser(principal, e.getMessage());
        }
    }

    @MessageMapping("/game.draw.response")
    public void handleDrawResponse(DrawResponseMessage message, Principal principal) {
        try {
            messagingTemplate.convertAndSend("/topic/game/" + message.roomId() + "/draw",
                    DrawEventMessage.response(message.playerId(), message.accept()));

            if (message.accept()) {
                gameService.handleDrawAccepted(message.roomId());
            }
        } catch (Exception e) {
            sendErrorToUser(principal, e.getMessage());
        }
    }

    @MessageMapping("/game.resign")
    public void handleResign(ResignMessage message, Principal principal) {
        try {
            gameService.resign(message.roomId(), message.playerId());
        } catch (Exception e) {
            sendErrorToUser(principal, e.getMessage());
        }
    }

    @MessageMapping("/game.new")
    public void handleNewGameRequest(NewGameMessage message, Principal principal) {
        try {
            messagingTemplate.convertAndSend("/topic/game/" + message.roomId() + "/new",
                    NewGameEventMessage.request(message.playerId()));
        } catch (Exception e) {
            sendErrorToUser(principal, e.getMessage());
        }
    }

    @MessageMapping("/game.new.response")
    public void handleNewGameResponse(NewGameResponseMessage message, Principal principal) {
        try {
            messagingTemplate.convertAndSend("/topic/game/" + message.roomId() + "/new",
                    NewGameEventMessage.response(message.playerId(), message.accept()));

            if (message.accept()) {
                gameService.startNewGame(message.roomId());
            }
        } catch (Exception e) {
            sendErrorToUser(principal, e.getMessage());
        }
    }

    @MessageMapping("/game.chat")
    public void handleChat(ChatMessage message, Principal principal) {
        try {
            messagingTemplate.convertAndSend("/topic/game/" + message.roomId() + "/chat", message);
        } catch (Exception e) {
            sendErrorToUser(principal, e.getMessage());
        }
    }

    @MessageMapping("/game.ready")
    public void handleReady(NewGameMessage message, Principal principal) {
        // Ready signal acknowledged - game start is handled via REST
    }

    private void sendErrorToUser(Principal principal, String errorMessage) {
        if (principal != null) {
            messagingTemplate.convertAndSendToUser(
                    principal.getName(), "/queue/errors", new ErrorMessage(errorMessage));
        }
    }
}
