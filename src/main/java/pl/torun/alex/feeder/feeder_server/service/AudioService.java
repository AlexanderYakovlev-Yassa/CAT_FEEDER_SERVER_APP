package pl.torun.alex.feeder.feeder_server.service;

import pl.torun.alex.feeder.feeder_server.dto.AudioPresetDto;

import java.util.List;

public interface AudioService {

    List<AudioPresetDto> getPresets();

    void playPreset(String presetId);

    void playVoice(byte[] audioData, String contentType);

    AudioPresetDto saveCustomPreset(String name, byte[] audioData, String contentType);

    void deleteCustomPreset(Long id);
}
