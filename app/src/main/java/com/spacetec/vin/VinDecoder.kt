package com.spacetec.vin

/**
 * Comprehensive VIN decoder for extracting vehicle specifications
 */
object VinDecoder {
    
    /**
     * Decode VIN to extract comprehensive vehicle information
     */
    fun decodeVin(vin: String): VehicleInfo? {
        if (vin.length != 17) return null
        
        return try {
            VehicleInfo(
                vin = vin,
                worldManufacturerIdentifier = vin.substring(0, 3),
                vehicleDescriptorSection = vin.substring(3, 9),
                vehicleIdentifierSection = vin.substring(9, 17),
                manufacturer = getManufacturer(vin.substring(0, 3)),
                country = getCountry(vin[0]),
                modelYear = getModelYear(vin[9]),
                assemblyPlant = vin[10].toString(),
                serialNumber = vin.substring(11, 17),
                engineType = getEngineType(vin[7]),
                bodyStyle = getBodyStyle(vin[5]),
                driveType = getDriveType(vin[6]),
                restraintSystem = getRestraintSystem(vin[8])
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get manufacturer from WMI (World Manufacturer Identifier)
     */
    private fun getManufacturer(wmi: String): String {
        return when (wmi.uppercase()) {
            // Major US Manufacturers
            "1G1", "1G3", "1G6", "1GC", "1GT" -> "General Motors"
            "1FA", "1FB", "1FC", "1FD", "1FE", "1FF", "1FG", "1FH", "1FJ", "1FK", "1FL", "1FM", "1FN", "1FP", "1FR", "1FS", "1FT", "1FU", "1FV", "1FW", "1FX", "1FY", "1FZ" -> "Ford Motor Company"
            "1C3", "1C4", "1C6", "1D3", "1D4", "1D7", "1D8" -> "Chrysler"
            "2C3", "2C4", "2C8", "2D3", "2D4", "2D8" -> "Chrysler Canada"
            "3C3", "3C4", "3C6", "3C8", "3D3", "3D4", "3D7" -> "Chrysler Mexico"
            
            // Japanese Manufacturers
            "JH4", "JHM" -> "Honda"
            "1HG", "2HG", "3HG" -> "Honda"
            "JF1", "JF2" -> "Subaru"
            "4F2", "4F4" -> "Subaru"
            "JT2", "JT3", "JT4", "JT6", "JT8" -> "Toyota"
            "4T1", "4T3", "5T3" -> "Toyota"
            "JN1", "JN6", "JN8" -> "Nissan"
            "1N4", "1N6", "3N1", "3N6" -> "Nissan"
            "JM1", "JM3", "JMZ" -> "Mazda"
            "1YV", "4F4", "4F6" -> "Mazda"
            "JS2", "JS3" -> "Suzuki"
            "2S3", "2S4" -> "Suzuki"
            
            // German Manufacturers
            "WBA", "WBS", "WBY" -> "BMW"
            "4US", "5UX", "5YM" -> "BMW"
            "WDB", "WDC", "WDD", "WDF", "WDH", "WDJ", "WDK" -> "Mercedes-Benz"
            "4JG", "55S" -> "Mercedes-Benz"
            "WVW", "WVG", "WV1", "WV2" -> "Volkswagen"
            "1VW", "3VW" -> "Volkswagen"
            "WAU", "WA1" -> "Audi"
            "WP0", "WP1" -> "Porsche"
            
            // Korean Manufacturers
            "KMH", "KMF" -> "Hyundai"
            "KND", "KNA", "KNB", "KNC", "KNE", "KNF", "KNG", "KNH", "KNJ", "KNK", "KNL", "KNM" -> "Kia"
            "KL1", "KL4" -> "Daewoo"
            
            // European Manufacturers
            "VF1", "VF2", "VF3", "VF6", "VF7", "VF8", "VF9" -> "Renault"
            "VF3" -> "Peugeot"
            "VF7" -> "Citroën"
            "ZFA", "ZFC", "ZFF" -> "Fiat"
            "ZAR" -> "Alfa Romeo"
            "ZLA" -> "Lancia"
            "VSS" -> "SEAT"
            "VWV" -> "Škoda"
            
            else -> "Unknown Manufacturer ($wmi)"
        }
    }
    
    /**
     * Get country of origin from first VIN character
     */
    private fun getCountry(firstChar: Char): String {
        return when (firstChar.uppercaseChar()) {
            '1', '4', '5' -> "United States"
            '2' -> "Canada"
            '3' -> "Mexico"
            '6' -> "Australia"
            '9' -> "Brazil"
            'J' -> "Japan"
            'K' -> "South Korea"
            'L' -> "China"
            'M' -> "India"
            'S' -> "United Kingdom"
            'T' -> "Czechoslovakia"
            'V' -> "France"
            'W' -> "Germany"
            'X' -> "Russia"
            'Y' -> "Sweden"
            'Z' -> "Italy"
            else -> "Unknown"
        }
    }
    
    /**
     * Get model year from 10th VIN character
     */
    private fun getModelYear(yearChar: Char): Int {
        return when (yearChar.uppercaseChar()) {
            'A' -> 1980; 'B' -> 1981; 'C' -> 1982; 'D' -> 1983; 'E' -> 1984
            'F' -> 1985; 'G' -> 1986; 'H' -> 1987; 'J' -> 1988; 'K' -> 1989
            'L' -> 1990; 'M' -> 1991; 'N' -> 1992; 'P' -> 1993; 'R' -> 1994
            'S' -> 1995; 'T' -> 1996; 'V' -> 1997; 'W' -> 1998; 'X' -> 1999
            'Y' -> 2000; '1' -> 2001; '2' -> 2002; '3' -> 2003; '4' -> 2004
            '5' -> 2005; '6' -> 2006; '7' -> 2007; '8' -> 2008; '9' -> 2009
            else -> {
                // For years 2010+, cycle repeats
                when (yearChar.uppercaseChar()) {
                    'A' -> 2010; 'B' -> 2011; 'C' -> 2012; 'D' -> 2013; 'E' -> 2014
                    'F' -> 2015; 'G' -> 2016; 'H' -> 2017; 'J' -> 2018; 'K' -> 2019
                    'L' -> 2020; 'M' -> 2021; 'N' -> 2022; 'P' -> 2023; 'R' -> 2024
                    'S' -> 2025; 'T' -> 2026; 'V' -> 2027; 'W' -> 2028; 'X' -> 2029
                    'Y' -> 2030
                    else -> 2000
                }
            }
        }
    }
    
    /**
     * Get engine type from 8th VIN character (varies by manufacturer)
     */
    private fun getEngineType(engineChar: Char): String {
        return when (engineChar.uppercaseChar()) {
            'A', 'B', 'C' -> "4-Cylinder"
            'D', 'E', 'F' -> "V6"
            'G', 'H', 'J' -> "V8"
            'K', 'L' -> "V10"
            'M', 'N' -> "V12"
            'P', 'R' -> "Hybrid"
            'S', 'T' -> "Electric"
            'U', 'V' -> "Diesel"
            'W', 'X' -> "Turbo"
            else -> "Unknown Engine ($engineChar)"
        }
    }
    
    /**
     * Get body style from 6th VIN character (varies by manufacturer)
     */
    private fun getBodyStyle(bodyChar: Char): String {
        return when (bodyChar.uppercaseChar()) {
            '1', 'A' -> "Sedan"
            '2', 'B' -> "Coupe"
            '3', 'C' -> "Convertible"
            '4', 'D' -> "Hatchback"
            '5', 'E' -> "Station Wagon"
            '6', 'F' -> "SUV"
            '7', 'G' -> "Pickup Truck"
            '8', 'H' -> "Van"
            '9', 'J' -> "Minivan"
            else -> "Unknown Body Style ($bodyChar)"
        }
    }
    
    /**
     * Get drive type from 7th VIN character (varies by manufacturer)
     */
    private fun getDriveType(driveChar: Char): String {
        return when (driveChar.uppercaseChar()) {
            '1', 'A', 'F' -> "Front-Wheel Drive"
            '2', 'B', 'R' -> "Rear-Wheel Drive"
            '3', 'C', '4' -> "All-Wheel Drive"
            '4', 'D' -> "4-Wheel Drive"
            else -> "Unknown Drive Type ($driveChar)"
        }
    }
    
    /**
     * Get restraint system from 9th VIN character
     */
    private fun getRestraintSystem(restraintChar: Char): String {
        return when (restraintChar.uppercaseChar()) {
            '0', '1', '2', '3' -> "Manual Belts"
            '4', '5', '6' -> "Automatic Belts"
            '7', '8', '9' -> "Airbags"
            'A', 'B', 'C' -> "Advanced Airbags"
            else -> "Unknown Restraint System ($restraintChar)"
        }
    }
    
    /**
     * Validate VIN checksum (9th digit)
     */
    fun validateVin(vin: String): Boolean {
        if (vin.length != 17) return false
        
        val weights = intArrayOf(8, 7, 6, 5, 4, 3, 2, 10, 0, 9, 8, 7, 6, 5, 4, 3, 2)
        var sum = 0
        
        for (i in vin.indices) {
            if (i == 8) continue // Skip check digit position
            
            val value = when (val char = vin[i].uppercaseChar()) {
                in '0'..'9' -> char - '0'
                'A' -> 1; 'B' -> 2; 'C' -> 3; 'D' -> 4; 'E' -> 5
                'F' -> 6; 'G' -> 7; 'H' -> 8; 'J' -> 1; 'K' -> 2
                'L' -> 3; 'M' -> 4; 'N' -> 5; 'P' -> 7; 'R' -> 9
                'S' -> 2; 'T' -> 3; 'U' -> 4; 'V' -> 5; 'W' -> 6
                'X' -> 7; 'Y' -> 8; 'Z' -> 9
                else -> return false
            }
            
            sum += value * weights[i]
        }
        
        val checkDigit = sum % 11
        val expectedCheckChar = if (checkDigit == 10) 'X' else checkDigit.toString()[0]
        
        return vin[8].uppercaseChar() == expectedCheckChar
    }
}
