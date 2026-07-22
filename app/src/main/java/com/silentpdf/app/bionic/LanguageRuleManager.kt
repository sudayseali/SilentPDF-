package com.silentpdf.app.bionic

import java.util.concurrent.ConcurrentHashMap

object LanguageRuleManager {

    private val profiles = ConcurrentHashMap<BionicLanguage, LanguageProfile>()
    private val fallbackLatin = DefaultLatinProfile(BionicLanguage.ENGLISH)

    init {
        registerProfile(DefaultLatinProfile(BionicLanguage.ENGLISH))
        registerProfile(DefaultLatinProfile(BionicLanguage.FRENCH))
        registerProfile(DefaultLatinProfile(BionicLanguage.SPANISH))
        registerProfile(DefaultLatinProfile(BionicLanguage.GERMAN))
        registerProfile(SomaliProfile())
        registerProfile(ArabicProfile())
        registerProfile(CjkProfile(BionicLanguage.CHINESE))
        registerProfile(CjkProfile(BionicLanguage.JAPANESE))
        registerProfile(CjkProfile(BionicLanguage.KOREAN))
        registerProfile(CyrillicProfile())
        registerProfile(DevanagariProfile())
        registerProfile(HebrewProfile())
    }

    fun registerProfile(profile: LanguageProfile) {
        profiles[profile.language] = profile
    }

    fun getProfile(language: BionicLanguage): LanguageProfile {
        return profiles[language] ?: fallbackLatin
    }
}
