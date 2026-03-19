package com.deathstar.vader.loom.controller;

import com.deathstar.vader.api.TaskFieldsApi;
import com.deathstar.vader.dto.generated.TaskFieldDefinition;
import com.deathstar.vader.dto.generated.TaskFieldDefinitionRequest;
import com.deathstar.vader.loom.core.domain.BucketType;
import com.deathstar.vader.loom.core.domain.FieldDefinition;
import com.deathstar.vader.loom.service.TaskFieldDefinitionService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TaskFieldDefinitionController implements TaskFieldsApi {

    private final TaskFieldDefinitionService service;

    @Override
    public ResponseEntity<List<TaskFieldDefinition>> tasksFieldsGet() {
        List<FieldDefinition> domainFields = service.getAllFields();

        List<TaskFieldDefinition> dtos =
                domainFields.stream()
                        .map(
                                f ->
                                        new TaskFieldDefinition()
                                                .id(f.fieldId())
                                                .name(f.name())
                                                .fieldType(
                                                        com.deathstar.vader.dto.generated.FieldType
                                                                .valueOf(f.type().name()))
                                                .bucketType(
                                                        f.targetBucket() != null
                                                                ? com.deathstar.vader.dto.generated
                                                                        .BucketType.valueOf(
                                                                        f.targetBucket().name())
                                                                : null))
                        .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @Override
    public ResponseEntity<TaskFieldDefinition> tasksFieldsPost(TaskFieldDefinitionRequest request) {
        BucketType domainBucket =
                request.getBucketType() != null
                        ? BucketType.valueOf(request.getBucketType().name())
                        : null;

        FieldDefinition domainField =
                service.createField(
                        request.getName(),
                        FieldDefinition.FieldType.valueOf(request.getFieldType().name()),
                        domainBucket);

        TaskFieldDefinition dto =
                new TaskFieldDefinition()
                        .id(domainField.fieldId())
                        .name(domainField.name())
                        .fieldType(
                                com.deathstar.vader.dto.generated.FieldType.valueOf(
                                        domainField.type().name()))
                        .bucketType(
                                domainField.targetBucket() != null
                                        ? com.deathstar.vader.dto.generated.BucketType.valueOf(
                                                domainField.targetBucket().name())
                                        : null);

        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
}
