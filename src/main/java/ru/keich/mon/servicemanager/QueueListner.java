package ru.keich.mon.servicemanager;

import java.util.function.Consumer;

import com.hazelcast.collection.ItemEvent;
import com.hazelcast.collection.ItemListener;

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

public class QueueListner<K> implements ItemListener<K> {

	private final Consumer<ItemEvent<K>> consumer;

	public QueueListner(Consumer<ItemEvent<K>> consumer) {
		super();
		this.consumer = consumer;
	}

	@Override
	public void itemAdded(ItemEvent<K> item) {
		consumer.accept(item);
	}

	@Override
	public void itemRemoved(ItemEvent<K> item) {

	}

}
