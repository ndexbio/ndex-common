/**
 *   Copyright (c) 2013, 2015
 *  	The Regents of the University of California
 *  	The Cytoscape Consortium
 *
 *   Permission to use, copy, modify, and distribute this software for any
 *   purpose with or without fee is hereby granted, provided that the above
 *   copyright notice and this permission notice appear in all copies.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 *   WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 *   MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 *   ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 *   WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 *   ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 *   OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package org.ndexbio.task.event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

public abstract class NdexTaskEvent {
	/*
	  * find getter methods for this event
	  */
	 Predicate<Method> getterMethodsPredicate = new Predicate<Method>() {
		@Override
		public boolean apply(Method method) {
			return method.getName().startsWith("get");
		}		 
	 };
	 
	 public Iterable<Method> findEventGetters(){
		List<Method> methods = Arrays.asList( this.getClass().getMethods());
		return Iterables.filter(methods,getterMethodsPredicate);
	 }
	 Function<Method,String> fieldNameFunction = new Function<Method,String>() {

		@Override
		public String apply(Method method) {
			// TODO Auto-generated method stub
			return method.getName().replace("get", "").toLowerCase();
		}
		 
	 };
	 
	 public Iterable<String> findFieldNames(){
		 return Iterables.transform(this.findEventGetters(), fieldNameFunction);
	 }
	 
	 
	 public abstract  List<String> getEventAttributes();

}
