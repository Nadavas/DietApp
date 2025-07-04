package com.nadavariel.dietapp.util

import androidx.annotation.DrawableRes
import com.nadavariel.dietapp.R // Make sure this import is correct for your project's R file

object AvatarConstants {
    val AVATAR_DRAWABLES = listOf(
        Pair("avatar_01", R.drawable.ic_person_filled), // default
        Pair("avatar_02", R.drawable.ic_adventurer_aidan),
        Pair("avatar_03", R.drawable.ic_adventurer_eliza),
        Pair("avatar_04", R.drawable.ic_adventurer_jameson),
        Pair("avatar_05", R.drawable.ic_adventurer_katherine),
        Pair("avatar_06", R.drawable.ic_adventurer_oliver),
        Pair("avatar_07", R.drawable.ic_adventurer_ryan),
        Pair("avatar_08", R.drawable.ic_adventurer_sadie),
        Pair("avatar_09", R.drawable.ic_adventurer_sara),
    )

    // A default avatar to show if none is selected or if the selected one is invalid
    @DrawableRes
    fun getDefaultAvatarResId(): Int {
        return R.drawable.ic_person_filled // Default fallback icon
    }

    // Function to get the resource ID from an avatarId (the string key)
    @DrawableRes
    fun getAvatarResId(avatarId: String?): Int {
        return AVATAR_DRAWABLES.firstOrNull { it.first == avatarId }?.second
            ?: getDefaultAvatarResId()
    }
}