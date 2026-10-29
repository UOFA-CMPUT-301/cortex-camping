package com.example.cortex_camping.model

data class Campsite (
    val ownerId: String = "", // each campsite will have an owner
    val campsiteId: String = "", // each campsite has a unique ID as well
    val name: String = "", // name of campsite
    val description: String = "", // desc of campsite
    val tag: List<String> = emptyList(), // tags, is a list because a campsite can have multiple tags
    val maxOccupancy: Int = 1, // maximum number of people that the campsite can house, 1 by default
    val price: Double, // price per night, in Canadian
    val imageUrl: String? = null, // optional image url of the campsite
    val availability: AvailabilitySchedule, // AvailabilitySchedule is its own class
    val rating: Double = 0.0 // campsite rating defaults to 0.0
)