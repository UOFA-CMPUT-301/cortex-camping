package com.example.cortex_camping.model

data class Campsite (
    val name: String, // name of campsite
    val description: String, // desc of campsite
    val tag: List<String>, // tags, is a list because a campsite can have multiple tags
    val maxOccupancy: Int, // maximum number of people that the campsite can house
    val price: Double, // price per night, in Canada
    val imageUrl: String? = null, // optional image url of the campsite
    val availability: AvailabilitySchedule, // AvailabilitySchedule is its own class
    val rating: Double = 0.0 // campsite rating defaults to 0.0
)