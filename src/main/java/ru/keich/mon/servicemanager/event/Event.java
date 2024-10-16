package ru.keich.mon.servicemanager.event;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import ru.keich.mon.servicemanager.BaseStatus;

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
public class Event {
	
	public enum EventType {
		NOTSET, PROBLEM, RESOLUTION, INFORMATION
	}
	
	private final String id;
	
	private final String source;
	
	private final String sourceKey;
	
	private final EventType type;
	
	private final BaseStatus status;

	private Instant createdOn = Instant.now();
	
	private Instant updatedOn = Instant.now();
	
	private Map<String, String> fields = Collections.emptyMap();
	
	private String node = "";
	
	private String summary = "";
	
	@JsonCreator
	public Event(
			@JsonProperty(value = "id", required = true) String id,
			@JsonProperty(value = "source", required = true) String source,
			@JsonProperty(value = "sourceKey", required = true) String sourceKey,
			@JsonProperty(value = "type", required = true) EventType type,
			@JsonProperty(value = "status", required = true) BaseStatus status) {
		super();
		this.id = id;
		this.source = source;
		this.sourceKey = sourceKey;
		this.type = type;
		this.status = status;
	}
	
}
