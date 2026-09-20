package com.foodiehub.catalog_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodiehub.catalog_service.dao.IdempotencyRecordDao;
import com.foodiehub.catalog_service.exception.IdempotencyKeyConflictException;
import com.foodiehub.catalog_service.exception.InvalidIdempotencyKeyException;
import com.foodiehub.catalog_service.model.IdempotencyRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRecordDao idempotencyRecordDao;
    private final ObjectMapper objectMapper;

    /**
     * Validate Idempotency-Key.
     */
    public void validateKey(String idempotencyKey) {

        if (idempotencyKey == null ||
                idempotencyKey.isBlank()) {

            throw new InvalidIdempotencyKeyException(
                    "Idempotency-Key header is required"
            );
        }

        if (idempotencyKey.length() > 100) {

            throw new InvalidIdempotencyKeyException(
                    "Idempotency-Key must not exceed 100 characters"
            );
        }
    }

    /**
     * Generate SHA-256 hash for the logical request.
     *
     * userId is included so that the same key cannot
     * accidentally be reused by another user.
     */
    public String generateRequestHash(
            UUID userId,
            Object request
    ) {

        try {

            String requestJson =
                    objectMapper.writeValueAsString(
                            new RequestHashData(
                                    userId,
                                    request
                            )
                    );

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            requestJson.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder result =
                    new StringBuilder();

            for (byte b : hash) {
                result.append(
                        String.format("%02x", b)
                );
            }

            return result.toString();

        } catch (NoSuchAlgorithmException |
                 JsonProcessingException e) {

            throw new RuntimeException(
                    "Unable to generate request hash",
                    e
            );
        }
    }

    /**
     * Find an already processed idempotency key.
     */
    @Transactional(readOnly = true)
    public Optional<IdempotencyRecord> findByKey(
            String idempotencyKey
    ) {

        return idempotencyRecordDao
                .findByIdempotencyKey(idempotencyKey);
    }

    /**
     * Verify that a reused idempotency key represents
     * exactly the same logical request.
     */
    public void validateExistingRecord(
            IdempotencyRecord record,
            String operation,
            String requestHash
    ) {

        if (!record.getOperation().equals(operation)) {

            throw new IdempotencyKeyConflictException(
                    "Idempotency key was already used for another operation"
            );
        }

        if (!record.getRequestHash().equals(requestHash)) {

            throw new IdempotencyKeyConflictException(
                    "Idempotency key cannot be reused with a different request"
            );
        }
    }

    /**
     * Store the response of a successfully processed request.
     */
    @Transactional
    public void saveResponse(
            String idempotencyKey,
            String operation,
            String requestHash,
            int responseStatus,
            Object responseBody
    ) {

        try {

            String responseJson =
                    objectMapper.writeValueAsString(
                            responseBody
                    );

            IdempotencyRecord record =
                    IdempotencyRecord.builder()
                            .idempotencyKey(idempotencyKey)
                            .operation(operation)
                            .requestHash(requestHash)
                            .responseStatus(responseStatus)
                            .responseBody(responseJson)
                            .build();

            idempotencyRecordDao.save(record);

        } catch (JsonProcessingException e) {

            throw new RuntimeException(
                    "Unable to store idempotency response",
                    e
            );
        }
    }

    /**
     * Convert the stored JSON response back to an Object.
     */
    public Object getStoredResponse(
            IdempotencyRecord record
    ) {

        try {

            return objectMapper.readValue(
                    record.getResponseBody(),
                    Object.class
            );

        } catch (JsonProcessingException e) {

            throw new RuntimeException(
                    "Unable to read stored idempotency response",
                    e
            );
        }
    }

    private record RequestHashData(
            UUID userId,
            Object request
    ) {
    }
}