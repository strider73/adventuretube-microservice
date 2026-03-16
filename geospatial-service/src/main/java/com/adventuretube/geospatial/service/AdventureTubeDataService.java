package com.adventuretube.geospatial.service;

import com.adventuretube.geospatial.exceptions.OwnershipMismatchException;
import com.adventuretube.geospatial.exceptions.code.GeoErrorCode;
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

    public AdventureTubeData update(String id, AdventureTubeData data) {
        return repository.findById(id)
                .map(existing -> {
                    data.setId(id);
                    return repository.save(data);
                })
                .orElseThrow(() -> new IllegalArgumentException("AdventureTubeData not found with id: " + id));
    }

//    public void delete(String id) {
//        if (!repository.existsById(id)) {
//            throw new IllegalArgumentException("AdventureTubeData not found with id: " + id);
//        }
//        repository.deleteById(id);
//    }

    public void deleteByYoutubeContentIdAndOwnerEmail(String youtubeContentId, String ownerEmail) {
        AdventureTubeData data = repository.findByYoutubeContentID(youtubeContentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "AdventureTubeData not found with youtubeContentID: " + youtubeContentId));

        if (!data.getOwnerEmail().equals(ownerEmail)) {
            throw new OwnershipMismatchException(GeoErrorCode.OWNERSHIP_MISMATCH);
        }

        repository.deleteById(data.getId());
    }


    public long count() {
        return repository.count();
    }
}
