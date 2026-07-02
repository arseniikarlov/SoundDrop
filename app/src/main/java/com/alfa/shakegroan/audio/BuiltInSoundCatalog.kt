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
        BundledSound("archive4_bruh", R.raw.archive4_bruh, "Bruh", isPopular = true),
        BundledSound("archive4_explosion", R.raw.archive4_explosion, "Explosion", isPopular = true),
        BundledSound("archive4_oof", R.raw.archive4_oof, "Oof!", isPopular = true),
        BundledSound("archive4_sad", R.raw.archive4_sad, "Sad"),
        BundledSound("archive4_tom_scream", R.raw.archive4_tom_scream, "Tom Scream", isPopular = true),
        BundledSound("archive4_tom_scream2", R.raw.archive4_tom_scream2, "Tom Scream2", isPopular = true),
        BundledSound("archive4_vine_boom", R.raw.archive4_vine_boom, "Vine Boom", isPopular = true),
    )

    fun cleanSoundById(id: String): BundledSound? = cleanSounds.firstOrNull { it.id == id }

    fun defaultThrowAssignment(): SoundAssignment = assignmentFor(cleanSounds.first { it.id == "archive4_tom_scream" })

    fun defaultSlapAssignment(): SoundAssignment = assignmentFor(cleanSounds.first { it.id == "archive4_oof" })

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
        SoundSourceType.CUSTOM -> assignment.displayName
    }

    fun isPopular(assignment: SoundAssignment): Boolean = when (assignment.sourceType) {
        SoundSourceType.BUILT_IN_CLEAN -> cleanSoundById(assignment.reference)?.isPopular == true
        SoundSourceType.CUSTOM -> false
    }
}
