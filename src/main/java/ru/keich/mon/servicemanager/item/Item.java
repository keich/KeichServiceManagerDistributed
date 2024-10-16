package ru.keich.mon.servicemanager.item;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import ru.keich.mon.servicemanager.BaseStatus;
import ru.keich.mon.servicemanager.StringKeyValue;

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

@JsonFilter("propertiesFilter")
@Getter
@Setter
public class Item {

	private final String id;

	private final String source;

	private final String sourceKey;

	private Instant createdOn = Instant.now();

	private Instant updatedOn = Instant.now();

	private Map<String, String> fields = Collections.emptyMap();

	private BaseStatus status = BaseStatus.CLEAR;

	private Map<String, ItemRule> rules = Collections.emptyMap();

	private Map<String, ItemFilter> filters = Collections.emptyMap();

	private Set<String> childrenIds = Collections.emptySet();
	
	transient
	private List<Item> children = Collections.emptyList();
	
	transient
	private List<Item> parents = Collections.emptyList();

	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private boolean hasChildren = false;

	private String name = "";
	
	@JsonIgnore
	private Set<ItemToEvent> itemToEvent = Collections.emptySet();

	@JsonIgnore
	private Set<StringKeyValue> allFiltersEqualFields = Collections.emptySet();

	@JsonCreator
	public Item(
			@JsonProperty(value = "id", required = true) String id,
			@JsonProperty(value = "source", required = true) String source,
			@JsonProperty(value = "sourceKey", required = true) String sourceKey) {
		super();
		this.id = id;
		this.source = source;
		this.sourceKey = sourceKey;
	}
	
	public void setChildrenIds(Set<String> childrenIds) {
		this.childrenIds = childrenIds;
		if(childrenIds.size() > 0) {
			this.hasChildren = true;
		} else {
			this.hasChildren = false;
		}
	}

	public void setAllFiltersEqualFields(Set<StringKeyValue> allFiltersEqualFields) {
		throw new RuntimeException("setAllFiltersEqualFields");
	}
	
	public void setFilters(Map<String, ItemFilter> filters) {
		this.filters = filters;
		allFiltersEqualFields = filters.entrySet().stream()
		.flatMap(e -> e.getValue().getEqualFields().entrySet().stream())
		.map(StringKeyValue::new)
		.collect(Collectors.toSet());
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Item other = (Item) obj;
		return Objects.equals(id, other.id);
	}

}