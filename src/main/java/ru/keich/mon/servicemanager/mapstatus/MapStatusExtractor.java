package ru.keich.mon.servicemanager.mapstatus;

import java.io.IOException;

import com.hazelcast.internal.serialization.impl.GenericRecordQueryReader;
import com.hazelcast.query.extractor.ValueCollector;
import com.hazelcast.query.extractor.ValueExtractor;

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

public class MapStatusExtractor implements ValueExtractor<GenericRecordQueryReader, String> {

	static final String KEYS = "keys";

	@Override
	public void extract(GenericRecordQueryReader record, String argument, ValueCollector<Object> collector) {
		try {
			var eventsStatus = (MapStatus) record.read("eventsStatus");
			eventsStatus.keySet().forEach(k -> {
				collector.addObject(k);
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
