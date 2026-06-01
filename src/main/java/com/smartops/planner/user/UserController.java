package com.smartops.planner.user;

import com.smartops.planner.auth.dto.RegisterRequest;
import com.smartops.planner.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Application users and roles")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "List users")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role")
    })
    public List<UserResponse> findAll() {
        return userService.findAll();
    }

    @PostMapping
    @Operation(summary = "Create user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "409", description = "Username already exists")
    })
    public ResponseEntity<UserResponse> create(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = userService.create(request);
        return ResponseEntity
                .created(URI.create("/api/users/" + response.id()))
                .body(response);
    }
}
