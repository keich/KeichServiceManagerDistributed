package ru.keich.mon.servicemanager.event;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.hazelcast.collection.IQueue;
import com.hazelcast.core.HazelcastInstance;

import ru.keich.mon.servicemanager.entity.EntityService;

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

@Service
public class EventService extends EntityService<String, Event>{
	
	static String NAME_EVENTS_MAP = "events";
	public static final String QUEUE_EVENT_CHANGE_NAME = "queueEventChange";
	public static final String QUEUE_EVENT_REMOVED_NAME = "queueEventRemoved";
	protected final IQueue<String> queueEventChange;
	protected final IQueue<String> queueEventRemoved;
	
	public EventService(HazelcastInstance hazelcastInstance) {
		super(NAME_EVENTS_MAP, hazelcastInstance);
		queueEventChange = hazelcastInstance.getQueue(QUEUE_EVENT_CHANGE_NAME);
		queueEventRemoved = hazelcastInstance.getQueue(QUEUE_EVENT_REMOVED_NAME);
	}
	
	@Override
	public void addOrUpdate(Event event) {
		var eventId = event.getId();
		lock(eventId, () -> event, old -> {
			event.setCreatedOn(old.getCreatedOn());
			event.setUpdatedOn(LocalDateTime.now());
			return event;
		});
		queueEventChange.add(event.getId());
	}

	@Override
	protected Event entityRemoved(Event event) {
		queueEventRemoved.add(event.getId());
		return event;
	}
	
}
