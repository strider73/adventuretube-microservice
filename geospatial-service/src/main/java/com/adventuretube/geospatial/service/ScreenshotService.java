package com.adventuretube.geospatial.service;

import com.adventuretube.geospatial.model.entity.ScreenshotJobStatus;
import com.adventuretube.geospatial.model.entity.adventuretube.AdventureTubeData;
import com.adventuretube.geospatial.model.entity.adventuretube.Chapter;
import com.adventuretube.geospatial.model.enums.ScreenshotJobStatusEnum;
import com.adventuretube.geospatial.repository.AdventureTubeDataRepository;
import com.adventuretube.geospatial.repository.ScreenshotJobStatusRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;



@Slf4j
@Service
public class ScreenshotService {
    private final S3Client s3Client;
    private final String bucketName;
    private final AdventureTubeDataRepository adventureTubeDataRepository;
    private final ScreenshotJobStatusRepository screenshotJobStatusRepository;

    public ScreenshotService(S3Client s3Client,
                             @Value("${minio.bucket-name}")String bucketName,
                             AdventureTubeDataRepository adventureTubeDataRepository,
                             ScreenshotJobStatusRepository screenshotJobStatusRepository){
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.adventureTubeDataRepository = adventureTubeDataRepository;
        this.screenshotJobStatusRepository = screenshotJobStatusRepository;
    }


    public void processScreenshotJob(String youtubeContentID, List<Chapter> chapters) {
        //find existing or create new job status
        ScreenshotJobStatus jobStatus = screenshotJobStatusRepository.findByYoutubeContentID(youtubeContentID)
                .map(existing -> {
                    log.info("Data already exist for youtubeContentID:"+existing.getYoutubeContentID()+" withs status :"+existing.getStatus() +"will be set as pending ");
                    existing.setStatus(ScreenshotJobStatusEnum.PENDING);
                    existing.setTotalChapters(chapters.size());
                    existing.setCompletedChapters(0);
                    existing.setErrorMessage(null);
                    existing.setUpdatedAt(LocalDateTime.now());
                    existing.setExpireAt(LocalDateTime.now());
                    return screenshotJobStatusRepository.save(existing);
                })
                .orElseGet(() -> screenshotJobStatusRepository.save(
                        ScreenshotJobStatus.builder()
                                .youtubeContentID(youtubeContentID)
                                .status(ScreenshotJobStatusEnum.PENDING)
                                .totalChapters(chapters.size())
                                .completedChapters(0)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .expireAt(LocalDateTime.now())
                                .build()
                ));


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

                // Step 4: Update chapter
                chapter.setScreenshotUrl(s3Key);

                // Update progress
                jobStatus.setCompletedChapters(i + 1);
                jobStatus.setUpdatedAt(LocalDateTime.now());
                screenshotJobStatusRepository.save(jobStatus);

                log.info("Screenshot uploaded: {}/{}", bucketName, s3Key);
            }

            // Update MongoDB with screenshot URLs
            adventureTubeDataRepository.findByYoutubeContentID(youtubeContentID)
                    .ifPresent(data -> {
                        data.setChapters(chapters);
                        adventureTubeDataRepository.save(data);
                    });

            // Mark completed
            jobStatus.setStatus(ScreenshotJobStatusEnum.COMPLETED);
            jobStatus.setUpdatedAt(LocalDateTime.now());
            screenshotJobStatusRepository.save(jobStatus);
            log.info("All screenshots completed for {}", youtubeContentID);

        } catch (IOException | InterruptedException e) {
            log.error("Screenshot processing failed for {}: {}", youtubeContentID, e.getMessage(), e);
            jobStatus.setStatus(ScreenshotJobStatusEnum.FAILED);
            jobStatus.setErrorMessage(e.getMessage());
            jobStatus.setUpdatedAt(LocalDateTime.now());
            screenshotJobStatusRepository.save(jobStatus);
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

    public void deleteScreenshots(String youtubeContentID, AdventureTubeData adventureTubeData) {

        adventureTubeData.getChapters().forEach(chapter -> {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(chapter.getScreenshotUrl())
                    .build());
            log.info("Deleted screenshot: {}", chapter.getScreenshotUrl());
        });
        log.info("All screenshots deleted for {}", youtubeContentID);



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
