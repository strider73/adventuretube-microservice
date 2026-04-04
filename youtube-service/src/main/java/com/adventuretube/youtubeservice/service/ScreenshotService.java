package com.adventuretube.youtubeservice.service;


import com.adventuretube.youtubeservice.kafka.ScreenProducer;
import com.adventuretube.youtubeservice.model.entity.adventuretube.AdventureTubeData;
import com.adventuretube.youtubeservice.model.entity.adventuretube.Chapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;


@Slf4j
@Service
public class ScreenshotService {
    private final S3Client s3Client;
    private final String bucketName;
    private final ScreenProducer screenProducer;

    public ScreenshotService(S3Client s3Client,
                             @Value("${minio.bucket-name}")String bucketName,
                             ScreenProducer screenProducer) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.screenProducer = screenProducer;

    }


    public void processScreenshotJob(String youtubeContentID,
                                     String trackingId ,
                                     List<Chapter> chapters) {
        //find existing or create new job status



        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("screenshot");
            String youtubeUrl = "https://youtube.com/watch?v=" + youtubeContentID;

            for (int i = 0; i < chapters.size(); i++) {
                Chapter chapter = chapters.get(i);
                long seconds = chapter.getYoutubeTime();

                log.info("Processing screenshot {}/{} for {} at {}s",
                        i + 1, chapters.size(), youtubeContentID, seconds);

                String startTime = formatTime(seconds);
                String endTime = formatTime(seconds + 1);
                Path clipFile = tempDir.resolve("ch" + (i + 1) + ".mp4");
                Path screenshotFile = tempDir.resolve("ch" + (i + 1) + ".jpg");

                // Step 1: yt-dlp
                int ytExit = runProcess("yt-dlp",
                        "-o", clipFile.toString(),
                        "--download-sections", "*" + startTime + "-" + endTime,
                        "-f", "best[height<=720]",
                        youtubeUrl);
                if (ytExit != 0) {
                    log.error("yt-dlp failed for chapter {} (exit {})", i + 1, ytExit);
                    continue;
                }

                // Step 2: ffmpeg
                int ffExit = runProcess("ffmpeg", "-y",
                        "-i", clipFile.toString(),
                        "-frames:v", "1",
                        "-update", "1",
                        "-q:v", "2",
                        screenshotFile.toString());
                if (ffExit != 0) {
                    log.error("ffmpeg failed for chapter {} (exit {})", i + 1, ffExit);
                    continue;
                }

                // Step 3: Upload to MinIO
                String s3Key = youtubeContentID + "/ch" + (i + 1) + "_" + seconds + "s.jpg";
                s3Client.putObject(
                        PutObjectRequest.builder()
                                .bucket(bucketName)
                                .key(s3Key)
                                .contentType("image/jpeg")
                                .build(),
                        screenshotFile);

                // Step 4: Set screenshot URL on chapter
                chapter.setScreenshotUrl(s3Key);

                log.info("Screenshot uploaded: {}/{}", bucketName, s3Key);
            }


            log.info("All screenshots completed for {}", youtubeContentID);
            //TODO create kafka message to update screenshots URL in geospatial-servic
            AdventureTubeData data = new AdventureTubeData();
            data.setChapters(chapters);
            screenProducer.returnScreenshotURL(youtubeContentID,trackingId,data);

        } catch (IOException | InterruptedException e) {
            log.error("Screenshot processing failed for {}: {}", youtubeContentID, e.getMessage(), e);

        }finally {
            if (tempDir != null) {
                try {
                    Files.walk(tempDir)
                            .sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                } catch (IOException e) {
                    log.warn("Failed to clean up temp dir: {}", e.getMessage());
                }
            }
        }


    }

    public void deleteScreenshots(String youtubeContentID, String trackingId, AdventureTubeData adventureTubeData) {

        adventureTubeData.getChapters().forEach(chapter -> {
            if (chapter.getScreenshotUrl() != null) {
                log.info("chapter url :"+chapter.getScreenshotUrl());
                s3Client.deleteObject(
                        DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(chapter.getScreenshotUrl())
                        .build());
                chapter.setScreenshotUrl(null);//remove screenshot url from chapter
                log.info("Deleted screenshot: {}", chapter.getScreenshotUrl());
            }

        });
        log.info("All screenshots deleted for {}", youtubeContentID);

        //create kafka message to update all deletion has been completed
        screenProducer.returnDeleteResult(youtubeContentID, trackingId, adventureTubeData);


    }

    private String formatTime(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }

    private int runProcess(String... command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
        Process process = pb.start();
        return process.waitFor();
    }
}
