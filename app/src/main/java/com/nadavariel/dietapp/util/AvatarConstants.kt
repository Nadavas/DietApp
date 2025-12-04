package com.nadavariel.dietapp.util

import com.nadavariel.dietapp.R

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
        Pair("avatar_10", R.drawable.ic_adventurer_mackenzie),
        Pair("avatar_11", R.drawable.ic_adventurer_easton),
        Pair("avatar_12", R.drawable.ic_adventurer_maria),
        Pair("avatar_13", R.drawable.ic_adventurer_vivian),
        Pair("avatar_14", R.drawable.ic_adventurer_jocelyn),
        Pair("avatar_15", R.drawable.ic_adventurer_brooklynn),
        Pair("avatar_16", R.drawable.ic_adventurer_brian),
        Pair("avatar_17", R.drawable.ic_adventurer_mason),
        Pair("avatar_18", R.drawable.ic_adventurer_avery),
    )

    // A default avatar to show if none is selected or if the selected one is invalid
//    @DrawableRes
//    fun getDefaultAvatarResId(): Int {
//        return R.drawable.ic_person_filled
//    }

    // Function to get the resource ID from an avatar Id (the string key)
//    @DrawableRes
//    fun getAvatarResId(avatarId: String?): Int {
//        return AVATAR_DRAWABLES.firstOrNull { it.first == avatarId }?.second
//            ?: getDefaultAvatarResId()
//    }
}