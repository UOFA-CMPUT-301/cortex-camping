package com.example.cortex_camping.util

import android.content.Context
import android.provider.Settings

/*
The purpose of this file is for US 1.09.01:
"As an entrant, I want to be identified by my device, so that I do not have to use a username and password."
We could use this /only/ for entrants but I think I want to extend its usage to Entrants, CampsiteOwners, and Admins since we're storing
them in a repository anyway and we need to handle clean storage.

Maybe that's wrong though who knows -Alex
 */

object DeviceUtils {
    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown device"
    }
}