package com.alfa.shakegroan.audio

import com.alfa.shakegroan.R
import com.alfa.shakegroan.data.BuiltInPack

data class BundledSound(
    val resId: Int,
    val displayName: String,
)

object BuiltInSoundCatalog {
    val cleanSounds = listOf(
        BundledSound(R.raw.clean_doh1, "doh1.mp3"),
        BundledSound(R.raw.clean_untitled2, "untitled2.mp3"),
        BundledSound(R.raw.clean_tom_scream, "tom_scream.mp3"),
        BundledSound(R.raw.clean_o_kurwa, "o-kurwa.mp3"),
        BundledSound(R.raw.clean_sdfds, "sdfds.mp3"),
        BundledSound(R.raw.clean_gta_wasted_5, "5-gta-wasted.mp3"),
    )

    val profanePhrases = listOf(
        "бля!",
        "ёб твою мать!",
        "какого хера!",
        "пиздец!",
        "сука, бля!",
    )

    fun previewFor(pack: BuiltInPack): String = when (pack) {
        BuiltInPack.CLEAN -> "Набор без мата: ${cleanSounds.size} файлов"
        BuiltInPack.PROFANE -> profanePhrases.first()
    }

    fun labelFor(pack: BuiltInPack): String = when (pack) {
        BuiltInPack.CLEAN -> "Не мат"
        BuiltInPack.PROFANE -> "Мат"
    }

    fun detailFor(pack: BuiltInPack): String = when (pack) {
        BuiltInPack.CLEAN -> "Встроенные MP3 из пакета"
        BuiltInPack.PROFANE -> "Встроенный TTS с матом"
    }
}
