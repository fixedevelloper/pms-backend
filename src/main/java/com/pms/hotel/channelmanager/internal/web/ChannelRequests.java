package com.pms.hotel.channelmanager.internal.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;

public final class ChannelRequests {

    private ChannelRequests() {
    }

    public record CreateChannelRequest(
            @NotBlank String name,
            @NotBlank @URL String webhookUrl,
            String webhookSecret) {
    }

    /** Tous les champs optionnels — seuls ceux présents sont appliqués. */
    public record UpdateChannelRequest(
            String name,
            @URL String webhookUrl,
            String webhookSecret,
            Boolean active) {
    }

    public record CreateMappingRequest(
            @NotNull Long roomId,
            @NotBlank String externalRoomId) {
    }
}
