package ru.keich.mon.servicemanager.item;

import java.util.concurrent.RecursiveAction;
import java.util.function.Consumer;

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

public class SimpleTask<K> extends RecursiveAction {
	
	private static final long serialVersionUID = 1L;
	private final K eventId;
	private final Consumer<K> consumer;
	
	public SimpleTask(K eventId, Consumer<K> consumer) {
		super();
		this.eventId = eventId;
		this.consumer = consumer;
	}

	@Override
	protected void compute() {
		consumer.accept(eventId);
	}

}
