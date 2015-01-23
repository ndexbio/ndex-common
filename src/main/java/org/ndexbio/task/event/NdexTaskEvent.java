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
