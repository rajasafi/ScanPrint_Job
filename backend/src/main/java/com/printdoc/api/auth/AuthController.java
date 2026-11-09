package com.printdoc.api.auth;

import com.printdoc.api.common.ApiResponse;
import com.printdoc.api.common.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService service; private final CurrentUser current;
    public AuthController(AuthService service,CurrentUser current){this.service=service;this.current=current;}
    @PostMapping("/shop/login")
    public ApiResponse<AuthDtos.LoginView> login(@Valid @RequestBody AuthDtos.LoginRequest request){return ApiResponse.ok("Signed in.",service.login(request));}
    @PostMapping("/shop/register") @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthDtos.LoginView> register(@Valid @RequestBody AuthDtos.RegisterRequest request){return ApiResponse.ok("Shop account created and signed in.",service.register(request));}
    @GetMapping("/me")
    public ApiResponse<AuthDtos.UserView> me(HttpServletRequest request){return ApiResponse.ok("Current account.",service.view(current.require(request)));}
}
