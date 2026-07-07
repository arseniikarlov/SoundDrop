package com.alfa.shakegroan.data

import com.alfa.shakegroan.audio.BuiltInSoundCatalog
import org.junit.Assert.assertEquals
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
        assertEquals(false, settings.slapEnabled)
        assertEquals(50.0f, settings.throwImpactThreshold, 0.0f)
        assertEquals(24.0f, settings.slapImpactThreshold, 0.0f)
        assertEquals(1000, settings.cooldownMs)
        assertEquals("archive4_tom_scream", settings.throwSound.reference)
        assertEquals("Tom Scream", settings.throwSound.displayName)
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
        assertEquals("archive4_tom_scream", settings.throwSound.reference)
        assertEquals("Tom Scream", settings.throwSound.displayName)
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

    @Test
    fun `custom sound codec keeps imported local files`() {
        val original = listOf(
            CustomSound(
                uri = "file:///data/user/0/com.fallouch.myapp/files/custom_sounds/funny_hit.mp3",
                displayName = "funny_hit.mp3",
            )
        )

        val restored = AppSettingsStorageMapper.decodeCustomSounds(
            AppSettingsStorageMapper.encodeCustomSounds(original)
        )

        assertEquals(1, restored.size)
        assertEquals(original.first().uri, restored.first().uri)
        assertEquals("funny_hit", restored.first().displayName)
    }
}
