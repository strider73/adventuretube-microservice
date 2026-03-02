package com.adventuretube.geospatial.service;

import com.adventuretube.geospatial.model.entity.adventuretube.AdventureTubeData;
import com.adventuretube.geospatial.repository.AdventureTubeDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdventureTubeDataService {

    private final AdventureTubeDataRepository repository;

    public List<AdventureTubeData> findAll() {
        return repository.findAll();
    }

    public Optional<AdventureTubeData> findById(String id) {
        return repository.findById(id);
    }

    public Optional<AdventureTubeData> findByYoutubeContentID(String youtubeContentID) {
        return repository.findByYoutubeContentID(youtubeContentID);
    }

    public List<AdventureTubeData> findByContentType(String contentType) {
        return repository.findByUserContentType(contentType);
    }

    public List<AdventureTubeData> findByCategory(String category) {
        return repository.findByUserContentCategoryContaining(category);
    }

    public AdventureTubeData save(AdventureTubeData data) {
        return repository.save(data);
    }

    public long count() {
        return repository.count();
    }
}
