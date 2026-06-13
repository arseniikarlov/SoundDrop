package com.alfa.shakegroan.data

import com.alfa.shakegroan.audio.BuiltInSoundCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsStorageMapperTest {

    @Test
    fun `uses slap defaults when snapshot has no slap fields`() {
        val settings = AppSettingsStorageMapper.fromSnapshot(
            StoredSettingsSnapshot(
                shakeSoundRaw = null,
                throwSoundRaw = null,
                slapSoundRaw = null,
                customSoundsRaw = null,
                legacyPlaybackModeRaw = PlaybackMode.BUILT_IN.name,
                legacyBuiltInPackRaw = BuiltInPack.CLEAN.name,
            )
        )

        assertTrue(settings.slapEnabled)
        assertEquals(95.0f, settings.throwImpactThreshold, 0.0f)
        assertEquals(18.0f, settings.slapImpactThreshold, 0.0f)
        assertEquals(1000, settings.cooldownMs)
        assertEquals("clean_untitled2", settings.slapSound.reference)
        assertEquals("untitled2", settings.slapSound.displayName)
    }

    @Test
    fun `legacy custom only snapshot migrates slap to first custom sound`() {
        val customSounds = listOf(
            CustomSound(uri = "content://demo/custom-1", displayName = "my-hit.mp3")
        )

        val settings = AppSettingsStorageMapper.fromSnapshot(
            StoredSettingsSnapshot(
                customSoundsRaw = AppSettingsStorageMapper.encodeCustomSounds(customSounds),
                legacyPlaybackModeRaw = PlaybackMode.CUSTOM_ONLY.name,
                legacyBuiltInPackRaw = BuiltInPack.CLEAN.name,
            )
        )

        assertEquals(SoundSourceType.CUSTOM, settings.slapSound.sourceType)
        assertEquals("content://demo/custom-1", settings.slapSound.reference)
        assertEquals("my-hit", settings.slapSound.displayName)
    }

    @Test
    fun `assignment codec round trips slap assignment`() {
        val original = BuiltInSoundCatalog.defaultSlapAssignment()

        val restored = AppSettingsStorageMapper.decodeAssignment(
            AppSettingsStorageMapper.encodeAssignment(original),
            BuiltInSoundCatalog.defaultShakeAssignment()
        )

        assertEquals(original, restored)
    }
}
