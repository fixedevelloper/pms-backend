package com.pms.hotel.channelmanager;

public record ChannelRoomMappingView(Long id, Long channelId, String channelName, Long roomId, String roomNumber, String externalRoomId) {
}
