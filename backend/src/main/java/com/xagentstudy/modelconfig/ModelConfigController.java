package com.xagentstudy.modelconfig;

import com.xagentstudy.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/model-configs")
public class ModelConfigController {
    private final ModelConfigService service;

    public ModelConfigController(ModelConfigService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<ModelConfigResponse>> list() {
        return ApiResponse.ok(service.list());
    }

    @PostMapping
    public ApiResponse<ModelConfigResponse> save(@Valid @RequestBody SaveModelConfigRequest request) {
        return ApiResponse.ok(service.save(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ModelConfigResponse> update(@PathVariable Long id, @Valid @RequestBody SaveModelConfigRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }
}
