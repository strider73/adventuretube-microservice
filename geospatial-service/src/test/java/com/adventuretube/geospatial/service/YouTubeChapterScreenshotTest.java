package com.adventuretube.geospatial.service;

import com.adventuretube.geospatial.model.entity.adventuretube.AdventureTubeData;
import com.adventuretube.geospatial.model.entity.adventuretube.Chapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("manual")
class YouTubeChapterScreenshotTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String MINIO_ENDPOINT = "http://strider-pi.local:9200";
    private static final String MINIO_ACCESS_KEY = "minioadmin";
    private static final String MINIO_SECRET_KEY = "minioadmin";
    private static final String BUCKET_NAME = "chapter-screenshots";

    private Path outputDir;
    private S3Client s3Client;

    private static final String TEST_JSON = """
            {
              "youtubeContentID": "1hRpjHi4xRA",
              "youtubeTitle": "Mornington Peninsula National Park hiking with Yegun hiking 14JULY2024",
              "chapters": [
                {
                  "youtubeId": "1hRpjHi4xRA",
                  "youtubeTime": 88,
                  "categories": ["hiking"],
                  "place": {
                    "name": "Mornington Peninsula National Park",
                    "youtubeTime": 88,
                    "placeID": "no placeID",
                    "contentCategory": ["hiking"],
                    "location": { "type": "Point", "coordinates": [144.88811373710632, -38.491395667870378] }
                  }
                },
                {
                  "youtubeId": "1hRpjHi4xRA",
                  "youtubeTime": 288,
                  "categories": ["hiking"],
                  "place": {
                    "name": "Cape Schanck Museum",
                    "youtubeTime": 288,
                    "placeID": "no placeID",
                    "contentCategory": ["hiking"],
                    "location": { "type": "Point", "coordinates": [144.8864883184433, -38.492033873334613] }
                  }
                },
                {
                  "youtubeId": "1hRpjHi4xRA",
                  "youtubeTime": 941,
                  "categories": ["campfire"],
                  "place": {
                    "name": "Cape Schanck Lighthouse",
                    "youtubeTime": 941,
                    "placeID": "no placeID",
                    "contentCategory": ["campfire"],
                    "location": { "type": "Point", "coordinates": [144.88652050495148, -38.492756046384436] }
                  }
                },
                {
                  "youtubeId": "1hRpjHi4xRA",
                  "youtubeTime": 1307,
                  "categories": ["bbq"],
                  "place": {
                    "name": "Signal Tower Hill",
                    "youtubeTime": 1307,
                    "placeID": "no placeID",
                    "contentCategory": ["bbq"],
                    "location": { "type": "Point", "coordinates": [144.88616108894348, -38.490925407595263] }
                  }
                }
              ]
            }
            """;

    @BeforeEach
    void setUp() throws IOException {
        outputDir = Files.createTempDirectory("yt-chapter-screenshots-");
        s3Client = S3Client.builder()
                .endpointOverride(URI.create(MINIO_ENDPOINT))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(MINIO_ACCESS_KEY, MINIO_SECRET_KEY)))
                .serviceConfiguration(cfg -> cfg.pathStyleAccessEnabled(true))
                .build();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (s3Client != null) {
            s3Client.close();
        }
        if (outputDir != null && Files.exists(outputDir)) {
            Files.walk(outputDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    @Test
    void shouldExtractScreenshotsAndUploadToMinIO() throws Exception {
        AdventureTubeData data = objectMapper.readValue(TEST_JSON, AdventureTubeData.class);

        assertThat(data.getChapters()).hasSize(4);
        assertThat(data.getYoutubeContentID()).isEqualTo("1hRpjHi4xRA");

        // Create bucket if it doesn't exist
        createBucketIfNotExists(BUCKET_NAME);

        String youtubeUrl = "https://youtube.com/watch?v=" + data.getYoutubeContentID();
        String youtubeId = data.getYoutubeContentID();
        List<String> uploadedKeys = new ArrayList<>();

        for (int i = 0; i < data.getChapters().size(); i++) {
            Chapter chapter = data.getChapters().get(i);
            long seconds = chapter.getYoutubeTime();
            String placeName = chapter.getPlace().getName();

            System.out.printf("Processing chapter %d: %s at %ds%n", i + 1, placeName, seconds);

            String startTime = formatTime(seconds);
            String endTime = formatTime(seconds + 1);
            String clipFile = outputDir.resolve("ch" + (i + 1) + ".mp4").toString();
            String screenshotFile = outputDir.resolve("ch" + (i + 1) + "_screenshot.jpg").toString();

            // Step 1: Download 1-second clip with yt-dlp
            int ytExitCode = runProcess(
                    "yt-dlp",
                    "-o", clipFile,
                    "--download-sections", "*" + startTime + "-" + endTime,
                    "-f", "best[height<=720]",
                    youtubeUrl
            );
            assertThat(ytExitCode)
                    .as("yt-dlp should succeed for chapter %d (%s)", i + 1, placeName)
                    .isEqualTo(0);
            assertThat(new File(clipFile)).exists();

            // Step 2: Extract frame with ffmpeg
            int ffmpegExitCode = runProcess(
                    "ffmpeg", "-y",
                    "-i", clipFile,
                    "-frames:v", "1",
                    "-update", "1",
                    "-q:v", "2",
                    screenshotFile
            );
            assertThat(ffmpegExitCode)
                    .as("ffmpeg should succeed for chapter %d (%s)", i + 1, placeName)
                    .isEqualTo(0);

            File screenshot = new File(screenshotFile);
            assertThat(screenshot).exists();
            assertThat(screenshot.length()).isGreaterThan(0);

            // Step 3: Upload to MinIO
            String s3Key = youtubeId + "/ch" + (i + 1) + "_" + seconds + "s.jpg";
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(BUCKET_NAME)
                            .key(s3Key)
                            .contentType("image/jpeg")
                            .build(),
                    screenshot.toPath()
            );
            uploadedKeys.add(s3Key);

            System.out.printf("  -> Uploaded to MinIO: %s/%s (%d bytes)%n",
                    BUCKET_NAME, s3Key, screenshot.length());
        }

        // Verify all files exist in MinIO
        System.out.println("\nVerifying uploads in MinIO...");
        for (String key : uploadedKeys) {
            HeadObjectResponse head = s3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(BUCKET_NAME)
                            .key(key)
                            .build()
            );
            assertThat(head.contentLength()).isGreaterThan(0);
            System.out.printf("  OK: %s (%d bytes)%n", key, head.contentLength());
        }

        System.out.printf("%nAll %d screenshots uploaded to MinIO bucket '%s'%n",
                uploadedKeys.size(), BUCKET_NAME);
    }

    private void createBucketIfNotExists(String bucketName) {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            System.out.println("Bucket already exists: " + bucketName);
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
            System.out.println("Bucket created: " + bucketName);
        }
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
