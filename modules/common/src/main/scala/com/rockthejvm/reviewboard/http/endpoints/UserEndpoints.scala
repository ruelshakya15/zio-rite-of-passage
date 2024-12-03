package com.rockthejvm.reviewboard.http.endpoints

import sttp.tapir.*
import sttp.tapir.json.zio.*
import sttp.tapir.generic.auto.* // imports the type class derivation package  (implicit conversion for json milaucha)

import com.rockthejvm.reviewboard.http.requests.* 
import com.rockthejvm.reviewboard.http.responses.* 
import com.rockthejvm.reviewboard.domain.data.UserToken
import com.rockthejvm.reviewboard.http.requests.UpdatePasswordRequest


trait UserEndpoints extends BaseEndpoint {

    // POST /users { email, password} -> {email}
    val createUserEndpoint = 
        baseEndpoint
            .tag("Users")
            .name("register")
            .description("Register a user account with username and password")
            .in("users")
            .post
            .in(jsonBody[RegisterUserAccount]) // email: String, password: String
            .out(jsonBody[UserResponse])
    
    // PUT /users/password { email, oldPassword, newPassword } -> { email }
    // TODO - should be an authorized endpoint (JWT) need to be logged in
    val updatePasswordEndpoint = 
        secureBaseEndpoint // instead of "baseEndpoint" as it requires JWT authorization
            .tag("Users")
            .name("update password")
            .description("Update user password")
            .in("users" / "password")
            .put
            .in(jsonBody[UpdatePasswordRequest])
            .out(jsonBody[UserResponse])

    // DELETE /users { email, password } -> { email }
    // TODO authorized endpoint
    val deleteEndpoint = 
        secureBaseEndpoint
        .tag("Users")
        .name("delete")
        .description("Delete user account")
        .in("users")
        .delete
        .in(jsonBody[DeleteAccountRequest])
        .out(jsonBody[UserResponse])
    
    // POST /users { email, password } -> { email, accessToken, expiration }
    val loginEndpoint = 
        baseEndpoint
        .tag("Users")
        .name("login")
        .description("Log in and generate a JWT token")
        .in("users" / "login")
        .post
        .in(jsonBody[LoginRequest])
        .out(jsonBody[UserToken])
    
    // forgot password flow
    // POST /user/forgot { email } - 200 OK even if email doesn't exist in DB
    val forgotPasswordEndpoint = 
        baseEndpoint
            .tag("Users")
            .name("forgot password")
            .description("Trigger email for password recovery")
            .in("users" / "forgot")
            .post
            .in(jsonBody[ForgotPasswordRequest])
            
    // recover password
    // POST /users/recover { email , token, newPassword }
    val recoverPasswordEndpoint = 
        baseEndpoint
            .tag("Users")
            .name("recover password")
            .description("Set new password based on OTP")
            .in("users" / "recover")
            .post
            .in(jsonBody[RecoverPasswordRequest])

}
