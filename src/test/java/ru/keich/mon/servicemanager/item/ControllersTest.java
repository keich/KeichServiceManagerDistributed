package ru.keich.mon.servicemanager.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ru.keich.mon.servicemanager.BaseStatus;
import ru.keich.mon.servicemanager.event.Event;
import ru.keich.mon.servicemanager.event.Event.EventType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ControllersTest {
	
	ObjectMapper mapper = new ObjectMapper();
	
	@Autowired
	private TestRestTemplate restTemplate;

	private List<Event> eventGetBySourceEqual(String source) {
		var result = restTemplate.exchange("/api/v1/event?source=eq:" + source,
				HttpMethod.GET, null, new ParameterizedTypeReference<List<Event>>() {});
		assertEquals(HttpStatus.OK, result.getStatusCode());
		return result.getBody();
	}
	
	private List<Event> eventGetBySourceNotEqual(String source) {
		var result = restTemplate.exchange("/api/v1/event?source=ne:" + source,
				HttpMethod.GET, null, new ParameterizedTypeReference<List<Event>>() {});
		assertEquals(HttpStatus.OK, result.getStatusCode());
		return result.getBody();
	}
	
	private List<Event> eventGetBySourceKeyEqual(String sourceKey) {
		var result = restTemplate.exchange("/api/v1/event?sourceKey=eq:" + sourceKey,
				HttpMethod.GET, null, new ParameterizedTypeReference<List<Event>>() {});
		assertEquals(HttpStatus.OK, result.getStatusCode());
		return result.getBody();
	}
	
	private List<Event> eventGetBySourceKeyNotEqual(String sourceKey) {
		var result = restTemplate.exchange("/api/v1/event?sourceKey=ne:" + sourceKey,
				HttpMethod.GET, null, new ParameterizedTypeReference<List<Event>>() {});
		assertEquals(HttpStatus.OK, result.getStatusCode());
		return result.getBody();
	}
	
	private  List<Item> itemGetBySourceEqual(String source) {
		var result = restTemplate.exchange("/api/v1/item?source=eq:" + source,
				HttpMethod.GET, null, new ParameterizedTypeReference<List<Item>>() {});
		assertEquals(HttpStatus.OK, result.getStatusCode());
		return result.getBody();
	}
	
	private List<Item> itemGetBySourceNotEqual(String source) {
		var result = restTemplate.exchange("/api/v1/item?source=ne:" + source,
				HttpMethod.GET, null, new ParameterizedTypeReference<List<Item>>() {});
		assertEquals(HttpStatus.OK, result.getStatusCode());
		return result.getBody();
	}
	
	private  List<Item> itemGetBySourceKeyEqual(String sourceKey) {
		var result = restTemplate.exchange("/api/v1/item?sourceKey=eq:" + sourceKey,
				HttpMethod.GET, null, new ParameterizedTypeReference<List<Item>>() {});
		assertEquals(HttpStatus.OK, result.getStatusCode());
		return result.getBody();
	}
	
	private List<Item> itemGetBySourceKeyNotEqual(String sourceKey) {
		var result = restTemplate.exchange("/api/v1/item?sourceKey=ne:" + sourceKey,
				HttpMethod.GET, null, new ParameterizedTypeReference<List<Item>>() {});
		assertEquals(HttpStatus.OK, result.getStatusCode());
		return result.getBody();
	}
	
	private Event[] itemGetEvents(String id) {
		var result = restTemplate.getForEntity("/api/v1/item/" + id + "/events", Event[].class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		return result.getBody();
	}
	
	private Item[] itemGetChildren(String id) {
		var result = restTemplate.getForEntity("/api/v1/item/" + id + "/children", Item[].class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		return result.getBody();
	}
	
	private Item itemGetTree(String id) {
		var result = restTemplate.getForEntity("/api/v1/item/" + id + "/tree", Item.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		return result.getBody();
	}

	private <T> void entityAdd(String path, T entiry) {
		var headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		var list = new ArrayList<T>();
		list.add(entiry);
		var result = restTemplate.postForEntity("/api/v1" + path, list, String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
	}
	
	private <T> void entityAdd(String path, T []list) {
		var headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		var result = restTemplate.postForEntity("/api/v1" + path, list, String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
	}

	private <K, T> void entityRemove(String path, List<K> ids) throws JsonProcessingException {
		var requestJson = mapper.writeValueAsString(ids);
		var headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		var entity = new HttpEntity<String>(requestJson, headers);
		var result = restTemplate.exchange("/api/v1" + path, HttpMethod.DELETE, entity, String.class);
		assertEquals(HttpStatus.OK, result.getStatusCode());
	}
	
	private <T> T entityGetById(String path, String id, Class<T> responseType) {
		var result = restTemplate.getForEntity("/api/v1" + path + "/" + id, responseType);
		assertEquals(HttpStatus.OK, result.getStatusCode());
		return result.getBody();
	}

	private <T> void entityAddAndGet(String path, String id, T entity, Class<T> entityType, Consumer<T> trigger)
			throws IOException {
		entityAdd(path, entity);
		var result = entityGetById(path, id, entityType);
		trigger.accept(result);
	}

	private void entityDeleteBySoyrceAndSourceKeyNot(String path, Item entity1, Item entity2) {
		entityAdd(path, entity1);
		entityAdd(path, entity2);
		restTemplate.delete("/api/v1" + path + "?query=bySourceAndSourceKeyNot&source=" + entity2.getSource()
				+ "&sourceKey=" + entity2.getSourceKey());

		var result = restTemplate.getForEntity("/api/v1/item/" + entity1.getId(), Item.class);
		assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
		entityGetById(path, entity2.getId().toString(), Item.class);
	}
	
	private void entityDeleteBySoyrceAndSourceKeyNot(String path, Event entity1, Event entity2) {
		entityAdd(path, entity1);
		entityAdd(path, entity2);
		restTemplate.delete("/api/v1" + path + "?query=bySourceAndSourceKeyNot&source=" + entity2.getSource()
				+ "&sourceKey=" + entity2.getSourceKey());

		var result = restTemplate.getForEntity("/api/v1/item/" + entity1.getId(), Item.class);
		assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
		entityGetById(path, entity2.getId().toString(), Item.class);
	}

	@Test
	public void itemAddAndGet() throws IOException {
		var json = """
				    {
				        "id": "id_itemAddAndGet1",
				        "source": "src_itemAddAndGet",
				        "sourceKey": "src_key_itemAddAndGet",
				        "fields": {
				            "name": "Hello",
				            "description": "World"
				        },
				        "rules": {},
				        "filters": {
				            "by_identity": {
				                "resultStatus": "INDETERMINATE",
				                "usingResultStatus": false,
				                "equalFields": {"identity": "68FB40A414A49978832133B9D476E5A" }
				            }
				        }
				    }
				""";
		var item = mapper.readValue(json, Item.class);
		entityAddAndGet("/item", item.getId(), item, Item.class, i -> {
			assertEquals(item.getId(), i.getId());
			assertEquals(item.getSource(), i.getSource());
			assertEquals(item.getSourceKey(), i.getSourceKey());
			assertEquals(item.getFields().get("name"), i.getFields().get("name"));
			assertEquals(item.getFields().get("description"), i.getFields().get("description"));
		});
	}

	@Test
	public void itemGetChildren() throws IOException {
		var jsonParent = """
					{
				        "id": "id_itemGetChildren1",
				        "source": "src_itemGetChildren",
				        "sourceKey": "src_key_itemGetChildren",
				        "childrenIds": ["id_itemGetChildren2","testid"]
				    }
				""";
		var itemParent = mapper.readValue(jsonParent, Item.class);

		var jsonChild = """
					{
				        "id": "id_itemGetChildren2",
				        "source": "src_itemGetChildren",
				        "sourceKey": "src_key_itemGetChildren"
				    }
				""";
		var itemChild = mapper.readValue(jsonChild, Item.class);

		entityAdd("/item", itemParent);
		entityAdd("/item", itemChild);

		var items = itemGetChildren(itemParent.getId());
		assertEquals(1, items.length);
		assertEquals(itemChild.getId(), items[0].getId());
	}
	
	@Test
	public void itemSourceFilter() throws IOException {
		final var id = "id_itemSourceFilter";
		final var source = "src_itemSourceFilter";
		final var sourceKey = "src_key_itemSourceFilter";
		var item = new Item(id, source, sourceKey);
		var item1 = new Item(id + "1", source + "1", sourceKey + "1");
		entityAdd("/item", item);
		entityAdd("/item", item1);
		
		var items = itemGetBySourceEqual(source);
		assertEquals(1, items.size());
		var retIitem = items.get(0);
		assertEquals(item.getId(), retIitem.getId());
		
		items = itemGetBySourceNotEqual(source);
		assertNotEquals(0, items.size());
		
		items.forEach(i -> {
			assertNotEquals(item.getId(), i.getId());
		});
		
	}
	
	@Test
	public void itemSourceKeyFilter() throws IOException {
		final var id = "id_itemSourceKeyFilter";
		final var source = "src_itemSourceKeyFilter";
		final var sourceKey = "src_key_itemSourceKeyFilter";
		
		var item = new Item(id, source, sourceKey);

		var item1 = new Item(id + "1", source + "1", sourceKey + "1");
		
		entityAdd("/item", item);
		entityAdd("/item", item1);
		
		var items = itemGetBySourceKeyEqual(sourceKey);
		assertEquals(1, items.size());
		var retIitem = items.get(0);
		assertEquals(item.getId(), retIitem.getId());
		
		items = itemGetBySourceKeyNotEqual(sourceKey);
		assertNotEquals(0, items.size());
		
		items.forEach(i -> {
			assertNotEquals(item.getId(), i.getId());
		});
		
	}
	
	@Test
	public void itemDeleteBySoyrceAndSourceKeyNot() throws IOException {
		final var id = "id_itemDeleteBySoyrceAndSourceKeyNot";
		final var source = "src_itemDeleteBySoyrceAndSourceKeyNot";
		final var sourceKey = "src_key_itemDeleteBySoyrceAndSourceKeyNot";
		
		var item1 = new Item(id + "1", source, sourceKey);

		var item2 = new Item(id + "2", source, sourceKey + "_new");
		
		entityDeleteBySoyrceAndSourceKeyNot("/item", item1, item2);
	}

	@Test
	public void itemFiltersEventMapping()  throws IOException, InterruptedException {
		var json = """
				 {
			        "id": "id_itemFiltersEventMapping",
			        "source": "src_itemFiltersEventMapping",
			        "sourceKey": "src_key_itemFiltersEventMapping",
			        "fields": {
			            "name": "Hello",
			            "description": "World"
			        },
			        "rules": {},
			        "filters": {
			            "by_identity": {
			                "resultStatus": "INDETERMINATE",
			                "usingResultStatus": false,
			                "equalFields": { "identity":"68FB40A414A49978832133B9D476E5A1", "manager": "SNMP"}
			            }
			        }
			    }
			""";
		var item = mapper.readValue(json, Item.class);
		entityAdd("/item", item);
		
		json = """
				 {
			        "id": "id_itemFiltersEventMappingROOT",
			        "source": "src_itemFiltersEventMapping",
			        "sourceKey": "src_key_itemFiltersEventMapping",
			        "fields": {
			            "name": "root",
			            "description": "root"
			        },
			        "childrenIds": ["id_itemFiltersEventMapping"]
			    }
			""";
		var itemRoot = mapper.readValue(json, Item.class);
		entityAdd("/item", itemRoot);
		
		json = """
			    {
			    	"id": "id_itemFiltersEventMappingEvent1",
			        "type": "PROBLEM",
			        "status": "WARNING",
			        "source": "source_itemFiltersEventMapping",
			        "sourceKey": "sourceKey_itemFiltersEventMapping",
			        "fields": {
			            "server": "localhost",
			            "summary": "Hello World",
			            "identity": "68FB40A414A49978832133B9D476E5A1",
			            "manager": "SNMP"
			        }
			    }
			""";
		var event1 = mapper.readValue(json, Event.class);
		entityAdd("/event", event1);
		Thread.sleep(1000);
		var events = itemGetEvents(itemRoot.getId());
		
		assertTrue(events.length > 0);
		
		json = """
			    {
			    	"id": "id_itemFiltersEventMappingEvent2",
			        "type": "PROBLEM",
			        "status": "CLEAR",
			        "source": "source_itemFiltersEventMapping",
			        "sourceKey": "sourceKey_itemFiltersEventMapping",
			        "fields": {
			            "server": "localhost",
			            "summary": "Hello World",
			            "identity": "68FB40A414A49978832133B9D476E5A1",
			            "manager": "SNMP"
			        }
			    }
			""";
		
		
		var event2 = mapper.readValue(json, Event.class);
		entityAdd("/event", event2);
		
		events = itemGetEvents(item.getId());
		assertTrue(events.length > 0);

		
		
		events = itemGetEvents(itemRoot.getId());

		var item2 = entityGetById("/item", item.getId(), Item.class);
		
		assertEquals(BaseStatus.WARNING, item2.getStatus());
		
		json = """
			    {
			    	"id": "id_itemFiltersEventMappingEvent3",
			        "type": "PROBLEM",
			        "status": "CRITICAL",
			        "source": "source_itemFiltersEventMapping",
			        "sourceKey": "sourceKey_itemFiltersEventMapping",
			        "fields": {
			            "server": "localhost",
			            "summary": "Hello World",
			            "identity": "68FB40A414A49978832133B9D476E5A1",
			            "manager": "SNMP"
			        }
			    }
			""";
		
		var event3 = mapper.readValue(json, Event.class);
		
		entityAdd("/event", event3);
		Thread.sleep(1000);
		
		item2= entityGetById("/item", item.getId(), Item.class);
		assertEquals(BaseStatus.CRITICAL, item2.getStatus());
		
		var ids = new ArrayList<String>();
		ids.add(event1.getId());
		ids.add(event2.getId());
		ids.add(event3.getId());

		entityRemove("/event", ids);
		Thread.sleep(1000);
		events = itemGetEvents(item.getId());
		
		assertTrue(events.length == 0);
		
		item2= entityGetById("/item", item.getId(), Item.class);
		assertEquals(BaseStatus.CLEAR, item2.getStatus());
		
		
		var item3 = entityGetById("/item", itemRoot.getId(), Item.class);
		assertEquals(BaseStatus.CLEAR, item3.getStatus());
	}		
	
	@Test
	public void itemEventClear()  throws IOException, InterruptedException {
		var json = """
				 {
			        "id": "itemEventClearItem",
			        "source": "src_itemFiltersEventMapping",
			        "sourceKey": "src_key_itemFiltersEventMapping",
			        "fields": {
			            "name": "Hello",
			            "description": "World"
			        },
			        "rules": {},
			        "filters": {
			            "by_identity": {
			                "resultStatus": "INDETERMINATE",
			                "usingResultStatus": false,
			                "equalFields": { "identity": "68FB40A414A49978832133B9D476E5A1","manager":"SNMP" }
			            }
			        }
			    }
			""";
		var item = mapper.readValue(json, Item.class);
		entityAdd("/item", item);
		Thread.sleep(1000);
		
		json = """
			    {
			    	"id": "itemEventClearEvent",
			        "type": "PROBLEM",
			        "status": "WARNING",
			        "source": "source_itemFiltersEventMapping",
			        "sourceKey": "sourceKey_itemFiltersEventMapping",
			        "fields": {
			            "server": "localhost",
			            "summary": "Hello World",
			            "identity": "68FB40A414A49978832133B9D476E5A1",
			            "manager": "SNMP"
			        }
			    }
			""";
		var event1 = mapper.readValue(json, Event.class);
		entityAdd("/event", event1);
		Thread.sleep(1000);
		

		var item2= entityGetById("/item", item.getId(), Item.class);
		assertEquals(BaseStatus.WARNING, item2.getStatus());
		
		json = """
			    {
			    	"id": "itemEventClearEvent",
			        "type": "PROBLEM",
			        "status": "CLEAR",
			        "source": "source_itemFiltersEventMapping",
			        "sourceKey": "sourceKey_itemFiltersEventMapping",
			        "fields": {
			            "server": "localhost",
			            "summary": "Hello World",
			            "identity": "68FB40A414A49978832133B9D476E5A1",
			            "manager": "SNMP"
			        }
			    }
			""";
		event1 = mapper.readValue(json, Event.class);
		entityAdd("/event", event1);
		Thread.sleep(1000);
		item2= entityGetById("/item", item.getId(), Item.class);
		assertEquals(BaseStatus.CLEAR, item2.getStatus());
	}
	
	@Test
	public void eventAddAndGet() throws IOException {
		var json = """
				    {
				    	"id": "id_eventAddAndGet",
				        "type": "PROBLEM",
				        "status": "WARNING",
				        "source": "source_eventAddAndGet",
				        "sourceKey": "sourceKey_eventAddAndGet",
				        "fields": {
				            "server": "localhost",
				            "summary": "Hello World",
				            "method": "eventAddAndGet"
				        }
				    }
				""";
		var event = mapper.readValue(json, Event.class);
		entityAddAndGet("/event", event.getId(), event, Event.class, i -> {
			assertEquals(event.getId(), i.getId());
			assertEquals(event.getSource(), i.getSource());
			assertEquals(event.getSourceKey(), i.getSourceKey());
			assertEquals(event.getFields(), i.getFields());
		});
	}
	
	@Test
	public void eventSourceFilter() throws IOException {
		final var id = "id_eventSourceFilter";
		final var source = "src_eventSourceFilter";
		final var sourceKey = "src_eventSourceFilter";
		final var event = new Event(id, source, sourceKey, EventType.PROBLEM, BaseStatus.WARNING);
		final var event1 = new Event(id + "1", source + "1", sourceKey + "1",EventType.PROBLEM, BaseStatus.WARNING);
		entityAdd("/event", event);
		entityAdd("/event", event1);
		
		var events = eventGetBySourceEqual(source);
		assertEquals(1, events.size());
		var retEvent= events.get(0);
		assertEquals(event.getId(), retEvent.getId());
		
		events = eventGetBySourceNotEqual(source);
		assertNotEquals(0, events.size());
		
		events.forEach(e -> {
			assertNotEquals(event.getId(), e.getId());
		});
	}
	
	@Test
	public void eventSourceKeyFilter() throws IOException {
		final var id = "id_eventSourceFilter";
		final var source = "src_eventSourceKeyFilter";
		final var sourceKey = "src_eventSourceKeyFilter";
		final var event = new Event(id, source, sourceKey, EventType.PROBLEM, BaseStatus.WARNING);
		final var event1 = new Event(id + "1", source + "1", sourceKey + "1", EventType.PROBLEM, BaseStatus.WARNING);
		entityAdd("/event", event);
		entityAdd("/event", event1);
		
		var events = eventGetBySourceKeyEqual(sourceKey);
		assertEquals(1, events.size());
		var retEvent= events.get(0);
		assertEquals(event.getId(), retEvent.getId());
		
		events = eventGetBySourceKeyNotEqual(sourceKey);
		assertNotEquals(0, events.size());
		
		events.forEach(e -> {
			assertNotEquals(event.getId(), e.getId());
		});
	}

	@Test
	public void eventDeleteBySoyrceAndSourceKeyNot() throws IOException {
		final var id = "id_eventDeleteBySoyrceAndSourceKeyNot";
		final var source = "src_eventDeleteBySoyrceAndSourceKeyNot";
		final var sourceKey = "src_eventDeleteBySoyrceAndSourceKeyNot";
		var event1 = new Event(id + "1", source, sourceKey, EventType.PROBLEM, BaseStatus.WARNING);
		var event2 = new Event(id + "2", source, sourceKey + "New", EventType.PROBLEM, BaseStatus.WARNING);
		entityDeleteBySoyrceAndSourceKeyNot("/event", event1, event2);
	}
	
	@Test
	public void itemTree() throws IOException {
		var json = """
					 {
				        "id": "id_itemTreeRoot",
				        "source": "src_itemTree",
				        "sourceKey": "src_key_itemTree",
				        "fields": {
				            "name": "itemTreeRoot",
				            "description": "root",
				            "method": "itemTree"
				        },
				        "childrenIds": ["id_itemTreeChild"]
				    }
				""";
		var root = mapper.readValue(json, Item.class);
		
		entityAddAndGet("/item", root.getId(), root, Item.class, i -> {
			assertEquals(root.getId(), i.getId());
		});
		
		json = """
					 {
				        "id": "id_itemTreeChild",
				        "source": "src_itemTree",
				        "sourceKey": "src_key_itemTree",
				        "fields": {
				            "name": "itemTreeChild",
				            "description": "root",
				            "method": "itemTree"
				        }
				    }
				""";
		var child = mapper.readValue(json, Item.class);
		
		entityAddAndGet("/item", child.getId(), child, Item.class, i -> {
			assertEquals(child.getId(), i.getId());
		});
		
		var retRoot = itemGetTree(root.getId());
		assertTrue(retRoot.getChildren().size() > 0);
		
		var opt = retRoot.getChildren().stream()
		.filter(retChild -> child.getId().equals(child.getId())).findFirst();
		assertThat(opt).isNotEmpty();
		var retChild = opt.get();
		assertEquals(child.getId(), retChild.getId());
		
	}
	
	@Test
	public void updateItemAndKeepStatus()  throws IOException, InterruptedException {
		var json = """
				 [{
			        "id": "item_updateItemAndKeepStatus",
			        "source": "src_updateItemAndKeepStatus",
			        "sourceKey": "src_key_updateItemAndKeepStatus",
			        "fields": {
			            "name": "Hello",
			            "description": "World"
			        },
			        "rules": {},
			        "filters": {
			            "by_identity": {
			                "resultStatus": "INDETERMINATE",
			                "usingResultStatus": false,
			                "equalFields": { "identity": "i_updateItemAndKeepStatus" }
			            }
			        }
			    }]
			""";
		var items  = mapper.readValue(json, Item[].class);
		entityAdd("/item", items);
		Thread.sleep(1000);
		
		json = """
				[
			    {
			    	"id": "event_updateItemAndKeepStatus_3",
			        "type": "PROBLEM",
			        "status": "WARNING",
			        "source": "source_updateItemAndKeepStatus",
			        "sourceKey": "sourceKey_updateItemAndKeepStatus",
			        "fields": {
			            "server": "localhost",
			            "summary": "Hello World",
			            "identity": "i_updateItemAndKeepStatus"
			        }
			    }]
			""";
		var events = mapper.readValue(json, Event[].class);
		entityAdd("/event", events);
		Thread.sleep(1000);
		json = """
				 [{
			        "id": "item_updateItemAndKeepStatus",
			        "source": "src_updateItemAndKeepStatus",
			        "sourceKey": "src_key_updateItemAndKeepStatus1",
			        "fields": {
			            "name": "Hello",
			            "description": "World"
			        },
			        "rules": {},
			        "filters": {
			            "by_identity": {
			                "resultStatus": "INDETERMINATE",
			                "usingResultStatus": false,
			                "equalFields": { "identity": "i_updateItemAndKeepStatus" }
			            }
			        }
			    }]
			""";
		items  = mapper.readValue(json, Item[].class);
		entityAdd("/item", items);
		Thread.sleep(1000);
		var retItem = entityGetById("/item", "item_updateItemAndKeepStatus", Item.class);
		assertEquals(BaseStatus.WARNING, retItem.getStatus());
	}

}
