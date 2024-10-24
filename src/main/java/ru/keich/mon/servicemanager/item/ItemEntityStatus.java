package ru.keich.mon.servicemanager.item;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import com.hazelcast.internal.serialization.impl.GenericRecordQueryReader;
import com.hazelcast.query.extractor.ValueCollector;
import com.hazelcast.query.extractor.ValueExtractor;

import java.util.Set;

import ru.keich.mon.servicemanager.BaseStatus;

public class ItemEntityStatus {

	private HashMap<String, BaseStatus> data = new HashMap<String, BaseStatus>();

	public void put(String id, BaseStatus status) {
		data.put(id, status);
	}

	public BaseStatus remove(String id) {
		return data.remove(id);
	}

	public BaseStatus get(String id) {
		return data.get(id);
	}

	public Set<String> keySet() {
		return data.keySet();
	}

	public Set<Entry<String, BaseStatus>> entrySet() {
		return data.entrySet();
	}
	
	//TODO pre calculate
	public BaseStatus getMaxStatus() {
		BaseStatus max = BaseStatus.CLEAR;
		for(var s: data.entrySet()) {
			max = max.max(s.getValue());
		}
		return max;
	}

	public static class ItemEntityStatusExtractor implements ValueExtractor<GenericRecordQueryReader, String> {

		static final String KEYS = "keys";

		@Override
		public void extract(GenericRecordQueryReader record, String argument, ValueCollector<Object> collector) {
			try {
				var eventsStatus = (ItemEntityStatus) record.read("eventsStatus");
				eventsStatus.keySet().forEach(k -> {
					collector.addObject(k);
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
