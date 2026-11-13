package org.stilllauncher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileKeyTest {

    @Test
    fun currentProfileKeepsPlainPackageName() {
        assertEquals(
            "com.example.app",
            profileKeyFor("com.example.app", userId = 0, currentUserId = 0)
        )
    }

    @Test
    fun secondaryProfileGetsStableSuffix() {
        assertEquals(
            "com.example.app#10",
            profileKeyFor("com.example.app", userId = 10, currentUserId = 0)
        )
    }

    @Test
    fun samePackageInDifferentProfilesHasDifferentKey() {
        val primary = profileKeyFor("org.telegram.messenger", 0, 0)
        val dual = profileKeyFor("org.telegram.messenger", 10, 0)
        assertEquals(false, primary == dual)
    }
}
