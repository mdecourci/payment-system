package com.payments.basic.repository;

import com.payments.basic.model.PaymentException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Generic thread-safe in-memory store.
 * Replace with a JPA/JDBC implementation in production — the interface stays the same.
 */
public class InMemoryStore<T> {

    private final Map<String, T> store = new ConcurrentHashMap<>();

    public T save(String id, T entity) {
        store.put(id, entity);
        return entity;
    }

    public Optional<T> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public T findByIdOrThrow(String id, String entityName) {
        return findById(id).orElseThrow(() -> new PaymentException.NotFound(entityName, id));
    }

    public List<T> findAll() {
        return List.copyOf(store.values());
    }

    public List<T> findWhere(Predicate<T> predicate) {
        return store.values().stream().filter(predicate).toList();    // Java 16+ toList() on Stream
    }

    public Stream<T> stream() {
        return store.values().stream();
    }

    public boolean existsById(String id) {
        return store.containsKey(id);
    }

    public long count() {
        return store.size();
    }

    public void delete(String id) {
        store.remove(id);
    }
}
