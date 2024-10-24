package ru.keich.mon.servicemanager.item;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;

import lombok.Getter;
import ru.keich.mon.servicemanager.BaseStatus;
import ru.keich.mon.servicemanager.QueueThreadReader;
import ru.keich.mon.servicemanager.StringKeyValue;
import ru.keich.mon.servicemanager.entity.EntityService;
import ru.keich.mon.servicemanager.event.Event;
import ru.keich.mon.servicemanager.event.EventService;

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
public class ItemService extends EntityService<String, Item> {
	
	public static final String NAME_ITEMS_MAP = "items";
	
	public static final String INDEX_FIELD_ALLEQUALFILTERS = "allFiltersEqualFields[any]";
	public static final String INDEX_FIELD_CHILDREN = "childrenIds[any]";
	public static final String INDEX_FIELD_ITEMTOEVENT = "events[keys]";
	public static final String INDEX_FIELD_CHILDRENSTATUS = "children[keys]";
	
	public static final String QUEUE_ITEM_CHANGE_NAME = "queueItemChange";

	
	protected final QueueThreadReader<String> queueEventChange;
	protected final QueueThreadReader<String> queueEventRemoved;
	protected final QueueThreadReader<ParentChild> queueItemChange;

	private final EventService eventService;
	ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
	
	public ItemService(HazelcastInstance hazelcastInstance,
			EventService eventService,
			@Value("${ru.keich.mon.servicemanager.item.queuethreadnumber:4}") Integer queueThredNumber) {
		super(NAME_ITEMS_MAP, hazelcastInstance);
		this.eventService = eventService;
		queueEventChange = new QueueThreadReader<String>(hazelcastInstance,
				EventService.QUEUE_EVENT_CHANGE_NAME,
				queueThredNumber,
				this::eventChanged);
		queueEventRemoved = new QueueThreadReader<String>(hazelcastInstance,
				EventService.QUEUE_EVENT_REMOVED_NAME,
				queueThredNumber,
				this::eventRemoved);
		queueItemChange = new QueueThreadReader<ParentChild>(hazelcastInstance,
				QUEUE_ITEM_CHANGE_NAME,
				queueThredNumber,
				this::childChanged);
	}

	@Override
	public void addOrUpdate(Item item) {
		var itemId = item.getId();
		lock(itemId, () -> item, old -> {
			item.setStatus(old.getStatus());
			item.setCreatedOn(old.getCreatedOn());
			item.setEventsStatus(old.getEventsStatus());
			item.setChildStatus(old.getChildStatus());
			return item;
		});
		queueItemChange.add(new ParentChild(item.getId(), ""));
	}

	@Override
	protected Item entityRemoved(Item item) {
		findParentIdsById(item.getId()).stream()
				.map(parentId -> new ParentChild(parentId, ""))
				.forEach(queueItemChange::add);
		return item;
	}

	static public <T> Predicate<String, Item> getPredicateEqual(String field, Comparable<T> k) {
		return Predicates.equal(field, k);
	}

	@Getter
	static class EventItemFilter {

		Event event;
		Item item;
		ItemFilter filter;

		public EventItemFilter(Event event, Item item, ItemFilter filter) {
			super();
			this.event = event;
			this.item = item;
			this.filter = filter;
		}

	}
	
	private List<EventItemFilter> findFiltersByEqualFields(Event event) {
		var fields = event.getFields();
		return fields.entrySet()
				.stream()
				.map(StringKeyValue::new)
				.map(kv -> getPredicateEqual(INDEX_FIELD_ALLEQUALFILTERS, kv))
				.flatMap(p -> map.values(p).stream())
				.distinct()
				.map(item -> {
					return item.getFilters()
							.entrySet()
							.stream()
							.filter(flt -> fields.entrySet().containsAll(flt.getValue().getEqualFields().entrySet()))
							.findFirst()
							.map(e -> new EventItemFilter(event, item, e.getValue()));
				})
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(Collectors.toList());
	}

	public List<String> findParentIdsById(String itemId) {
		return map.keySet(Predicates.equal(INDEX_FIELD_CHILDREN, itemId))
				.stream()
				.collect(Collectors.toList());
	}

	private Set<String> findEventIdsById(String parentId) {
		var out = new HashSet<String>();
		var history = new HashSet<String>();
		var queue = new ArrayDeque<String>();
		queue.add(parentId);
		while (!queue.isEmpty()) {
			final var itemId = queue.poll();
			if (history.contains(itemId)) {
				continue;
			}
			history.add(itemId);
			var item = map.get(itemId);
			if(Objects.isNull(item)) {
				continue;
			}
			var eventIds = item.getEventsStatus().keySet();
			out.addAll(eventIds);
			for(var childId: item.getChildrenIds()) {
				queue.add(childId);
			};
		}
		return out;
	}

	public List<Event> findAllEventsById(String itemId) {
		return findEventIdsById(itemId).stream()
				.map(eventService::findById)
				.filter(Objects::nonNull)
				.toList();
	}
	
	private Set<String> findItemIdsByEventId(String eventId) {
		var p = getPredicateEqual(INDEX_FIELD_ITEMTOEVENT, eventId);
		return map.keySet(p);
	}
	
	private void addEventToItem(EventItemFilter eventItemFilter) {
		var filter = eventItemFilter.getFilter();
		var event = eventItemFilter.getEvent();
		var itemId = eventItemFilter.getItem().getId();
		var status = filter.isUsingResultStatus() ? filter.getResultStatus() : event.getStatus();
		lock(itemId, item -> {
			item.getEventsStatus().put(event.getId(), status);
			return Optional.of(item);
		}, id -> queueItemChange.add(new ParentChild(id, "")));
	}
	
	private void removeEventFromItem(String itemId, String eventId) {
		lock(itemId, item -> {
			return Optional.of(item.getEventsStatus().remove(eventId)).map(b -> item);
		}, id -> queueItemChange.add(new ParentChild(id, "")));
	}
	
	
	private void eventChanged(String eventId) {
		Optional.ofNullable(eventService.findById(eventId))
				.ifPresent(event -> {
					findFiltersByEqualFields(event).forEach(this::addEventToItem);
				});
	}
	
	private void eventRemoved(String eventId) {
		findItemIdsByEventId(eventId).stream().forEach(itemId -> removeEventFromItem(itemId, eventId));
	}
	
	@Getter
	private static class ParentChild {
		
		String parentId;
		String childId;
		
		public ParentChild(String parentId, String childId) {
			super();
			this.parentId = parentId;
			this.childId = childId;
		}
		
	}

	private void pushParentsForUpdate(String childId) {
		findParentIdsById(childId).stream()
				.map(parentId -> new ParentChild(parentId, childId))
				.forEach(queueItemChange::add);
	}
	
	private void childChanged(ParentChild info) {
		var parentId = info.getParentId();
		var childId = info.getChildId();
		lock(parentId, parent -> {
			if (!"".equals(childId)) {
				var child = findById(childId);
				parent.getChildStatus().put(childId, child.getStatus());
				calculateStatus(parent);
				return Optional.of(parent);
			}
			return calculateStatus(parent);
		}, this::pushParentsForUpdate);
	}

	private int calculateEntityStatusAsCluster(Item item, ItemRule rule) {
		final var overal = item.getChildrenIds().length;
		if (overal <= 0) {
			return 0;
		}
		var listStatus = item.getChildStatus().entrySet().stream()
				.map(Map.Entry::getValue)
				.mapToInt(BaseStatus::ordinal)
				.boxed()
				.filter(i -> i >= rule.getStatusThreshold().ordinal())
				.toList();
		final var percent = 100 * listStatus.size() / overal;
		if (percent >= rule.getValueThreshold()) {
			if (rule.isUsingResultStatus()) {
				return rule.getResultStatus().ordinal();
			} else {
				return listStatus.stream().mapToInt(i -> i).min().orElse(0);
			}
		}
		return 0;
	}
	
	private int calculateEntityStatusDefault(Item item) {
		return item.getChildStatus().getMaxStatus().ordinal();
	}
	
	private int calculateStatusByChild(Item item) {
		var rules = item.getRules();
		return rules.entrySet().stream().mapToInt(e -> {
			var rule = e.getValue();
			switch (rule.getType()) {
			case CLUSTER:
				return calculateEntityStatusAsCluster(item, rule);
			default:
				return calculateEntityStatusDefault(item);
			}
		}).max().orElse(calculateEntityStatusDefault(item));
	}
	
	private Optional<Item> calculateStatus(Item item) {
		var maxStatus = BaseStatus.fromInteger(calculateStatusByChild(item));
		var eventStatusMax = item.getEventsStatus().getMaxStatus();
		maxStatus = maxStatus.max(eventStatusMax);
		if (maxStatus != item.getStatus()) {
			item.setStatus(maxStatus);
			return Optional.of(item);
		}
		return Optional.empty();
	}

}
