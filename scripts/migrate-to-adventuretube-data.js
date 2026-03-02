// migrate-to-adventuretube-data.js
// Merges data from 5 source collections into adventureTubeData
// Run: mongosh "mongodb://strider:5785Ch00@travel-tube.com:27017/adventuretube?authSource=admin" migrate-to-adventuretube-data.js

// ============================================================
// Step 1: Build lookup maps from sites (places with geo data)
// ============================================================

// victoria_camping_sites: id, title, location, lat, lng, category, description
const victoriaSites = db.victoria_camping_sites.find().toArray();
const victoriaSiteMap = {};
victoriaSites.forEach(s => { victoriaSiteMap[s.id] = s; });

// korea_adventure_sites: id, title, location, lat, lng, category, description, originalVideoId
const koreaSites = db.korea_adventure_sites.find().toArray();
const koreaSiteMap = {};
koreaSites.forEach(s => { koreaSiteMap[s.id] = s; });

print(`Loaded ${victoriaSites.length} victoria sites, ${koreaSites.length} korea sites`);

// ============================================================
// Step 2: Build lookup maps from community videos
// ============================================================

// victoria_community_videos: campingSiteId, videos[]
const victoriaVideos = db.victoria_community_videos.find().toArray();
const victoriaVideoMap = {};
victoriaVideos.forEach(v => { victoriaVideoMap[v.campingSiteId] = v.videos || []; });

// korea_community_videos: campingSiteId, videos[]
const koreaVideos = db.korea_community_videos.find().toArray();
const koreaVideoMap = {};
koreaVideos.forEach(v => { koreaVideoMap[v.campingSiteId] = v.videos || []; });

print(`Loaded ${victoriaVideos.length} victoria video groups, ${koreaVideos.length} korea video groups`);

// ============================================================
// Step 3: chris_video (your own videos)
// ============================================================
const chrisVideos = db.chris_video.find().toArray();
print(`Loaded ${chrisVideos.length} chris videos`);

// ============================================================
// Helper: convert site to Place subdocument
// ============================================================
function siteToPlace(site) {
    return {
        contentCategory: [site.category || "General"],
        youtubeTime: 0,
        location: {
            type: "Point",
            coordinates: [site.lng || 0, site.lat || 0]  // GeoJSON [lng, lat]
        },
        placeID: site.id || "",
        name: site.title || ""
    };
}

// ============================================================
// Helper: convert video + site into Chapter subdocument
// ============================================================
function videoToChapter(video, site) {
    return {
        place: site ? siteToPlace(site) : {
            contentCategory: ["General"],
            youtubeTime: 0,
            location: { type: "Point", coordinates: [0, 0] },
            placeID: "",
            name: ""
        },
        youtubeId: video.videoId || "",
        youtubeTime: 0,
        categories: site ? [site.category || "General"] : ["General"]
    };
}

// ============================================================
// Helper: derive userContentType from category
// ============================================================
function deriveContentType(category) {
    if (!category) return "vlog";
    const cat = category.toLowerCase();
    if (cat.includes("food") || cat.includes("cafe")) return "review";
    if (cat.includes("festival") || cat.includes("cultural")) return "documentary";
    if (cat.includes("hiking") || cat.includes("trail") || cat.includes("mountain")) return "guide";
    if (cat.includes("camping") || cat.includes("glamping") || cat.includes("bush")) return "guide";
    if (cat.includes("national park") || cat.includes("state forest")) return "guide";
    return "vlog";
}

// ============================================================
// Helper: derive trip duration from category
// ============================================================
function deriveTripDuration(category) {
    if (!category) return "1 day";
    const cat = category.toLowerCase();
    if (cat.includes("national park") || cat.includes("hiking") || cat.includes("mountain")) return "2 days";
    if (cat.includes("camping") || cat.includes("glamping") || cat.includes("bush")) return "weekend";
    if (cat.includes("road")) return "3 days";
    if (cat.includes("festival")) return "1 day";
    if (cat.includes("food") || cat.includes("cafe") || cat.includes("urban")) return "half day";
    return "1 day";
}

// ============================================================
// Step 4: Process chris_video → one AdventureTubeData per video
// ============================================================
const docs = [];
const usedYoutubeIDs = new Set();

chrisVideos.forEach(cv => {
    if (!cv.videoId || usedYoutubeIDs.has(cv.videoId)) return;
    usedYoutubeIDs.add(cv.videoId);

    // Try to find linked site
    const site = victoriaSiteMap[cv.campingSiteId] || koreaSiteMap[cv.campingSiteId] || null;

    const places = [];
    const chapters = [];

    if (site) {
        places.push(siteToPlace(site));
        chapters.push(videoToChapter(cv, site));
    }

    docs.push({
        userContentCategory: [cv.category || "General"],
        places: places,
        userTripDuration: deriveTripDuration(cv.category),
        youtubeDescription: site ? (site.description || cv.title) : cv.title,
        youtubeTitle: cv.title,
        chapters: chapters,
        userContentType: deriveContentType(cv.category),
        coreDataID: new ObjectId().toString(),
        youtubeContentID: cv.videoId
    });
});

print(`Created ${docs.length} docs from chris_video`);

// ============================================================
// Step 5: Process victoria sites + community videos
// ============================================================
victoriaSites.forEach(site => {
    const communityVids = victoriaVideoMap[site.id] || [];

    communityVids.forEach(video => {
        if (!video.videoId || usedYoutubeIDs.has(video.videoId)) return;
        usedYoutubeIDs.add(video.videoId);

        const places = [siteToPlace(site)];
        const chapters = [videoToChapter(video, site)];

        docs.push({
            userContentCategory: [site.category || "General"],
            places: places,
            userTripDuration: deriveTripDuration(site.category),
            youtubeDescription: site.description || video.title || "",
            youtubeTitle: video.title || "",
            chapters: chapters,
            userContentType: deriveContentType(site.category),
            coreDataID: new ObjectId().toString(),
            youtubeContentID: video.videoId
        });
    });
});

const afterVictoria = docs.length;
print(`Created ${afterVictoria} total docs after victoria community videos`);

// ============================================================
// Step 6: Process korea sites + community videos
// ============================================================
koreaSites.forEach(site => {
    // Include the originalVideoId from the site itself
    if (site.originalVideoId && !usedYoutubeIDs.has(site.originalVideoId)) {
        usedYoutubeIDs.add(site.originalVideoId);

        const places = [siteToPlace(site)];
        const chapters = [{
            place: siteToPlace(site),
            youtubeId: site.originalVideoId,
            youtubeTime: 0,
            categories: [site.category || "General"]
        }];

        docs.push({
            userContentCategory: [site.category || "General"],
            places: places,
            userTripDuration: deriveTripDuration(site.category),
            youtubeDescription: site.description || site.title || "",
            youtubeTitle: site.title || "",
            chapters: chapters,
            userContentType: deriveContentType(site.category),
            coreDataID: new ObjectId().toString(),
            youtubeContentID: site.originalVideoId
        });
    }

    // Community videos for this site
    const communityVids = koreaVideoMap[site.id] || [];

    communityVids.forEach(video => {
        if (!video.videoId || usedYoutubeIDs.has(video.videoId)) return;
        usedYoutubeIDs.add(video.videoId);

        const places = [siteToPlace(site)];
        const chapters = [videoToChapter(video, site)];

        docs.push({
            userContentCategory: [site.category || "General"],
            places: places,
            userTripDuration: deriveTripDuration(site.category),
            youtubeDescription: site.description || video.title || "",
            youtubeTitle: video.title || "",
            chapters: chapters,
            userContentType: deriveContentType(site.category),
            coreDataID: new ObjectId().toString(),
            youtubeContentID: video.videoId
        });
    });
});

print(`Created ${docs.length} total docs after korea`);

// ============================================================
// Step 7: Insert all into adventureTubeData
// ============================================================
if (docs.length > 0) {
    const result = db.adventureTubeData.insertMany(docs);
    print(`SUCCESS: Inserted ${Object.keys(result.insertedIds).length} documents into adventureTubeData`);
} else {
    print("No documents to insert");
}
