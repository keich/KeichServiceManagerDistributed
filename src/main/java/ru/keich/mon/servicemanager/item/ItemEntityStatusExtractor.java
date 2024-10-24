package ru.keich.mon.servicemanager.item;

import java.io.IOException;

import com.hazelcast.internal.serialization.impl.GenericRecordQueryReader;
import com.hazelcast.query.extractor.ValueCollector;
import com.hazelcast.query.extractor.ValueExtractor;

public class ItemEntityStatusExtractor implements ValueExtractor<GenericRecordQueryReader, String> {

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
