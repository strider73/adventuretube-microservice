package com.adventuretube.web.service;


import com.adventuretube.common.client.ServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class YoutubeDataService {

    private static final String BASE_URL = "http://YOUTUBE-SERVICE";
    private final ServiceClient serviceClient;
}
