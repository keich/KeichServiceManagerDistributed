package ru.keich.mon.servicemanager;

import java.util.Map;
import java.util.Objects;

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

public class StringKeyValue implements Comparable<StringKeyValue> {

	private String key;

	private String value;

	public StringKeyValue(String key, String value) {
		super();
		this.key = key;
		this.value = value;
	}

	public StringKeyValue(Map.Entry<String, String> entry) {
		super();
		this.key = entry.getKey();
		this.value = entry.getValue();
	}

	@Override
	public String toString() {
		return "StringKeyValue [key=" + key + ", value=" + value + "]";
	}

	@Override
	public int compareTo(StringKeyValue o) {
		var t = key.compareTo(o.key);
		if (t == 0) {
			return value.compareTo(o.value);
		}
		return t;
	}

	@Override
	public int hashCode() {
		return Objects.hash(key, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StringKeyValue other = (StringKeyValue) obj;
		return Objects.equals(key, other.key) && Objects.equals(value, other.value);
	}

}
