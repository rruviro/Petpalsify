package com.doctorblue.petpalsify.Model

data class PetProfile(
    var petImage: String? = null,
    val petName: String = "",
    val petAge: String = "",
    val petAbout: String = "",
    val petHealth: String = "",
    val petWeight: String = "",
    val petAddress: String = "",
    val petBirthday: String = "",
    val petCategory: String = "",
    val petBreed: String = "",
    val petGender: String = "",
    val userContact: String = "",
    val petPostDate: String = "",
    var petId: String? = "",
    val userpetId: String = "",
    var userId: String? = ""
)