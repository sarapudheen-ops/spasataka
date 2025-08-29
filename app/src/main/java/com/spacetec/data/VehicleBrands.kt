package com.spacetec.data

/**
 * Data class containing all supported vehicle brands
 */
data class VehicleBrands(
    val brands: List<String>
) {
    companion object {
        /**
         * Get the complete list of supported vehicle brands
         */
        fun getAllBrands(): List<String> = listOf(
            "Acura",
            "Alfa Romeo",
            "Aston Martin",
            "Audi",
            "BMW",
            "Bentley",
            "Bugatti",
            "Buick",
            "Cadillac",
            "Chevrolet",
            "Chrysler",
            "Citroën",
            "Dacia",
            "Daewoo",
            "Daihatsu",
            "Dodge",
            "Ferrari",
            "Fiat",
            "Ford",
            "Geely",
            "Genesis",
            "GMC",
            "Great Wall",
            "Haval",
            "Honda",
            "Hyundai",
            "Infiniti",
            "Isuzu",
            "Jaguar",
            "Jeep",
            "Kia",
            "Koenigsegg",
            "Lada",
            "Lamborghini",
            "Lancia",
            "Land Rover",
            "Lexus",
            "Lincoln",
            "Lotus",
            "Maserati",
            "Maybach",
            "Mazda",
            "McLaren",
            "Mercedes-Benz",
            "Mini",
            "Mitsubishi",
            "Nissan",
            "Opel",
            "Peugeot",
            "Polestar",
            "Pontiac",
            "Porsche",
            "Proton",
            "Ram",
            "Renault",
            "Rolls Royce",
            "Rover",
            "Saab",
            "Scion",
            "Seat",
            "Skoda",
            "Smart",
            "SsangYong",
            "Subaru",
            "Suzuki",
            "Tata",
            "Tesla",
            "Toyota",
            "Vauxhall",
            "Volkswagen",
            "Volvo"
        )
        
        /**
         * Get brands filtered by search query
         */
        fun searchBrands(query: String): List<String> {
            return getAllBrands().filter { 
                it.contains(query, ignoreCase = true) 
            }
        }
        
        /**
         * Check if a brand is supported
         */
        fun isBrandSupported(brand: String): Boolean {
            return getAllBrands().contains(brand)
        }
    }
}
