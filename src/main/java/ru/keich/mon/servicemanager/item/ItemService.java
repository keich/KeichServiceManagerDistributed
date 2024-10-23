package ru.keich.mon.servicemanager.item;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.hazelcast.collection.IQueue;
import com.hazelcast.collection.ItemEvent;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;

import lombok.Getter;
import ru.keich.mon.servicemanager.BaseStatus;
import ru.keich.mon.servicemanager.QueueListner;
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
	public static final String INDEX_FIELD_ITEMTOEVENT = "itemToEvent[any]";
	
	public static final String QUEUE_ITEM_CHANGE_NAME = "queueItemChange";

	
	protected final IQueue<String> queueEventChange;
	protected final IQueue<String> queueEventRemoved;
	protected final IQueue<String> queueItemChange;
	private final EventService eventService;
	ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
	
	public ItemService(HazelcastInstance hazelcastInstance, EventService eventService) {
		super(NAME_ITEMS_MAP, hazelcastInstance);
		this.eventService = eventService;
		queueEventChange = hazelcastInstance.getQueue(EventService.QUEUE_EVENT_CHANGE_NAME);
		queueEventChange.addItemListener(new QueueListner<String>(this::eventChanged), true);
		queueEventRemoved = hazelcastInstance.getQueue(EventService.QUEUE_EVENT_REMOVED_NAME);
		queueEventRemoved.addItemListener(new QueueListner<String>(this::eventRemoved), true);
		queueItemChange = hazelcastInstance.getQueue(QUEUE_ITEM_CHANGE_NAME);
		queueItemChange.addItemListener(new QueueListner<String>(this::itemChanged), true);
	}

	@Override
	public void addOrUpdate(Item item) {
		var itemId = item.getId();
		lock(itemId, () -> item, old -> {
			item.setStatus(old.getStatus());
			item.setCreatedOn(old.getCreatedOn());
			item.setItemToEvent(old.getItemToEvent());
			return item;
		});
		queueItemChange.add(item.getId());
	}

	@Override
	protected Item entityRemoved(Item item) {
		findParentIdsById(item.getId()).forEach(queueItemChange::add);
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
			var eventIds = item.getItemToEvent().stream()
					.map(ItemToEvent::getEventId)
					.toList();
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
		var kv = new ItemToEvent(eventId, BaseStatus.CLEAR);
		var p = getPredicateEqual(INDEX_FIELD_ITEMTOEVENT, kv);
		return map.keySet(p);
	}
	
	private void addEventToItem(EventItemFilter eventItemFilter) {
		var filter = eventItemFilter.getFilter();
		var event = eventItemFilter.getEvent();
		var itemId = eventItemFilter.getItem().getId();
		var status = filter.isUsingResultStatus() ? filter.getResultStatus() : event.getStatus();
		lock(itemId, item -> {
			var rel = new ItemToEvent(event.getId(), status);
			item.getItemToEvent().remove(rel);
			item.getItemToEvent().add(rel);
			return Optional.of(item);
		});
		queueItemChange.add(itemId);
	}
	
	private void eventChanged(ItemEvent<String> info) {
		var opt = Optional.ofNullable(queueEventChange.poll()).map(eventService::findById);
		while (opt.isPresent()) {
			opt.map(this::findFiltersByEqualFields).orElse(Collections.emptyList()).forEach(this::addEventToItem);
			opt = Optional.ofNullable(queueEventChange.poll()).map(eventService::findById);
		}
	}
	
	private void eventRemoved(ItemEvent<String> info) {
		var opt = Optional.ofNullable(queueEventRemoved.poll());
		while (opt.isPresent()) {
			var eventId = opt.get();
			var ie = new ItemToEvent(eventId, BaseStatus.CLEAR);
			findItemIdsByEventId(eventId).stream()
					.forEach(itemId -> {
						lock(itemId, item -> {
							return Optional.of(item.getItemToEvent().remove(ie)).filter(ret -> ret).map(b -> item);
						}, queueItemChange::add);
					});

			opt = Optional.ofNullable(queueEventRemoved.poll());
		}
	}

	private void pushParentsForUpdate(String itemId) {
		findParentIdsById(itemId).forEach(queueItemChange::add);
	}
	
	private void itemChanged(ItemEvent<String> info) {
		var opt = Optional.ofNullable(queueItemChange.poll());
		while (opt.isPresent()) {
			lock(opt.get(), this::calculateStatus, this::pushParentsForUpdate);
			opt = Optional.ofNullable(queueItemChange.poll());
		}
	}

	private int calculateEntityStatusAsCluster(Item item, ItemRule rule) {
		final var overal = item.getChildrenIds().length;
		if (overal <= 0) {
			return 0;
		}
		this.findByIds(item.getChildrenIds());
		var listStatus = findByIds(item.getChildrenIds()).entrySet().stream()
				.map(Map.Entry::getValue)
				.mapToInt(child -> child.getStatus().ordinal())
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
		return findByIds(item.getChildrenIds()).entrySet().stream()
				.map(Map.Entry::getValue)
				.mapToInt(child -> child.getStatus().ordinal())
				.max().orElse(0);
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
		var maxEventStatus = item.getItemToEvent().stream()
				.mapToInt(ie -> ie.getStatus().ordinal())
				.max()
				.orElse(0);
		final var eventStatusMax = BaseStatus.fromInteger(maxEventStatus);
		maxStatus = maxStatus.max(eventStatusMax);
		if (maxStatus != item.getStatus()) {
			item.setStatus(maxStatus);
			return Optional.of(item);
		}
		return Optional.empty();
	}

}
