package com.pms.hotel.channelmanager.internal;

import com.pms.hotel.channelmanager.ChannelSummary;
import com.pms.hotel.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Un canal de distribution (OTA) vers lequel pousser la disponibilité/les tarifs — voir ChannelPushService. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "channels")
public class Channel extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "webhook_url", nullable = false)
    private String webhookUrl;

    @Column(name = "webhook_secret")
    private String webhookSecret;

    @Column(nullable = false)
    private boolean active = true;

    public ChannelSummary toSummary() {
        return new ChannelSummary(getId(), name, webhookUrl, webhookSecret != null && !webhookSecret.isBlank(), active);
    }
}
