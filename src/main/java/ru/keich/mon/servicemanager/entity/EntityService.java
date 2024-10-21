package ru.keich.mon.servicemanager.entity;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.hazelcast.config.IndexType;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;

import ru.keich.mon.servicemanager.query.Filter;

/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public abstract class EntityService<K, T> {
	
	public static final String INDEX_FIELD_SOURCE = "source";
	public static final String INDEX_FIELD_SOURCE_KEY = "sourceKey";

	protected final IMap<K, T> map;
	
	public EntityService(String mapName, HazelcastInstance hazelcastInstance) {
		map = hazelcastInstance.getMap(mapName);
		map.addIndex(IndexType.HASH, INDEX_FIELD_SOURCE);
		map.addIndex(IndexType.HASH, INDEX_FIELD_SOURCE_KEY);
	}

	protected void lock(K entityId, Supplier<T> insert, Function<T, T> update) {
		map.lock(entityId);
		try {
			var entity = Optional.ofNullable(map.get(entityId))
					.map(update::apply)
					.orElseGet(insert);
			map.set(entityId, entity);
		} finally {
			map.unlock(entityId);
		}
	}
	
	protected void lock(K entityId, Function<T, Optional<T>> update) {
		map.lock(entityId);
		try {
			Optional.ofNullable(map.get(entityId))
					.flatMap(update::apply)
					.ifPresent(entity -> map.set(entityId, entity));
		} finally {
			map.unlock(entityId);
		}
	}
	
	public void lock(K entityId, Function<T, Optional<T>> update, Consumer<K> after) {
		map.lock(entityId);
		final boolean changed;
		try {
			changed = Optional.ofNullable(map.get(entityId))
					.flatMap(update::apply)
					.map(entity -> {
						map.set(entityId, entity);
						return true;
					}).orElse(false);
		} finally {
			map.unlock(entityId);
		}
		if(changed) {
			after.accept(entityId);
		}
	}
	
	protected abstract T entityRemoved(T entity);

	public abstract void addOrUpdate(T entity);

	public T findById(K entityId) {
		return map.get(entityId);
	}

	public Map<K, T> findByIds(Set<K> keys) {
		return map.getAll(keys);
	}

	public Optional<T> deleteById(K entityId) {
		return Optional.ofNullable(map.remove(entityId)).map(this::entityRemoved);
	}

	public List<T> deleteByIds(List<K> ids) {
		return ids.stream()
				.map(this::deleteById)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(Collectors.toList());
	}

	public List<T> deleteBySourceAndSourceKeyNot(String source, String sourceKey) {
		var pSource = Predicates.equal(INDEX_FIELD_SOURCE, source);
		var pSourceKey = Predicates.notEqual(INDEX_FIELD_SOURCE_KEY, sourceKey);
		var ids = map.keySet(Predicates.and(pSource, pSourceKey));
		return ids.stream()
				.map(this::deleteById)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(Collectors.toList());
	}

	public Predicate<K, T> getPredicate(Filter filter) {
		switch (filter.getOperator()) {
		case NE:
			return Predicates.notEqual(filter.getName(), filter.getValue());
		case EQ:
			return Predicates.equal(filter.getName(), filter.getValue());
		case LT:
			return Predicates.lessThan(filter.getName(), filter.getValue());
		case GT:
			return Predicates.greaterEqual(filter.getName(), filter.getValue());
		case CO:
			return Predicates.ilike(filter.getName(), "%" + filter.getValue() + "%");
		case NC:
			return Predicates.not(Predicates.ilike(filter.getName(), "%" + filter.getValue() + "%"));
		default:
			throw new UnsupportedOperationException("Operator not supported");
		}
	}

	public List<T> query(List<Filter> filters) {
		var arr = filters.stream().map(f -> getPredicate(f)).toArray(Predicate[]::new);
		Predicate<K, T> p = Predicates.and(arr);
		return map.values(p).stream().toList();
	}
	
}
