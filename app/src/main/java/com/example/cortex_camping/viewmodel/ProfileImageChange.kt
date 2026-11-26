package com.example.cortex_camping.viewmodel

import com.example.cortex_camping.util.ImageUtils

/** What to do with the profile picture when the profile is saved (US 01.08.02). */
sealed interface ProfileImageChange {
    data object Keep : ProfileImageChange
    data object Remove : ProfileImageChange
    data class Upload(val uri: String) : ProfileImageChange

    companion object {
        /**
         * Works out the change from the saved picture to the one on the form. A newly picked photo
         * is a local Uri, and null means the picture was removed.
         */
        fun between(stored: String?, onForm: String?): ProfileImageChange = when {
            onForm == stored -> Keep
            onForm == null -> Remove
            ImageUtils.isLocalUri(onForm) -> Upload(onForm)
            else -> Keep
        }
    }
}
