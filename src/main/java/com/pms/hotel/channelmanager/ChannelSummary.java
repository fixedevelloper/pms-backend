package com.pms.hotel.channelmanager;

/** webhookSecret n'est jamais renvoyé en clair — hasSecret indique seulement s'il est configuré. */
public record ChannelSummary(Long id, String name, String webhookUrl, boolean hasSecret, boolean active) {
}
