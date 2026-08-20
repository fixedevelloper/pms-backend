package com.pms.hotel.shared.web;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Limiteur de débit générique par clé (typiquement une IP), fenêtre
 * glissante en mémoire — pas de dépendance externe (Redis, Bucket4j...),
 * suffisant pour une seule instance. Sous attaque distribuée, le nombre de
 * clés distinctes peut croître ; {@link #evictStaleEntries()} borne la
 * mémoire en purgeant les clés inactives.
 */
@Component
public class RateLimiter {

    private final Map<String, ConcurrentLinkedDeque<Long>> hits = new ConcurrentHashMap<>();

    public boolean tryConsume(String key, int maxRequests, Duration window) {
        long now = System.currentTimeMillis();
        long windowStart = now - window.toMillis();
        ConcurrentLinkedDeque<Long> timestamps = hits.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxRequests) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }

    /** Purge active (pas seulement les clés déjà vidées par tryConsume) : une IP qui cesse d'appeler ne serait sinon jamais nettoyée. */
    @Scheduled(fixedRate = 10 * 60 * 1000)
    void evictStaleEntries() {
        long cutoff = System.currentTimeMillis() - Duration.ofMinutes(10).toMillis();
        hits.values().forEach(deque -> {
            synchronized (deque) {
                while (!deque.isEmpty() && deque.peekFirst() < cutoff) {
                    deque.pollFirst();
                }
            }
        });
        hits.values().removeIf(java.util.Deque::isEmpty);
    }
}
