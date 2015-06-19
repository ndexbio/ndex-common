/**
 * Copyright (c) 2013, 2015, The Regents of the University of California, The Cytoscape Consortium
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package org.ndexbio.task.event;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractQueue;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.eventbus.Subscribe;
import com.google.common.eventbus.AllowConcurrentEvents; 


public class NdexTaskEventHandler {
	private final FileWriter fw;
	private final String newLine = System.getProperty("line.separator");
	private final AbstractQueue<String> eventQueue = Queues
			.newConcurrentLinkedQueue();
	private Joiner joiner = Joiner.on(',').useForNull("");
	private long eventCounter = 0;
	private Class noparams[] ={}; // supports reflective invocation of event getters
	
	
	
	public NdexTaskEventHandler(String filename) throws IOException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(filename),
				"A file name is required");
		this.fw = new FileWriter(filename);
		this.registerEventSubscription();
	}

	/*
	 * write out data columns
	 */
	private void initializeCsvFile(NdexTaskEvent event) {
		try {
			this.fw.write(joiner.join(event.getEventAttributes()) + newLine);
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}

	}

	private void registerEventSubscription() {
		NdexEventBus.INSTANCE.getEventBus().register(this);
		System.out.println("NdexEventHandler registered for events");
	}

	public void shutdown() {

		try {
			while (!this.eventQueue.isEmpty()) {
				fw.append(this.eventQueue.poll());
			}
			this.fw.flush();
			this.fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Subscribe
	@AllowConcurrentEvents
	public void handleNdexTaskEvent(NdexTaskEvent event) {
		System.out.println("*****  NdexTaskEvent received");
		// print column headers before first data row
		if (this.eventCounter++ < 1) {
			this.initializeCsvFile(event);
		}
		// invoke the getter methods on the event
		try {
			this.fw.append(joiner.join(this.findFieldValues(event)) + newLine);
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}

	}

	/*
	 * method to resolve the attribute values within the event
	 */
	private Iterable<String> findFieldValues(final NdexTaskEvent event) {
		List<String> valueList = Lists.newArrayList();
		Class noparams[] ={};
		for(String field : event.getEventAttributes()){
			String methodName = "get" + field;
			try {
				Method method = event.getClass().getDeclaredMethod(methodName, noparams);
				try {
					Object obj = method.invoke(event, new Object[]{});
					String value = obj != null ? obj.toString():"";
					valueList.add(value);
				} catch (IllegalAccessException | IllegalArgumentException
						| InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			} catch (NoSuchMethodException | SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 	
		}
		
		return valueList;
	}
}
