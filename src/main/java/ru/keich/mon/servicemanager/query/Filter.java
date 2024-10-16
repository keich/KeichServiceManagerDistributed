package ru.keich.mon.servicemanager.query;

import lombok.Getter;

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

@Getter
public class Filter {

	private final String name;
	private final Operator operator;
	private final String value;

	public Filter(String name, Operator operator, String value) {
		super();
		this.name = name;
		this.operator = operator;
		this.value = value;
	}

	public Filter(String name, String value) {
		var arr = value.split(":", 2);
		if (arr.length == 2) {
			this.name = name;
			this.operator = Operator.fromString(arr[0]);
			this.value = arr[1];
		} else {
			this.name = name;
			this.operator = Operator.ERROR;
			this.value = value;
		}
	}

}
