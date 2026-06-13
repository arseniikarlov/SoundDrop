package com.alfa.shakegroan.audio

import com.alfa.shakegroan.R
import com.alfa.shakegroan.data.CustomSound
import com.alfa.shakegroan.data.SoundAssignment
import com.alfa.shakegroan.data.SoundSourceType

data class BundledSound(
    val id: String,
    val resId: Int,
    val displayName: String,
    val isPopular: Boolean = false,
)

object BuiltInSoundCatalog {
    val cleanSounds = listOf(
        BundledSound("clean_doh1", R.raw.clean_doh1, "doh1", isPopular = true),
        BundledSound("clean_untitled2", R.raw.clean_untitled2, "untitled2"),
        BundledSound("clean_tom_scream", R.raw.clean_tom_scream, "tom_scream", isPopular = true),
        BundledSound("clean_o_kurwa", R.raw.clean_o_kurwa, "o-kurwa"),
        BundledSound("clean_sdfds", R.raw.clean_sdfds, "sdfds"),
        BundledSound("clean_gta_wasted_5", R.raw.clean_gta_wasted_5, "5-gta-wasted", isPopular = true),
    )

    val profanePhrases = listOf(
        "бля!",
        "ёб твою мать!",
        "какого хера!",
        "пиздец!",
        "сука, бля!",
    )

    const val PROFANE_SOUND_ID = "profane_tts"
    const val PROFANE_SOUND_NAME = "Русский мат TTS"

    fun cleanSoundById(id: String): BundledSound? = cleanSounds.firstOrNull { it.id == id }

    fun defaultThrowAssignment(): SoundAssignment = assignmentFor(cleanSounds.first { it.id == "clean_tom_scream" })

    fun defaultSlapAssignment(): SoundAssignment = assignmentFor(cleanSounds.first { it.id == "clean_untitled2" })

    fun profaneAssignment(): SoundAssignment = SoundAssignment(
        sourceType = SoundSourceType.BUILT_IN_PROFANE,
        reference = PROFANE_SOUND_ID,
        displayName = PROFANE_SOUND_NAME,
    )

    fun assignmentFor(sound: BundledSound): SoundAssignment = SoundAssignment(
        sourceType = SoundSourceType.BUILT_IN_CLEAN,
        reference = sound.id,
        displayName = sound.displayName,
    )

    fun assignmentFor(sound: CustomSound): SoundAssignment = SoundAssignment(
        sourceType = SoundSourceType.CUSTOM,
        reference = sound.uri,
        displayName = sound.displayName,
    )

    fun labelFor(assignment: SoundAssignment): String = when (assignment.sourceType) {
        SoundSourceType.BUILT_IN_CLEAN -> cleanSoundById(assignment.reference)?.displayName ?: assignment.displayName
        SoundSourceType.BUILT_IN_PROFANE -> PROFANE_SOUND_NAME
        SoundSourceType.CUSTOM -> assignment.displayName
    }

    fun isPopular(assignment: SoundAssignment): Boolean = when (assignment.sourceType) {
        SoundSourceType.BUILT_IN_CLEAN -> cleanSoundById(assignment.reference)?.isPopular == true
        SoundSourceType.BUILT_IN_PROFANE -> true
        SoundSourceType.CUSTOM -> false
    }
}
