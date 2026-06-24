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
                throwSoundRaw = null,
                slapSoundRaw = null,
                customSoundsRaw = null,
                legacyPlaybackModeRaw = PlaybackMode.BUILT_IN.name,
                legacyBuiltInPackRaw = BuiltInPack.CLEAN.name,
            )
        )

        assertEquals(false, settings.hasSeenIntroGuide)
        assertTrue(settings.slapEnabled)
        assertEquals(67.0f, settings.throwImpactThreshold, 0.0f)
        assertEquals(13.0f, settings.slapImpactThreshold, 0.0f)
        assertEquals(1000, settings.cooldownMs)
        assertEquals("archive4_aaaaaa", settings.throwSound.reference)
        assertEquals("AAAAAA", settings.throwSound.displayName)
        assertEquals("archive4_oof", settings.slapSound.reference)
        assertEquals("Oof!", settings.slapSound.displayName)
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
    fun `legacy profane assignment falls back to clean throw default`() {
        val settings = AppSettingsStorageMapper.fromSnapshot(
            StoredSettingsSnapshot(
                throwSoundRaw = """{"sourceType":"BUILT_IN_PROFANE","reference":"profane_tts","displayName":"legacy"}""",
                legacyBuiltInPackRaw = "PROFANE",
            )
        )

        assertEquals(SoundSourceType.BUILT_IN_CLEAN, settings.throwSound.sourceType)
        assertEquals("archive4_aaaaaa", settings.throwSound.reference)
        assertEquals("AAAAAA", settings.throwSound.displayName)
    }

    @Test
    fun `assignment codec round trips slap assignment`() {
        val original = BuiltInSoundCatalog.defaultSlapAssignment()

        val restored = AppSettingsStorageMapper.decodeAssignment(
            AppSettingsStorageMapper.encodeAssignment(original),
            BuiltInSoundCatalog.defaultThrowAssignment()
        )

        assertEquals(original, restored)
    }
}
