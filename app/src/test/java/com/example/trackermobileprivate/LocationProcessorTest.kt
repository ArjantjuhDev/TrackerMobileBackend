package com.example.trackermobileprivate

import org.junit.Assert.assertEquals
import org.junit.Test

class LocationProcessorTest {
    @Test
    fun testProcessLocation() {
        val processor = LocationProcessor()
        val latitude = 52.370216
        val longitude = 4.895168
        val result = processor.processLocation(latitude, longitude)
        assertEquals("Amsterdam: 52.370216, 4.895168", result)
    }
}

// Simpele klasse om te testen
class LocationProcessor {
    fun processLocation(lat: Double, lon: Double): String {
        // In een echte app zou je hier reverse geocoding doen
        // Voor deze test simuleren we Amsterdam
        return "Amsterdam: $lat, $lon"
    }
}
