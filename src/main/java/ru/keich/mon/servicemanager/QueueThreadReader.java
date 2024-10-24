package ru.keich.mon.servicemanager;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.hazelcast.collection.IQueue;
import com.hazelcast.core.HazelcastInstance;

import lombok.extern.java.Log;

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

@Log
public class QueueThreadReader<K> {

	static final Integer POOL_SECONDS = 30;
	private final Consumer<K> consumer;
	private final IQueue<K> queue;
	private Integer counter = 0;

	public QueueThreadReader(HazelcastInstance hazelcastInstance, String name, int  number, Consumer<K> consumer) {
		super();
		this.consumer = consumer;
		this.queue = hazelcastInstance.getQueue(name);

		for (int i = 0; i < number; i++) {
			runThread(name.toLowerCase());
		}
	}

	private Thread runThread(String name) {
		var thread = new Thread(() -> {
			try {
				while (true) {
					Optional.ofNullable(queue.poll(POOL_SECONDS, TimeUnit.SECONDS))
							.ifPresent(t -> consumer.accept(t));
				}
				
			} catch (InterruptedException v) {
				log.warning(v.toString());
			}
		}, this.getClass().getName() + "-" + name + "-" + counter);
		counter = counter + 1;
		thread.start();
		return thread;
	}

	public void add(K value) {
		queue.add(value);
	}

}
