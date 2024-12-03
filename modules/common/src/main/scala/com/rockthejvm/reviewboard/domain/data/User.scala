package com.rockthejvm.reviewboard.domain.data

final case class User(
    id: Long,
    email: String,
    hashedPassword: String
){
    def toUserID = UserID(id, email)
}

// tonned down version of User
final case class UserID(
    id: Long,
    email: String
)