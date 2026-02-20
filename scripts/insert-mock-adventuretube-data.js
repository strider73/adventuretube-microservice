// insert-mock-adventuretube-data.js
// Run: mongosh "mongodb://strider:5785Ch00@adventuretube.net:27017/adventuretube?authSource=admin" insert-mock-adventuretube-data.js

const categories = ["Travel", "Food", "Nature", "Culture", "Adventure", "Hiking", "Beach", "City", "Camping", "Road Trip"];
const contentTypes = ["vlog", "guide", "review", "documentary", "short"];
const tripDurations = ["1 day", "2 days", "3 days", "1 week", "2 weeks", "weekend", "half day"];

const placeNames = [
    "Great Ocean Road Lookout", "Bells Beach", "12 Apostles", "Lorne Pier", "Apollo Bay",
    "Torquay Surf Beach", "Cape Otway Lighthouse", "Kennet River Koala Walk", "Erskine Falls",
    "Split Point Lighthouse", "Maits Rest Rainforest", "London Arch", "Loch Ard Gorge",
    "Teddy's Lookout", "Wye River", "Cumberland River", "Aireys Inlet", "Anglesea Beach",
    "Point Addis", "Johanna Beach", "Blanket Bay", "Moonlight Head", "Princetown",
    "Port Campbell National Park", "Bay of Islands", "The Grotto", "Thunder Cave",
    "Gyeongbokgung Palace", "Bukchon Hanok Village", "Namsan Tower", "Myeongdong Street",
    "Hongdae District", "Gangnam Station", "Insadong Art Street", "Dongdaemun Market",
    "Haeundae Beach Busan", "Gamcheon Culture Village", "Jagalchi Fish Market",
    "Nami Island", "Seoraksan National Park", "Jeju Hallasan", "Seongsan Ilchulbong",
    "Woljeongri Beach Jeju", "Manjanggul Cave", "Gwangjang Market", "Cheonggyecheon Stream",
    "Itaewon District", "Starfield Library", "Lotte World Tower",
    "Flinders Street Station", "Federation Square", "Royal Botanic Gardens Melbourne",
    "Queen Victoria Market", "Melbourne Cricket Ground", "St Kilda Beach",
    "Brighton Beach Boxes", "Hosier Lane", "Block Arcade", "Eureka Skydeck",
    "Shrine of Remembrance", "Melbourne Museum", "Degraves Street",
    "Philip Island Penguin Parade", "Yarra Valley Winery", "Puffing Billy Railway",
    "Dandenong Ranges", "Mornington Peninsula Hot Springs", "Wilsons Promontory",
    "Grampians National Park", "Ballarat Sovereign Hill", "Daylesford Lake",
    "Tokyo Tower", "Shibuya Crossing", "Senso-ji Temple", "Meiji Shrine",
    "Fushimi Inari Shrine Kyoto", "Arashiyama Bamboo Grove", "Mount Fuji 5th Station",
    "Tsukiji Outer Market", "Akihabara Electric Town", "Shinjuku Gyoen",
    "Osaka Castle", "Dotonbori Street", "Nara Deer Park", "Kinkaku-ji Temple",
    "Hakone Open Air Museum", "Kamakura Great Buddha", "Nikko Toshogu Shrine",
    "Miyajima Island", "Hiroshima Peace Memorial", "Kanazawa Kenrokuen Garden",
    "Sydney Opera House", "Harbour Bridge", "Bondi Beach", "Blue Mountains",
    "Taronga Zoo", "Manly Beach", "The Rocks Markets", "Darling Harbour",
    "Royal National Park", "Cockatoo Island"
];

function rand(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

function pick(arr) {
    return arr[rand(0, arr.length - 1)];
}

function pickN(arr, n) {
    const shuffled = [...arr].sort(() => 0.5 - Math.random());
    return shuffled.slice(0, n);
}

function randomCoordinates() {
    // Mix of Australia (-37.x, 144.x), Korea (37.x, 127.x), Japan (35.x, 139.x)
    const regions = [
        { latBase: -37.8, lonBase: 144.9, spread: 1.5 },  // Melbourne/Victoria
        { latBase: -38.7, lonBase: 143.5, spread: 0.5 },  // Great Ocean Road
        { latBase: 37.5, lonBase: 127.0, spread: 0.5 },   // Seoul
        { latBase: 33.5, lonBase: 126.5, spread: 0.3 },   // Jeju
        { latBase: 35.7, lonBase: 139.7, spread: 0.5 },   // Tokyo
        { latBase: -33.9, lonBase: 151.2, spread: 0.3 },   // Sydney
    ];
    const region = pick(regions);
    const lat = region.latBase + (Math.random() - 0.5) * region.spread;
    const lon = region.lonBase + (Math.random() - 0.5) * region.spread;
    return [parseFloat(lon.toFixed(6)), parseFloat(lat.toFixed(6))]; // GeoJSON: [lng, lat]
}

function generatePlace(index) {
    const name = placeNames[index % placeNames.length];
    return {
        contentCategory: pickN(categories, rand(1, 3)),
        youtubeTime: rand(0, 1400),
        location: {
            type: "Point",
            coordinates: randomCoordinates()
        },
        placeID: "ChIJ" + Math.random().toString(36).substring(2, 15),
        name: name
    };
}

function generateChapter(placeIndex, youtubeContentID) {
    const place = generatePlace(placeIndex);
    return {
        place: place,
        youtubeId: youtubeContentID,
        youtubeTime: place.youtubeTime,
        categories: pickN(categories, rand(1, 3))
    };
}

const docs = [];

for (let i = 0; i < 100; i++) {
    const youtubeContentID = "yt_" + Math.random().toString(36).substring(2, 13);
    const numPlaces = rand(2, 5);
    const numChapters = rand(2, 6);

    const places = [];
    for (let p = 0; p < numPlaces; p++) {
        places.push(generatePlace(rand(0, placeNames.length - 1)));
    }

    const chapters = [];
    for (let c = 0; c < numChapters; c++) {
        chapters.push(generateChapter(rand(0, placeNames.length - 1), youtubeContentID));
    }

    docs.push({
        userContentCategory: pickN(categories, rand(1, 4)),
        places: places,
        userTripDuration: pick(tripDurations),
        youtubeDescription: `Adventure vlog #${i + 1} exploring amazing locations. Join us on this ${pick(tripDurations)} trip through beautiful scenery, local food, and cultural experiences.`,
        youtubeTitle: `${pick(["Epic", "Amazing", "Ultimate", "Best", "Hidden", "Top", "Must-See", "Incredible"])} ${pick(["Adventure", "Travel", "Journey", "Trip", "Tour", "Exploration", "Guide", "Vlog"])} - ${places[0].name} & More!`,
        chapters: chapters,
        userContentType: pick(contentTypes),
        coreDataID: new ObjectId().toString(),
        youtubeContentID: youtubeContentID
    });
}

const result = db.adventureTubeData.insertMany(docs);
print(`Inserted ${result.insertedIds.length} documents into adventureTubeData`);
