package ru.keich.mon.servicemanager.item;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import com.hazelcast.internal.serialization.impl.GenericRecordQueryReader;
import com.hazelcast.query.extractor.ValueCollector;
import com.hazelcast.query.extractor.ValueExtractor;

import java.util.Set;

import ru.keich.mon.servicemanager.BaseStatus;

public class ItemEventsStatus {

	private HashMap<String, BaseStatus> events = new HashMap<String, BaseStatus>();

	public void put(String id, BaseStatus status) {
		events.put(id, status);
	}

	public BaseStatus remove(String id) {
		return events.remove(id);
	}

	public BaseStatus get(String id) {
		return events.get(id);
	}

	public Set<String> keySet() {
		return events.keySet();
	}

	public Set<Entry<String, BaseStatus>> entrySet() {
		return events.entrySet();
	}

	public static class ItemEventsStatusExtractor implements ValueExtractor<GenericRecordQueryReader, String> {

		static final String KEYS = "keys";

		@Override
		public void extract(GenericRecordQueryReader record, String argument, ValueCollector<Object> collector) {
			try {
				var eventsStatus = (ItemEventsStatus) record.read("eventsStatus");
				eventsStatus.keySet().forEach(k -> {
					collector.addObject(k);
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
