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
    private final String audioDir = System.getProperty("user.dir") + "/audio_analysis_temp";

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
        ProcessBuilder ffmpegBuilder = new ProcessBuilder("/usr/bin/ffmpeg", "-y", "-i", tempMp3Path,
                "-ar", "16000",
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
        java.util.List<String> attributes = new java.util.ArrayList<>();
        int pitchIndex = -1;
        int energyIndex = -1;
        int toneIndex = -1; // MFCC
        int voiceProbIndex = -1;
        int zcrIndex = -1;
        double voiceProb = 0, zcr = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(outputCsv))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("@attribute")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length > 1) {
                        attributes.add(parts[1]);
                    }
                    continue;
                }
                if (line.startsWith("@") || line.startsWith("name"))
                    continue;

                // Find indices dynamically if not found yet
                if (pitchIndex == -1 && !attributes.isEmpty()) {
                    for (int i = 0; i < attributes.size(); i++) {
                        String attr = attributes.get(i);
                        if (attr.contains("F0_sma_amean"))
                            pitchIndex = i;
                        else if (attr.contains("pcm_RMSenergy_sma_amean"))
                            energyIndex = i;
                        else if (attr.contains("pcm_fftMag_mfcc_sma[4]_amean"))
                            toneIndex = i;
                        else if (attr.contains("voiceProb_sma_amean"))
                            voiceProbIndex = i;
                        else if (attr.contains("pcm_zcr_sma_amean"))
                            zcrIndex = i;
                    }
                    // Fallbacks if exact names not found
                    if (energyIndex == -1)
                        energyIndex = 6; // Default fallback
                    if (toneIndex == -1)
                        toneIndex = 24;
                    if (pitchIndex == -1)
                        pitchIndex = attributes.size() - 2;
                    if (voiceProbIndex == -1)
                        voiceProbIndex = 14;
                    if (zcrIndex == -1)
                        zcrIndex = 13;
                }

                String[] parts = line.split(",");

                if (parts.length > 300) {
                    // Use dynamic indices.
                    // attributes.get(i) corresponds to parts[i].
                    // attributes[0] is 'name', parts[0] is instance name.

                    if (energyIndex != -1 && energyIndex < parts.length)
                        energy = Double.parseDouble(parts[energyIndex]);

                    if (toneIndex != -1 && toneIndex < parts.length)
                        tone = Double.parseDouble(parts[toneIndex]);

                    if (pitchIndex != -1 && pitchIndex < parts.length)
                        pitch = Double.parseDouble(parts[pitchIndex]);

                    if (voiceProbIndex != -1 && voiceProbIndex < parts.length)
                        voiceProb = Double.parseDouble(parts[voiceProbIndex]);

                    if (zcrIndex != -1 && zcrIndex < parts.length)
                        zcr = Double.parseDouble(parts[zcrIndex]);

                    // Emotion Proxy
                    emotion = (energy * 1000) + (pitch / 50);

                    System.out.println("Extracted - Pitch: " + pitch + ", Energy: " + energy + ", Tone: " + tone
                            + ", VoiceProb: " + voiceProb + ", ZCR: " + zcr
                            + ", Emotion Proxy: " + emotion);
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Normalize values (Revised based on logs)
        // Pitch: Normal speech F0 is 80-250Hz. Using lower base to tolerate deep
        // voices/noise.
        double normPitch = (pitch - 0) / (250 - 0);

        // Energy: RMS Energy usually 0.0 to 0.1
        double normEnergy = (energy - 0) / (0.05 - 0);

        // Tone: MFCC
        double normTone = (tone - (-20)) / (20 - (-20));

        // Emotion: Proxy range
        double normEmotion = (emotion - 0) / (10 - 0);

        System.out.println("Normalized - Pitch: " + normPitch + ", Energy: " + normEnergy + ", Tone: " + normTone
                + ", Emotion: " + normEmotion);

        // Initialize scores and define ideal values
        double pitchScore, energyScore, toneScore, emotionScore;

        // Ideal normalized values (based on midpoints of "good" ranges)
        double idealPitch = 0.55;
        double idealEnergy = 0.45;
        double idealTone = 0.55;
        double idealEmotion = 0.55;

        // Scoring Logic:
        // Score is 2.0 at idealValue.
        // For every 0.1 deviation (increase/decrease) from ideal, the score decreases
        // by 0.25.
        // Clamped between 1.0 and 2.0.

        pitchScore = Math.round(
                Math.max(1.0, Math.min(2.0, 2.0 - (Math.abs(normPitch - idealPitch) / 0.1) * 0.25)) * 10.0) / 10.0;
        energyScore = Math.round(
                Math.max(1.0, Math.min(2.0, 2.0 - (Math.abs(normEnergy - idealEnergy) / 0.1) * 0.25)) * 10.0) / 10.0;
        toneScore = Math.round(Math.max(1.0, Math.min(2.0, 2.0 - (Math.abs(normTone - idealTone) / 0.1) * 0.25)) * 10.0)
                / 10.0;
        emotionScore = Math.round(
                Math.max(1.0, Math.min(2.0, 2.0 - (Math.abs(normEmotion - idealEmotion) / 0.1) * 0.25)) * 10.0) / 10.0;

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

            if (totalWords > 140) {
                int extraWords = totalWords - 140;
                double penalty = Math.floor(extraWords / 20.0) * 0.3;
                rateScore -= penalty;
            }
            rateScore = Math.max(rateScore, 0.0);

            // --- Articulation Score (multi-factor) ---
            // Factor 1: Repeated consecutive words (stuttering / poor articulation)
            // e.g. "I I think" or "the the meeting"
            int stutterCount = 0;
            for (int i = 1; i < words.length; i++) {
                if (words[i].toLowerCase().equals(words[i - 1].toLowerCase())) {
                    stutterCount++;
                }
            }
            double stutterRatio = (double) stutterCount / totalWords;
            // Penalty: 0.3 per 5% stutter rate
            double stutterPenalty = Math.min(0.6, Math.floor(stutterRatio / 0.05) * 0.3);

            // Factor 2: Filler word density (dedicated articulation sensitivity)
            // Higher filler density = less articulate (separate from fillerScore)
            double articulationFillerPenalty = 0.0;
            if (fillerRatio > 0.05)
                articulationFillerPenalty = 0.2;
            if (fillerRatio > 0.12)
                articulationFillerPenalty = 0.4;

            // Factor 3: Vocabulary richness (Type-Token Ratio)
            // Low TTR means the speaker is repetitive and lacks lexical variety
            Set<String> uniqueWords = new HashSet<>();
            for (String w : words)
                uniqueWords.add(w.toLowerCase().replaceAll("[^a-z]", ""));
            uniqueWords.remove(""); // clean empty strings from punctuation stripping
            double ttr = (double) uniqueWords.size() / totalWords;
            // Good TTR for spontaneous speech is > 0.5. Penalise if low.
            double ttrPenalty = 0.0;
            if (ttr < 0.5)
                ttrPenalty = 0.2;
            if (ttr < 0.35)
                ttrPenalty = 0.4;

            // Factor 4: Average word length — very short avg (< 3.5 chars) suggests
            // fragmented, unclear speech; avg > 7 is fine (substantive vocabulary)
            double avgWordLen = Arrays.stream(words)
                    .mapToInt(String::length)
                    .average()
                    .orElse(5.0);
            double avgLenPenalty = 0.0;
            if (avgWordLen < 3.5)
                avgLenPenalty = 0.3; // very fragmented speech
            else if (avgWordLen < 4.5)
                avgLenPenalty = 0.1; // slightly fragmented

            // Factor 5: Acoustic Articulation (openSMILE)
            // Blending voicing probability and Zero Crossing Rate (consonant clarity)
            double normVoiceProb = Math.min(1.0, voiceProb / 0.8); // High voicing = clearer speech
            double normZCR = Math.min(1.0, zcr / 0.1); // Higher ZCR = sharper consonant formation
            double acousticArticulationBase = (normVoiceProb * 0.7) + (normZCR * 0.3);

            // Penalty for poor acoustic clarity
            double acousticPenalty = 0.0;
            if (acousticArticulationBase < 0.5)
                acousticPenalty = 0.4;
            else if (acousticArticulationBase < 0.7)
                acousticPenalty = 0.2;

            // Combine all articulation penalties (capped at 1.5 so score never <0.5 for
            // edge cases)
            double totalArticulationPenalty = Math.min(1.5,
                    stutterPenalty + articulationFillerPenalty + ttrPenalty + avgLenPenalty + acousticPenalty);
            articulationScore = Math.max(0.5, 2.0 - totalArticulationPenalty);

            System.out.println("Articulation - StutterRatio: " + stutterRatio
                    + ", FillerRatio: " + fillerRatio
                    + ", TTR: " + ttr
                    + ", AvgWordLen: " + avgWordLen
                    + ", AcousticBase: " + acousticArticulationBase
                    + " => Penalty: " + totalArticulationPenalty
                    + ", Score: " + articulationScore);
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
