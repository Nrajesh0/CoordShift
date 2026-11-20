package com.example.domain

/**
 * 100% offline GPS place name resolver (0ms latency, zero network permissions).
 * Uses a compact bundled gazetteer of major global cities and metro hubs.
 */
object PlaceNameResolver {

    private data class CitySeed(
        val name: String,
        val lat: Double,
        val lng: Double
    )

    private val offlineCities = listOf(
        // Japan & Korea
        CitySeed("Tokyo, JP", 35.6762, 139.6503),
        CitySeed("Yokohama, JP", 35.4437, 139.6380),
        CitySeed("Osaka, JP", 34.6937, 135.5023),
        CitySeed("Kyoto, JP", 35.0116, 135.7681),
        CitySeed("Kobe, JP", 34.6901, 135.1955),
        CitySeed("Nagoya, JP", 35.1815, 136.9066),
        CitySeed("Sapporo, JP", 43.0618, 141.3545),
        CitySeed("Sendai, JP", 38.2682, 140.8694),
        CitySeed("Hiroshima, JP", 34.3853, 132.4553),
        CitySeed("Fukuoka, JP", 33.5902, 130.4017),
        CitySeed("Naha, JP", 26.2124, 127.6809),
        CitySeed("Seoul, KR", 37.5665, 126.9780),
        CitySeed("Incheon, KR", 37.4563, 126.7052),
        CitySeed("Busan, KR", 35.1796, 129.0756),
        CitySeed("Daegu, KR", 35.8714, 128.6014),
        CitySeed("Jeju, KR", 33.4996, 126.5312),
        // East & Southeast Asia
        CitySeed("Taipei, TW", 25.0330, 121.5654),
        CitySeed("Kaohsiung, TW", 22.6273, 120.3014),
        CitySeed("Taichung, TW", 24.1477, 120.6736),
        CitySeed("Hong Kong", 22.3193, 114.1694),
        CitySeed("Macau", 22.1987, 113.5439),
        CitySeed("Shanghai, CN", 31.2304, 121.4737),
        CitySeed("Beijing, CN", 39.9042, 116.4074),
        CitySeed("Shenzhen, CN", 22.5431, 114.0579),
        CitySeed("Guangzhou, CN", 23.1291, 113.2644),
        CitySeed("Chengdu, CN", 30.5728, 104.0668),
        CitySeed("Singapore", 1.3521, 103.8198),
        CitySeed("Bangkok, TH", 13.7563, 100.5018),
        CitySeed("Chiang Mai, TH", 18.7883, 98.9853),
        CitySeed("Phuket, TH", 7.8804, 98.3923),
        CitySeed("Kuala Lumpur, MY", 3.1390, 101.6869),
        CitySeed("Penang, MY", 5.4164, 100.3327),
        CitySeed("Manila, PH", 14.5995, 120.9842),
        CitySeed("Cebu, PH", 10.3157, 123.8854),
        CitySeed("Jakarta, ID", -6.2088, 106.8456),
        CitySeed("Surabaya, ID", -7.2575, 112.7521),
        CitySeed("Bali, ID", -8.4095, 115.1889),
        CitySeed("Ho Chi Minh, VN", 10.8231, 106.6297),
        CitySeed("Hanoi, VN", 21.0278, 105.8342),
        CitySeed("Da Nang, VN", 16.0544, 108.2022),
        // South Asia
        CitySeed("Mumbai, IN", 19.0760, 72.8777),
        CitySeed("New Delhi, IN", 28.6139, 77.2090),
        CitySeed("Bengaluru, IN", 12.9716, 77.5946),
        CitySeed("Hyderabad, IN", 17.3850, 78.4867),
        CitySeed("Chennai, IN", 13.0827, 80.2707),
        CitySeed("Kolkata, IN", 22.5726, 88.3639),
        CitySeed("Pune, IN", 18.5204, 73.8567),
        CitySeed("Ahmedabad, IN", 23.0225, 72.5714),
        CitySeed("Jaipur, IN", 26.9124, 75.7873),
        CitySeed("Kochi, IN", 9.9312, 76.2673),
        CitySeed("Coimbatore, IN", 11.0168, 76.9558),
        CitySeed("Visakhapatnam, IN", 17.6868, 83.2185),
        CitySeed("Vijayawada, IN", 16.5062, 80.6480),
        CitySeed("Tirupati, IN", 13.6288, 79.4192),
        CitySeed("Lucknow, IN", 26.8467, 80.9462),
        CitySeed("Chandigarh, IN", 30.7333, 76.7794),
        CitySeed("Indore, IN", 22.7196, 75.8577),
        CitySeed("Bhubaneswar, IN", 20.2961, 85.8245),
        CitySeed("Nagpur, IN", 21.1458, 79.0882),
        CitySeed("Colombo, LK", 6.9271, 79.8612),
        CitySeed("Dhaka, BD", 23.8103, 90.4125),
        CitySeed("Karachi, PK", 24.8607, 67.0011),
        CitySeed("Lahore, PK", 31.5204, 74.3587),
        CitySeed("Kathmandu, NP", 27.7172, 85.3240),
        // Middle East
        CitySeed("Dubai, AE", 25.2048, 55.2708),
        CitySeed("Abu Dhabi, AE", 24.4539, 54.3773),
        CitySeed("Doha, QA", 25.2854, 51.5310),
        CitySeed("Riyadh, SA", 24.7136, 46.6753),
        CitySeed("Jeddah, SA", 21.4858, 39.1925),
        CitySeed("Istanbul, TR", 41.0082, 28.9784),
        CitySeed("Ankara, TR", 39.9334, 32.8597),
        CitySeed("Tel Aviv, IL", 32.0853, 34.7818),
        // Europe
        CitySeed("London, UK", 51.5072, -0.1276),
        CitySeed("Manchester, UK", 53.4808, -2.2426),
        CitySeed("Birmingham, UK", 52.4862, -1.8904),
        CitySeed("Edinburgh, UK", 55.9533, -3.1883),
        CitySeed("Dublin, IE", 53.3498, -6.2603),
        CitySeed("Paris, FR", 48.8566, 2.3522),
        CitySeed("Lyon, FR", 45.7640, 4.8357),
        CitySeed("Marseille, FR", 43.2965, 5.3698),
        CitySeed("Berlin, DE", 52.5200, 13.4050),
        CitySeed("Munich, DE", 48.1351, 11.5820),
        CitySeed("Hamburg, DE", 53.5511, 9.9937),
        CitySeed("Frankfurt, DE", 50.1109, 8.6821),
        CitySeed("Cologne, DE", 50.9375, 6.9603),
        CitySeed("Madrid, ES", 40.4168, -3.7038),
        CitySeed("Barcelona, ES", 41.3874, 2.1686),
        CitySeed("Zaragoza, ES", 41.6488, -0.8891),
        CitySeed("Seville, ES", 37.3891, -5.9845),
        CitySeed("Valencia, ES", 39.4699, -0.3763),
        CitySeed("Canary Islands, ES", 28.1235, -15.4363),
        CitySeed("Rome, IT", 41.9028, 12.4964),
        CitySeed("Milan, IT", 45.4642, 9.1900),
        CitySeed("Naples, IT", 40.8518, 14.2681),
        CitySeed("Florence, IT", 43.7696, 11.2558),
        CitySeed("Amsterdam, NL", 52.3676, 4.9041),
        CitySeed("Rotterdam, NL", 51.9244, 4.4777),
        CitySeed("Brussels, BE", 50.8503, 4.3517),
        CitySeed("Vienna, AT", 48.2082, 16.3738),
        CitySeed("Zurich, CH", 47.3769, 8.5417),
        CitySeed("Geneva, CH", 46.2044, 6.1432),
        CitySeed("Prague, CZ", 50.0755, 14.4378),
        CitySeed("Budapest, HU", 47.4979, 19.0402),
        CitySeed("Warsaw, PL", 52.2297, 21.0122),
        CitySeed("Krakow, PL", 50.0647, 19.9450),
        CitySeed("Stockholm, SE", 59.3293, 18.0686),
        CitySeed("Copenhagen, DK", 55.6761, 12.5683),
        CitySeed("Oslo, NO", 59.9139, 10.7522),
        CitySeed("Helsinki, FI", 60.1699, 24.9384),
        CitySeed("Athens, GR", 37.9838, 23.7275),
        CitySeed("Lisbon, PT", 38.7223, -9.1393),
        CitySeed("Porto, PT", 41.1579, -8.6291),
        CitySeed("Bucharest, RO", 44.4268, 26.1025),
        // North America
        CitySeed("New York, US", 40.7128, -74.0060),
        CitySeed("San Francisco, US", 37.7749, -122.4194),
        CitySeed("San Jose, US", 37.3382, -121.8863),
        CitySeed("Los Angeles, US", 34.0522, -118.2437),
        CitySeed("Santa Monica, US", 34.0195, -118.4912),
        CitySeed("San Diego, US", 32.7157, -117.1611),
        CitySeed("Chicago, US", 41.8781, -87.6298),
        CitySeed("Miami, US", 25.7617, -80.1918),
        CitySeed("Orlando, US", 28.5383, -81.3792),
        CitySeed("Atlanta, US", 33.7490, -84.3880),
        CitySeed("Houston, US", 29.7604, -95.3698),
        CitySeed("Dallas, US", 32.7767, -96.7970),
        CitySeed("Austin, US", 30.2672, -97.7431),
        CitySeed("Phoenix, US", 33.4484, -112.0740),
        CitySeed("Denver, US", 39.7392, -104.9903),
        CitySeed("Seattle, US", 47.6062, -122.3321),
        CitySeed("Portland, US", 45.5152, -122.6784),
        CitySeed("Boston, US", 42.3601, -71.0589),
        CitySeed("Washington DC, US", 38.9072, -77.0369),
        CitySeed("Philadelphia, US", 39.9526, -75.1652),
        CitySeed("Las Vegas, US", 36.1699, -115.1398),
        CitySeed("Honolulu, US", 21.3069, -157.8583),
        CitySeed("Anchorage, US", 61.2181, -149.9003),
        CitySeed("Toronto, CA", 43.6532, -79.3832),
        CitySeed("Vancouver, CA", 49.2827, -123.1207),
        CitySeed("Montreal, CA", 45.5017, -73.5673),
        CitySeed("Calgary, CA", 51.0447, -114.0719),
        CitySeed("Mexico City, MX", 19.4326, -99.1332),
        CitySeed("Guadalajara, MX", 20.6597, -103.3496),
        CitySeed("Monterrey, MX", 25.6866, -100.3161),
        // South America
        CitySeed("São Paulo, BR", -23.5505, -46.6333),
        CitySeed("Rio de Janeiro, BR", -22.9068, -43.1729),
        CitySeed("Brasília, BR", -15.7975, -47.8919),
        CitySeed("Buenos Aires, AR", -34.6037, -58.3816),
        CitySeed("Santiago, CL", -33.4489, -70.6693),
        CitySeed("Bogotá, CO", 4.7110, -74.0721),
        CitySeed("Medellín, CO", 6.2442, -75.5812),
        CitySeed("Lima, PE", -12.0464, -77.0428),
        CitySeed("Quito, EC", -0.1807, -78.4678),
        // Oceania & Africa
        CitySeed("Sydney, AU", -33.8688, 151.2093),
        CitySeed("Melbourne, AU", -37.8136, 144.9631),
        CitySeed("Brisbane, AU", -27.4705, 153.0260),
        CitySeed("Perth, AU", -31.9505, 115.8605),
        CitySeed("Adelaide, AU", -34.9285, 138.6007),
        CitySeed("Auckland, NZ", -36.8485, 174.7633),
        CitySeed("Wellington, NZ", -41.2865, 174.7762),
        CitySeed("Christchurch, NZ", -43.5321, 172.6362),
        CitySeed("Cairo, EG", 30.0444, 31.2357),
        CitySeed("Johannesburg, ZA", -26.2041, 28.0473),
        CitySeed("Cape Town, ZA", -33.9249, 18.4241),
        CitySeed("Lagos, NG", 6.5244, 3.3792),
        CitySeed("Nairobi, KE", -1.2921, 36.8219),
        CitySeed("Casablanca, MA", 33.5731, -7.5898)
    )

    /**
     * Instant 0ms offline lookup against the bundled world city gazetteer.
     * Returns exact city within 25 km, "Near <City>" within 65 km, or null if remote.
     */
    fun lookupOfflineCity(lat: Double, lng: Double): String? {
        var nearest: CitySeed? = null
        var bestDistKm = Double.MAX_VALUE
        for (city in offlineCities) {
            val dist = CooldownCalculator.distanceKm(lat, lng, city.lat, city.lng)
            if (dist < bestDistKm) {
                bestDistKm = dist
                nearest = city
            }
        }
        return when {
            nearest == null -> null
            bestDistKm <= 25.0 -> nearest.name
            bestDistKm <= 65.0 -> "Near ${nearest.name}"
            else -> null
        }
    }
}
