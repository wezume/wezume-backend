package com.example.vprofile.ffmpeg;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.springframework.stereotype.Service;

@Service
public class FFmpegService {

    public void compressVideo(File inputFile, File outputFile) throws IOException, InterruptedException {
        String ffmpegPath = "/usr/bin/ffmpeg";
        // Use pre-resized watermark PNGs (already the exact target size) so FFmpeg loads
        // them at native resolution — no runtime downscale, maximum sharpness.
        // watermark_100.png = 100px wide for portrait 270x480 output (visible in thumbnails)
        // watermark_200.png = 200px wide for landscape 854x480 output
        // All videos are portrait only.
        String watermarkPath = "/home/wezume/htdocs/wezume.in/img/watermark_100.png";

        String[] command = {
            ffmpegPath,
            "-i", inputFile.getAbsolutePath(),
            "-i", watermarkPath,
            "-filter_complex",
                "[0:v]scale=270:480:force_original_aspect_ratio=decrease,pad=270:480:(ow-iw)/2:(oh-ih)/2,format=yuv420p[v];" +
                "[1:v]scale=70:-1,format=rgba[wm];" +
                "[v][wm]overlay=x=W-w-15:y=15[out]",
            "-map", "[out]",
            "-map", "0:a?",
            "-vcodec", "libx264",
            "-preset", "medium",
            "-crf", "26",
            "-acodec", "aac",
            "-b:a", "96k",
            "-r", "24",
            "-movflags", "+faststart",
            "-f", "mp4",
            "-y",
            outputFile.getAbsolutePath()
        };

        System.out.println("Running FFmpeg command: " + String.join(" ", command));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("FFMPEG: " + line);
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            System.err.println("FFmpeg full output:\n" + output);
            throw new IOException("FFmpeg process failed with exit code " + exitCode);
        }
    }

}
