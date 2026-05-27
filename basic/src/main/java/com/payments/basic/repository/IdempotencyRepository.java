package com.payments.basic.repository;

import com.payments.basic.domain.IdempotencyRecord;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IdempotencyRepository {

    private final Map<String, IdempotencyRecord> storage =
            new ConcurrentHashMap<>();

    public IdempotencyRecord find(String key) {
        return storage.get(key);
    }

    public void save(IdempotencyRecord record) {
        storage.put(record.getKey(), record);
    }
}