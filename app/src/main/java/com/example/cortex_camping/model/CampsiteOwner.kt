package com.example.cortex_camping.model

import java.time.LocalDate

// we will connect this to firebase later at some point

class CampsiteOwner(
    val ownerName: String,
    private val campsites: MutableList<Campsite> = mutableListOf() // CampsiteOwner can have 0 or more campsites
) {
    fun listCampsite(campsite: Campsite) {
        campsites.add(campsite)
    }

    fun getCampsites(): List<Campsite> { // returns a read-only list of campsites (we might not need this)
        return campsites.toList()
    }
}