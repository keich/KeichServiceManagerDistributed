package ru.keich.mon.servicemanager.mapstatus;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Optional;

import java.util.Set;

import ru.keich.mon.servicemanager.BaseStatus;

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

public class MapStatus {

	private BaseStatus maxStatus = BaseStatus.CLEAR;
	
	private HashMap<String, BaseStatus> data = new HashMap<String, BaseStatus>();

	public Optional<BaseStatus> put(String id, BaseStatus status) {
		var opt = Optional.ofNullable(data.put(id, status));
		maxStatus = calculateMaxStatus();
		return opt;
	}

	public BaseStatus remove(String id) {
		var oldStatus = data.remove(id);
		maxStatus = calculateMaxStatus();
		return oldStatus;
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
	
	public BaseStatus getMaxStatus() {
		return maxStatus;
	}
	
	private BaseStatus calculateMaxStatus() {
		BaseStatus max = BaseStatus.CLEAR;
		for(var s: data.entrySet()) {
			max = max.max(s.getValue());
		}
		return max;
	}

}
