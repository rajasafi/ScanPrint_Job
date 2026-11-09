package com.printdoc.api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos(){}
    public record LoginRequest(@NotBlank @Email @Size(max=254) String email,@NotBlank @Size(min=8,max=128) String password){}
    public record UserView(UUID id,String email,String displayName,String role,String shopCode,String shopName){}
    public record LoginView(String accessToken,String tokenType,long expiresInSeconds,UserView user){}
    /** Shop code is optional; when blank the server derives a unique code from the shop name. */
    public record RegisterRequest(
        @NotBlank @Size(min=2,max=100) String shopName,
        @NotBlank @Size(min=2,max=100) String ownerName,
        @NotBlank @Email @Size(max=254) String email,
        @NotBlank @Size(min=8,max=128) String password,
        @Size(max=32) String shopCode,
        @Size(max=32) String phone,
        @Size(max=100) String printerBrand){}
}
