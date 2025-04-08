package com.duanlab.npsclient

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

object IntentExtraKey {
    const val NpsConfig = "NpsConfig"
}

@Parcelize
data class NpsConfig(
    val cmdstr: String
) : Parcelable
