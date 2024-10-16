package ru.keich.mon.servicemanager;

import java.time.Instant;
import java.util.Objects;

import com.hazelcast.nio.serialization.compact.CompactReader;
import com.hazelcast.nio.serialization.compact.CompactSerializer;
import com.hazelcast.nio.serialization.compact.CompactWriter;

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

public class InstantSerializer implements CompactSerializer<Instant> {

	static String FIELD_NAME = "epochMilli";

	@Override
	public Instant read(CompactReader reader) {
		long epoch = reader.readNullableInt64(FIELD_NAME);
		if (Objects.nonNull(epoch)) {
			Instant.ofEpochMilli(epoch);
		}
		return Instant.ofEpochMilli(epoch);
	}

	@Override
	public void write(CompactWriter writer, Instant instant) {
		writer.writeNullableInt64(FIELD_NAME, instant.toEpochMilli());
	}

	@Override
	public String getTypeName() {
		return "Instant";
	}

	@Override
	public Class<Instant> getCompactClass() {
		return Instant.class;
	}

}
