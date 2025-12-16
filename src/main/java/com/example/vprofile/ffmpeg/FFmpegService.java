package com.example.vprofile.ffmpeg;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.springframework.stereotype.Service;

@Service
public class FFmpegService {

    /**
     * Compresses a video file and applies a watermark using an optimized FFmpeg command.
     *
     * @param inputFile  The source video file.
     * @param outputFile The destination for the compressed video file.
     * @throws IOException          if an I/O error occurs.
     * @throws InterruptedException if the process is interrupted.
     */
    public void compressVideo(File inputFile, File outputFile) throws IOException, InterruptedException {
        // Hardcoded paths for ffmpeg and the watermark image
       String ffmpegPath = "/usr/bin/ffmpeg";

        // Path to the watermark image
        String watermarkPath = "/home/wezume/htdocs/wezume.in/img/watermark.png";
        
        // --- IMPORTANT ---
        // For this to be fast, you must resize your 'circle.png' image to the
        // desired final dimensions (e.g., 300x150 pixels) *before* running this code.
        // This avoids forcing FFmpeg to scale the image for every video frame.

        // --- OPTION 1: Standard CPU Encoding (Faster than before) ---
        // This command is much faster because it no longer scales the watermark.
        String[] command = {
            ffmpegPath,
            "-i", inputFile.getAbsolutePath(),
            "-i", watermarkPath, // The pre-scaled watermark image
            "-filter_complex", "overlay=x=W-w-80:y=20", // Simplified, faster filter
            "-vcodec", "libx264",
            "-preset", "ultrafast", // Fastest CPU preset
            "-crf", "30", // Lower quality for smaller size and faster encoding
            "-f", "mp4",
            "-y", // Overwrite output file if it exists
            outputFile.getAbsolutePath()
        };

        // --- OPTION 2: Hardware Accelerated Encoding (Much Faster on Mac) ---
        // Uncomment the following block to use Apple's VideoToolbox for hardware encoding.
        /*
        String[] command = {
            ffmpegPath,
            "-i", inputFile.getAbsolutePath(),
            "-i", watermarkPath,
            "-filter_complex", "overlay=x=W-w-80:y=20",
            "-vcodec", "h264_videotoolbox", // Use Apple's hardware encoder
            "-b:v", "4M", // Target a video bitrate of 4 Mbps (adjust as needed)
            "-allow_sw", "1", // Allow fallback to software filters if needed
            "-f", "mp4",
            "-y", // Overwrite output file
            outputFile.getAbsolutePath()
        };
        */

        System.out.println("Running FFmpeg command: " + String.join(" ", command));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // We print line-by-line to see progress in real-time
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

