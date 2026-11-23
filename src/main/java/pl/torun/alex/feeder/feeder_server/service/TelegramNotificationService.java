package pl.torun.alex.feeder.feeder_server.service;

/**
 * Sends notifications to Telegram when a feeder's online/offline status changes.
 */
public interface TelegramNotificationService {

    /**
     * Notifies configured Telegram chats about a feeder connectivity state transition.
     *
     * @param deviceId feeder serial number (e.g. FEED001)
     * @param online   true if the feeder came online, false if it went offline
     * @param detail   human-readable context (e.g. RSSI or disconnect reason)
     */
    void notifyFeederStatusChanged(String deviceId, boolean online, String detail);
}
