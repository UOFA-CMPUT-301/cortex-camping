package com.example.cortex_camping.viewmodel

import com.example.cortex_camping.model.Admin
import com.example.cortex_camping.model.CampsiteOwner
import com.example.cortex_camping.model.Entrant

/** Lets an admin also be a camper and/or campsite owner with their admin profile (US 03.11.01). */
object RoleAccess {

    fun availableRoles(isRegistered: Boolean, isAdmin: Boolean): List<Role> = buildList {
        if (isRegistered || isAdmin) {
            add(Role.Owner)
            add(Role.User)
        }
        if (isAdmin) add(Role.Admin)
    }

    /** Whether Log In can skip registration. */
    fun canLogIn(isRegistered: Boolean, isAdmin: Boolean): Boolean = isRegistered || isAdmin

    /** A camper profile for an admin, or null if the device already has one. */
    fun entrantFor(deviceId: String, existing: Entrant?, admin: Admin?): Entrant? {
        if (existing != null || admin == null) return null
        return Entrant(id = deviceId, name = admin.name)
    }

    /** An owner profile for this device, or null if it already has one. Uses the camper profile's details, or the admin's. */
    fun ownerFor(deviceId: String, existing: CampsiteOwner?, entrant: Entrant?, admin: Admin?): CampsiteOwner? {
        if (existing != null) return null
        return CampsiteOwner(
            id = deviceId,
            name = entrant?.name?.ifBlank { null } ?: admin?.name.orEmpty(),
            email = entrant?.email.orEmpty(),
        )
    }
}
