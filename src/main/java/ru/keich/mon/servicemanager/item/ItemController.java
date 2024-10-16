package ru.keich.mon.servicemanager.item;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.java.Log;
import ru.keich.mon.servicemanager.entity.EntityController;

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

@RestController
@RequestMapping("/api/v1")
@Log
public class ItemController extends EntityController<String, Item> {

	public static final String FILTER_NAME = "propertiesFilter";
	public static final String QUERY_CHILDREN = "children";
	public static final String QUERY_PARENTS = "parents";

	private ItemService itemService;

	public ItemController(@Autowired ItemService itemService) {
		super(itemService);
		this.itemService = itemService;
	}

	@PostMapping("/item")
	@Override
	public ResponseEntity<String> addOrUpdate(@RequestBody List<Item> items) {
		var dateTime = LocalDateTime.now();
		items.forEach(item -> {
			if (Objects.isNull(item.getCreatedOn())) {
				item.setCreatedOn(dateTime);
			}
			if (Objects.isNull(item.getUpdatedOn())) {
				item.setUpdatedOn(dateTime);
			}
		});
		return super.addOrUpdate(items);
	}

	@GetMapping("/item")
	@CrossOrigin(origins = "*")
	@Override
	public ResponseEntity<MappingJacksonValue> query(@RequestParam MultiValueMap<String, String> reqParam) {
		return super.query(reqParam);
	}

	@GetMapping("/item/{id}")
	@CrossOrigin(origins = "*")
	@Override
	public ResponseEntity<MappingJacksonValue> findById(@PathVariable String id
			,@RequestParam MultiValueMap<String, String> reqParam) {
		return super.findById(id, reqParam);
	}

	@GetMapping("/item/{id}/children")
	@CrossOrigin(origins = "*")
	public ResponseEntity<MappingJacksonValue> findChildrenById(@PathVariable String id
			,@RequestParam MultiValueMap<String, String> reqParam) {
		var children = Optional.ofNullable(itemService.findById(id))
				.map(Item::getChildrenIds)
				.orElse(Collections.emptySet())
				.stream()
				.map(itemService::findById)
				.filter(Objects::nonNull)
				.toList();
		return applyFilter(new MappingJacksonValue(children), reqParam);
	}

	@GetMapping("/item/{id}/parents")
	@CrossOrigin(origins = "*")
	public ResponseEntity<MappingJacksonValue> findParentsById(@PathVariable String id
			,@RequestParam MultiValueMap<String, String> reqParam) {
		var parents = itemService.findParentIdsById(id).stream()
				.map(itemService::findById)
				.filter(Objects::nonNull)
				.toList();
		return applyFilter(new MappingJacksonValue(parents), reqParam);
	}

	@DeleteMapping("/item")
	@CrossOrigin(origins = "*")
	@Override
	public ResponseEntity<Integer> deleteByFilter(@RequestBody(required = false) List<String> ids
			,@RequestParam Map<String, String> reqParam) {
		return super.deleteByFilter(ids, reqParam);
	}
	
	@GetMapping("/item/{id}/events")
	@CrossOrigin(origins = "*")
	public ResponseEntity<MappingJacksonValue> findAllEventsById(@PathVariable String id
			,@RequestParam MultiValueMap<String, String> reqParam) {
		return applyFilter(new MappingJacksonValue(itemService.findAllEventsById(id)), reqParam);
	}

	@GetMapping("/item/{id}/tree")
	@CrossOrigin(origins = "*")
	// TODO rename children/tree
	public ResponseEntity<MappingJacksonValue> getTree(@PathVariable String id
			,@RequestParam MultiValueMap<String, String> reqParam) {
		if (reqParam.containsKey(QUERY_PROPERTY)) {
			reqParam.add(QUERY_PROPERTY, QUERY_CHILDREN);
		}
		return Optional.ofNullable(itemService.findById(id))
				.map(parent -> setChildren(parent, new HashSet<String>()))
				.map(parent -> applyFilter(new MappingJacksonValue(parent), reqParam))
				.orElse(ResponseEntity.notFound().build());
	}

	private Item setChildren(Item parent, HashSet<String> history) {
		var children = itemService.findByIds(parent.getChildrenIds())
				.entrySet()
				.stream()
				.map(Map.Entry::getValue)
				.toList();
		history.add(parent.getId());
		children.forEach(child -> {
			if (history.contains(child.getId())) {
				log.warning("setChildren: circle found from " + parent.getId() + " to " + child.getId());
			} else {
				setChildren(child, history);
			}
		});
		parent.setChildren(children);
		history.remove(parent.getId());
		return parent;
	}

	@GetMapping("/item/{id}/parents/tree")
	@CrossOrigin(origins = "*")
	public ResponseEntity<MappingJacksonValue> findParentsTreeById(@PathVariable String id
			,@RequestParam MultiValueMap<String, String> reqParam) {
		if (reqParam.containsKey(QUERY_PROPERTY)) {
			reqParam.add(QUERY_PROPERTY, QUERY_PARENTS);
		}
		return Optional.ofNullable(itemService.findById(id))
				.map(child -> setParents(child, new HashSet<String>()))
				.map(child -> applyFilter(new MappingJacksonValue(child), reqParam))
				.orElse(ResponseEntity.notFound().build());
	}

	private Item setParents(Item child, HashSet<String> history) {
		var parents = itemService.findParentIdsById(child.getId()).stream()
				.map(itemService::findById)
				.filter(Objects::nonNull)
				.toList();
		history.add(child.getId());
		parents.forEach(parent -> {
			if (history.contains(parent.getId())) {
				log.warning("setParents: circle found from " + parent.getId() + " to " + child.getId());
			} else {
				setParents(parent, history);
			}
		});
		child.setParents(parents);
		history.remove(child.getId());
		return child;
	}

}
