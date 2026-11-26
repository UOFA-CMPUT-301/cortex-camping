/*
 * RoleAccessTest.kt
 *
 * Unit tests for RoleAccess (US 03.11.01: an admin can be a campsite owner and/or entrant
 * with their admin profile)
 */

package com.example.cortex_camping

import com.example.cortex_camping.model.Admin
import com.example.cortex_camping.model.CampsiteOwner
import com.example.cortex_camping.model.Entrant
import com.example.cortex_camping.viewmodel.Role
import com.example.cortex_camping.viewmodel.RoleAccess
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoleAccessTest {

    private val deviceId = "device-1"
    private val admin = Admin(id = deviceId, name = "Ada Admin")

    @Test
    fun adminCanChooseOwnerCamperOrAdmin() {
        val roles = RoleAccess.availableRoles(isRegistered = false, isAdmin = true)
        assertEquals(listOf(Role.Owner, Role.User, Role.Admin), roles)
    }

    @Test
    fun registeredCamperCannotChooseAdmin() {
        val roles = RoleAccess.availableRoles(isRegistered = true, isAdmin = false)
        assertEquals(listOf(Role.Owner, Role.User), roles)
    }

    @Test
    fun unregisteredNonAdminHasNoRoles() {
        assertTrue(RoleAccess.availableRoles(isRegistered = false, isAdmin = false).isEmpty())
        assertFalse(RoleAccess.canLogIn(isRegistered = false, isAdmin = false))
    }

    @Test
    fun ownerOnlyCanLogInAndChooseOwnerOrCamper() {
        assertTrue(RoleAccess.canLogIn(isRegistered = false, isAdmin = false, isOwner = true))
        val roles = RoleAccess.availableRoles(isRegistered = false, isAdmin = false, isOwner = true)
        assertEquals(listOf(Role.Owner, Role.User), roles)
    }

    @Test
    fun adminCanLogInWithoutRegistering() {
        assertTrue(RoleAccess.canLogIn(isRegistered = false, isAdmin = true))
    }

    @Test
    fun adminWithoutCamperProfileGetsOneFromAdminProfile() {
        val entrant = RoleAccess.entrantFor(deviceId, existing = null, admin = admin)
        assertEquals(deviceId, entrant?.id)
        assertEquals("Ada Admin", entrant?.name)
    }

    @Test
    fun existingCamperProfileIsKept() {
        val existing = Entrant(id = deviceId, name = "Ada Camper")
        assertNull(RoleAccess.entrantFor(deviceId, existing, admin))
    }

    @Test
    fun nonAdminIsNotGivenCamperProfile() {
        assertNull(RoleAccess.entrantFor(deviceId, existing = null, admin = null))
    }

    @Test
    fun adminOwnerProfileUsesAdminName() {
        val owner = RoleAccess.ownerFor(deviceId, existing = null, entrant = null, admin = admin)
        assertEquals(CampsiteOwner(id = deviceId, name = "Ada Admin", email = ""), owner)
    }

    @Test
    fun ownerProfilePrefersCamperDetails() {
        val entrant = Entrant(id = deviceId, name = "Ada Camper", email = "ada@example.com")
        val owner = RoleAccess.ownerFor(deviceId, existing = null, entrant = entrant, admin = admin)
        assertEquals(CampsiteOwner(id = deviceId, name = "Ada Camper", email = "ada@example.com"), owner)
    }

    @Test
    fun existingOwnerProfileIsKept() {
        val existing = CampsiteOwner(id = deviceId, name = "Ada Owner")
        assertNull(RoleAccess.ownerFor(deviceId, existing, entrant = null, admin = admin))
    }
}
