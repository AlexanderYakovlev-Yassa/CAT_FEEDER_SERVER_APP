package pl.torun.alex.feeder.feeder_server.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import pl.torun.alex.feeder.feeder_server.config.SpeakerProperties;
import pl.torun.alex.feeder.feeder_server.dto.AudioPresetDto;
import pl.torun.alex.feeder.feeder_server.entity.CustomAudio;
import pl.torun.alex.feeder.feeder_server.repository.CustomAudioRepository;
import pl.torun.alex.feeder.feeder_server.service.AudioService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioServiceImpl implements AudioService {

    private final SpeakerProperties speakerProperties;
    private final ResourceLoader resourceLoader;
    private final CustomAudioRepository customAudioRepository;

    private static final int BYTES_PER_SECOND = 44100 * 2 * 2; // 176,400 B/s (44.1 kHz, 16-bit, Stereo)
    private static final int CHUNK_SIZE = 4096;

    private static final List<AudioPresetDto> PRESETS = Arrays.asList(
            AudioPresetDto.builder()
                    .id("dinner_bell")
                    .name("Колокольчик (Иди кушать!)")
                    .description("Мелодичный призывный перезвон")
                    .icon("pi-bell")
                    .durationSeconds(3)
                    .custom(false)
                    .build(),
            AudioPresetDto.builder()
                    .id("bowl_clink")
                    .name("Звон миски")
                    .description("Звук насыпания корма в металлическую миску")
                    .icon("pi-shopping-bag")
                    .durationSeconds(4)
                    .custom(false)
                    .build(),
            AudioPresetDto.builder()
                    .id("cat_call")
                    .name("Кис-кис-кис")
                    .description("Призывные звуковые трели")
                    .icon("pi-heart")
                    .durationSeconds(3)
                    .custom(false)
                    .build(),
            AudioPresetDto.builder()
                    .id("attention_beep")
                    .name("Сигнал внимания")
                    .description("Двойной предупреждающий звуковой сигнал")
                    .icon("pi-exclamation-circle")
                    .durationSeconds(2)
                    .custom(false)
                    .build()
    );

    @Override
    public List<AudioPresetDto> getPresets() {
        List<AudioPresetDto> all = new ArrayList<>(PRESETS);
        try {
            List<CustomAudio> customAudios = customAudioRepository.findAllByOrderByCreatedAtDesc();
            for (CustomAudio c : customAudios) {
                all.add(AudioPresetDto.builder()
                        .id("custom_" + c.getId())
                        .name(c.getName())
                        .description("Пользовательское аудиосообщение")
                        .icon("pi-microphone")
                        .durationSeconds(c.getDurationSeconds())
                        .custom(true)
                        .build());
            }
        } catch (Exception e) {
            log.error("Failed to load custom audios: {}", e.getMessage(), e);
        }
        return all;
    }

    @Override
    public void playPreset(String presetId) {
        if (!speakerProperties.isEnabled()) {
            log.warn("Speaker service disabled by configuration");
            return;
        }

        if (presetId.startsWith("custom_")) {
            Long id = Long.parseLong(presetId.substring(7));
            CustomAudio customAudio = customAudioRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Custom audio not found: " + id));

            log.info("Playing custom audio '{}' ({} bytes, {} sec) to speaker {}:{}",
                    customAudio.getName(), customAudio.getAudioData().length, customAudio.getDurationSeconds(),
                    speakerProperties.getHost(), speakerProperties.getPort());

            try {
                streamPcmToSocket(new ByteArrayInputStream(customAudio.getAudioData()));
            } catch (Exception e) {
                log.error("Failed to stream custom audio '{}': {}", customAudio.getName(), e.getMessage(), e);
                throw new RuntimeException("Error streaming custom audio: " + e.getMessage(), e);
            }
            return;
        }

        String resourcePath = "classpath:audio/" + presetId + ".wav";
        Resource resource = resourceLoader.getResource(resourcePath);
        if (!resource.exists()) {
            throw new IllegalArgumentException("Audio preset not found: " + presetId);
        }

        log.info("Playing audio preset '{}' to speaker {}:{}", presetId,
                speakerProperties.getHost(), speakerProperties.getPort());

        try (InputStream is = resource.getInputStream()) {
            // Пропускаем 44 байта стандартного RIFF WAV заголовка, чтобы передать чистый PCM поток
            long skipped = is.skip(44);
            log.debug("Skipped WAV header ({} bytes)", skipped);
            streamPcmToSocket(is);
        } catch (Exception e) {
            log.error("Failed to stream preset '{}': {}", presetId, e.getMessage(), e);
            throw new RuntimeException("Error streaming audio preset: " + e.getMessage(), e);
        }
    }

    @Override
    public void playVoice(byte[] audioData, String contentType) {
        if (!speakerProperties.isEnabled()) {
            log.warn("Speaker service disabled by configuration");
            return;
        }

        if (audioData == null || audioData.length == 0) {
            throw new IllegalArgumentException("Voice audio data is empty");
        }

        log.info("Transcoding and streaming voice recording ({} bytes, content-type: {}) to speaker {}:{}",
                audioData.length, contentType, speakerProperties.getHost(), speakerProperties.getPort());

        try {
            byte[] pcmData = transcodeToPcm(audioData);
            streamPcmToSocket(new ByteArrayInputStream(pcmData));
            log.info("Voice stream successfully played!");
        } catch (Exception e) {
            log.error("Failed to stream voice recording: {}", e.getMessage(), e);
            throw new RuntimeException("Error streaming voice recording: " + e.getMessage(), e);
        }
    }

    @Override
    public AudioPresetDto saveCustomPreset(String name, byte[] audioData, String contentType) {
        if (audioData == null || audioData.length == 0) {
            throw new IllegalArgumentException("Audio data cannot be empty");
        }

        String displayName = (name != null && !name.trim().isEmpty()) ? name.trim() : "Голосовое сообщение";

        log.info("Saving new custom audio preset '{}' ({} bytes, content-type: {})", displayName, audioData.length, contentType);

        byte[] pcmData = transcodeToPcm(audioData);
        int durationSec = Math.max(1, pcmData.length / BYTES_PER_SECOND);

        CustomAudio customAudio = CustomAudio.builder()
                .name(displayName)
                .audioData(pcmData)
                .durationSeconds(durationSec)
                .build();

        customAudio = customAudioRepository.save(customAudio);
        log.info("Saved custom audio preset with id={}", customAudio.getId());

        return AudioPresetDto.builder()
                .id("custom_" + customAudio.getId())
                .name(customAudio.getName())
                .description("Пользовательское аудиосообщение")
                .icon("pi-microphone")
                .durationSeconds(durationSec)
                .custom(true)
                .build();
    }

    @Override
    public void deleteCustomPreset(Long id) {
        log.info("Deleting custom audio preset id={}", id);
        customAudioRepository.deleteById(id);
    }

    /**
     * Транскодирование входящих аудиоданных любого формата в стандартный PCM 44.1kHz Stereo s16le через FFmpeg
     */
    private byte[] transcodeToPcm(byte[] inputAudio) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    speakerProperties.getFfmpegPath(),
                    "-hide_banner",
                    "-loglevel", "error",
                    "-i", "pipe:0",
                    "-f", "s16le",
                    "-acodec", "pcm_s16le",
                    "-ac", "2",
                    "-ar", "44100",
                    "pipe:1"
            );

            Process process = pb.start();

            Thread writerThread = new Thread(() -> {
                try (OutputStream os = process.getOutputStream();
                     ByteArrayInputStream bais = new ByteArrayInputStream(inputAudio)) {
                    byte[] buf = new byte[CHUNK_SIZE];
                    int r;
                    while ((r = bais.read(buf)) != -1) {
                        os.write(buf, 0, r);
                    }
                    os.flush();
                } catch (Exception e) {
                    log.debug("FFmpeg stdin closed: {}", e.getMessage());
                }
            });
            writerThread.start();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (InputStream is = process.getInputStream()) {
                byte[] buf = new byte[CHUNK_SIZE];
                int r;
                while ((r = is.read(buf)) != -1) {
                    baos.write(buf, 0, r);
                }
            }

            process.waitFor();
            writerThread.join(1000);

            byte[] result = baos.toByteArray();
            if (result.length == 0) {
                throw new IllegalStateException("FFmpeg produced empty audio output");
            }
            return result;
        } catch (Exception e) {
            log.error("FFmpeg transcoding failed: {}", e.getMessage(), e);
            throw new RuntimeException("Transcoding error: " + e.getMessage(), e);
        }
    }

    /**
     * Потоковая передача PCM сэмплов в TCP-сокет роутера с контролем скорости (pace limiting)
     */
    private void streamPcmToSocket(InputStream pcmStream) throws Exception {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(speakerProperties.getHost(), speakerProperties.getPort()),
                    speakerProperties.getTimeoutMs());

            OutputStream os = socket.getOutputStream();
            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            long totalBytesSent = 0;
            long startTime = System.currentTimeMillis();

            while ((bytesRead = pcmStream.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
                totalBytesSent += bytesRead;

                // Контроль скорости: аудиопоток должен поступать точно в темпе реального воспроизведения
                long expectedElapsedMs = (totalBytesSent * 1000L) / BYTES_PER_SECOND;
                long actualElapsedMs = System.currentTimeMillis() - startTime;
                if (expectedElapsedMs > actualElapsedMs) {
                    Thread.sleep(expectedElapsedMs - actualElapsedMs);
                }
            }

            os.flush();
            log.info("Successfully sent {} bytes of PCM audio to speaker", totalBytesSent);
        }
    }
}
