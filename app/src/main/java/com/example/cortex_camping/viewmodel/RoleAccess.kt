package com.example.cortex_camping.viewmodel

import com.example.cortex_camping.model.Admin
import com.example.cortex_camping.model.CampsiteOwner
import com.example.cortex_camping.model.Entrant

/** Lets an admin also be a camper and/or campsite owner with their admin profile (US 03.11.01). */
object RoleAccess {

    /**
     * Any account can be an owner or a camper. A device with only an owner profile (e.g. its camper
     * profile was deleted) still gets Camper, and picking it sends them to register a new camper profile.
     */
    fun availableRoles(isRegistered: Boolean, isAdmin: Boolean, isOwner: Boolean = false): List<Role> = buildList {
        if (isRegistered || isAdmin || isOwner) {
            add(Role.Owner)
            add(Role.User)
        }
        if (isAdmin) add(Role.Admin)
    }

    /** Whether this device has any account (camper, owner or admin), so it logs in instead of registering. */
    fun canLogIn(isRegistered: Boolean, isAdmin: Boolean, isOwner: Boolean = false): Boolean =
        isRegistered || isAdmin || isOwner

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
