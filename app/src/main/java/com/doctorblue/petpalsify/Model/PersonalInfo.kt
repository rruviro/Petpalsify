package com.doctorblue.petpalsify.Model

data class PersonalInfo(
    val firstname: String = "",
    val lastname: String = "",
    val phoneNumber: String = "",
    val password: String = "",
    val category: String? = null,
    val postCount: String? = null,
    val profileImage: String? = null,
    val profileCover: String? = null
)
