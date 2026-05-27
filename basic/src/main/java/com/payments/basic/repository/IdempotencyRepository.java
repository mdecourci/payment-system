package com.payments.basic.repository;

import com.payments.basic.model.IdempotencyRecord;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class IdempotencyRepository {

    private final Map<String, IdempotencyRecord> storage = new ConcurrentHashMap<>();

    public Optional<IdempotencyRecord> find(String key) {

        return Optional.ofNullable(storage.get(key));
    }

    public void save(IdempotencyRecord record) {

        storage.put(record.key(), record);
    }
}