package com.smartops.planner.skill;

import com.smartops.planner.skill.dto.CreateSkillRequest;
import com.smartops.planner.skill.dto.SkillResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/skills")
@Tag(name = "Skills", description = "Skill catalog management")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping
    @Operation(summary = "List skills")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Skills returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role")
    })
    public List<SkillResponse> findAll() {
        return skillService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find skill by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Skill found"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "Skill not found")
    })
    public SkillResponse findById(@PathVariable Long id) {
        return skillService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Create skill")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Skill created"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "409", description = "Skill already exists")
    })
    public ResponseEntity<SkillResponse> create(@Valid @RequestBody CreateSkillRequest request) {
        SkillResponse response = skillService.create(request);
        return ResponseEntity
                .created(URI.create("/api/skills/" + response.id()))
                .body(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete skill")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Skill deleted"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "Skill not found")
    })
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        skillService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
