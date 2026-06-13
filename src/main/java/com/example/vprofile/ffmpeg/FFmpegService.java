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
        String wmBase = "/home/wezume/htdocs/wezume.in/img/";

        int[] dims = getVideoDimensions(inputFile);
        boolean isPortrait = dims[1] > dims[0]; // height > width

        String watermarkPath = wmBase + (isPortrait ? "watermark_100.png" : "watermark_200.png");

        // Scale to fit within target dimensions without cropping, then pad with black to fill.
        // force_original_aspect_ratio=decrease avoids zooming in on non-9:16 inputs.
        String scaleAndPad = isPortrait
            ? "[0:v]scale=270:480:force_original_aspect_ratio=decrease,pad=270:480:(ow-iw)/2:(oh-ih)/2,format=yuv420p[v]"
            : "[0:v]scale=854:480:force_original_aspect_ratio=decrease,pad=854:480:(ow-iw)/2:(oh-ih)/2,format=yuv420p[v]";

        // Scale watermark to 70px wide for portrait, 150px for landscape — visible but not intrusive.
        String wmScale = isPortrait ? "scale=70:-1," : "scale=150:-1,";

        String[] command = {
            ffmpegPath,
            "-i", inputFile.getAbsolutePath(),
            "-i", watermarkPath,
            "-filter_complex",
                scaleAndPad + ";" +
                "[1:v]" + wmScale + "format=rgba[wm];" +
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

    private int[] getVideoDimensions(File videoFile) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
            "/usr/bin/ffprobe", "-v", "quiet",
            "-show_entries", "stream=width,height",
            "-of", "csv=p=0",
            videoFile.getAbsolutePath()
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String line = "";
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String l;
            while ((l = r.readLine()) != null) {
                if (l.matches("\\d+,\\d+")) { line = l; break; }
            }
        }
        p.waitFor();
        if (line.isEmpty()) return new int[]{1920, 1080}; // fallback: assume landscape
        String[] parts = line.split(",");
        return new int[]{ Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) };
    }
}
