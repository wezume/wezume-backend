package com.example.vprofile.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.vprofile.score.SpeechScore;
import com.example.vprofile.score.SpeechScoreRepository;
import com.example.vprofile.score.TotalScoreService;
import com.example.vprofile.videofolder.VideoRepository;

@Service
public class AudioAnalysisService {

    @Autowired
    private SpeechScoreRepository speechScoreRepository;

    @Autowired
    private TotalScoreService totalScoreService;

    @Autowired
    private VideoRepository videoRepository;

    private final String openSmileBin = "/usr/local/bin";
    private final String configFile = "/root/opensmile/config/is09-13/IS09_emotion.conf";
    private final String audioDir = "/home/wezume/htdocs/wezume.in/audio";

    public SpeechScore analyzeAudio(String audioUrl, Long videoId)
            throws IOException, InterruptedException {

        try {
            Files.createDirectories(Paths.get(audioDir));
            System.out.println("Audio directory created or already exists: " + audioDir);
        } catch (IOException e) {
            System.err.println("Failed to create audio directory: " + e.getMessage());
            e.printStackTrace();
        }

        String tempMp3Path = audioDir + "/downloaded_audio.mp3";
        String wavPath = audioDir + "/downloaded_audio.wav";
        String outputCsv = audioDir + "/smile_output.csv";

        // 1. Download the audio file
        try (InputStream in = new URL(audioUrl).openStream();
                FileOutputStream out = new FileOutputStream(tempMp3Path)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        // 2. Convert MP3 to WAV using FFmpeg
        ProcessBuilder ffmpegBuilder = new ProcessBuilder("/usr/bin/ffmpeg", "-y", "-i", tempMp3Path, "-ar", "16000",
                "-ac", "1", wavPath);
        ffmpegBuilder.redirectErrorStream(true);
        Process ffmpegProcess = ffmpegBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(ffmpegProcess.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        int ffmpegExitCode = ffmpegProcess.waitFor();
        if (ffmpegExitCode != 0) {
            throw new RuntimeException("FFmpeg conversion failed.");
        }

        // 3. Run openSMILE on WAV
        ProcessBuilder smileBuilder = new ProcessBuilder(
                openSmileBin + "/SMILExtract",
                "-C", configFile,
                "-I", wavPath,
                "-O", outputCsv);
        smileBuilder.redirectErrorStream(true);
        Process smileProcess = smileBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(smileProcess.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        int smileExitCode = smileProcess.waitFor();
        if (smileExitCode != 0) {
            throw new RuntimeException("openSMILE failed.");
        }

        // 4. Parse CSV output
        double pitch = 0, energy = 0, tone = 0, emotion = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(outputCsv))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("@") || line.startsWith("name"))
                    continue;
                String[] parts = line.split(",");
                if (parts.length > 30) {
                    pitch = Double.parseDouble(parts[10]);
                    energy = Double.parseDouble(parts[15]);
                    tone = Double.parseDouble(parts[20]);
                    emotion = Double.parseDouble(parts[30]);

                    System.out.println("Pitch: " + pitch);
                    System.out.println("Energy: " + energy);
                    System.out.println("Tone: " + tone);
                    System.out.println("Emotion: " + emotion);
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Normalize values (0 to 1 scale)
        double normPitch = (pitch - 0) / (6 - 0);
        // double normEnergy = (energy - 0) / (6 - 0);
        double normTone = (tone - (-10)) / (10 - (-10));
        // double normEmotion = (emotion - (-1)) / (1 - (-1));

        // Initialize scores
        double pitchScore = 1, energyScore = 1, toneScore = 1, emotionScore = 1;

        // Simple rules on normalized values:

        // Pitch (ideal ~ 0.4 to 0.75)
        if ((normPitch >=0.1 && normPitch < 0.2)) {
            pitchScore = 1.5;
        } else if (normPitch > 0.2 && normPitch < 0.3) {
            pitchScore = 1.75;
        } else if (normPitch > 0.3){
            pitchScore = 2;
        }else {
            pitchScore = 1;
        }

        // Energy: Ideal [0.4 - 0.7], Slightly off [0.3 - 0.4) or (0.7 - 0.8], Very off
        // <0.3 or >0.8
        if (energy >= 0.3 && energy < 0.4) {
            energyScore = 1.5;
        } else if (energy >= 0.4 && energy <= 5) {
            energyScore = 1.75;
        } else if (energy > 5) {
            energyScore = 2;
        }

        // Tone (ideal ~ 0.55 to 0.75)
         if ((normTone >= 0.1 && normTone < 0.3)) {
            toneScore = 1.5;
        } else if (normTone >0.3 && normTone < 0.5) {
            toneScore = 1.75;
        } else if ( normTone < 0.5) {
            toneScore = 2;
        }
         else {
            toneScore = 1;
        }

        // Emotion: Ideal [0.0 - 0.6], Slightly off [-0.5 - 0.0) or (0.6 - 0.8], Very
        // off <-0.5 or >0.8
        if (emotion >= 0.1 && emotion < 0.3) {
            emotionScore = 1.5;
        } else if (emotion >= 0.3 && emotion < 0.5) {
            emotionScore = 1.75;
        } else if (emotion >= 0.5) {
            emotionScore = 2;
        }

        System.out.println("Pitch Score: " + pitchScore);
        System.out.println("Energy Score: " + energyScore);
        System.out.println("Tone Score: " + toneScore);
        System.out.println("Emotion Score: " + emotionScore);

        // 6. Analyze transcript
        String transcript = "";
        try {
            transcript = videoRepository.findById(videoId)
                    .orElseThrow(() -> new RuntimeException("Video not found with ID: " + videoId))
                    .getTranscription();
        } catch (Exception e) {
            System.err.println("Transcript fetch failed: " + e.getMessage());
        }
        System.out.println("Transcript: " + transcript);
        double fillerScore = 2.0, rateScore = 2.0, articulationScore = 2.0;

        if (transcript != null && !transcript.isEmpty()) {
            String[] fillerWords = { "um", "uh", "like", "you know", "actually", "basically", "so", "well", "hmm" };
            Set<String> fillerSet = new HashSet<>(Arrays.asList(fillerWords));

            String[] words = transcript.split("\\s+");
            int totalWords = words.length;

            int fillerCount = 0;
            for (String word : words) {
                if (fillerSet.contains(word.toLowerCase())) {
                    fillerCount++;
                }
            }

            double fillerRatio = (double) fillerCount / totalWords;
            if (fillerRatio > 0.1)
                fillerScore -= 0.3;

            double durationInSeconds = 60.0;
            double speechRate = totalWords / durationInSeconds;
            if (speechRate < 1.0 || speechRate > 4.0)
                rateScore -= 0.3;

            if (totalWords > 160) {
                int extraWords = totalWords - 160;
                double penalty = Math.floor(extraWords / 20.0) * 0.3;
                rateScore -= penalty;
            }
            rateScore = Math.max(rateScore, 0.0);

            int longWords = (int) Arrays.stream(words).filter(w -> w.length() >= 10).count();
            double articulationRatio = (double) longWords / totalWords;
            if (articulationRatio < 0.15)
                articulationScore -= 0.3;
        }

        // 7. Save score
        SpeechScore score = new SpeechScore();
        score.setVideoId(videoId);
        score.setPitchScore(pitchScore);
        score.setEnergyScore(energyScore);
        score.setToneScore(toneScore);
        score.setEmotionScore(emotionScore);
        score.setFillerWordScore(fillerScore);
        score.setSpeechRateScore(rateScore);
        score.setSentenceStructureScore(0.0); // Placeholder
        score.setArticulationScore(articulationScore);

        double totalScore = pitchScore + energyScore + toneScore + emotionScore +
                fillerScore + rateScore + articulationScore;

        BigDecimal roundedScore = new BigDecimal(totalScore).setScale(1, RoundingMode.HALF_UP);
        score.setTotalScore(roundedScore.doubleValue());

        speechScoreRepository.save(score);

        totalScoreService.computeTotalScoreIfReady(videoId);

        // 8. Cleanup (optional)
        new File(tempMp3Path).delete();
        new File(wavPath).delete();
        new File(outputCsv).delete();

        return score;
    }

}
