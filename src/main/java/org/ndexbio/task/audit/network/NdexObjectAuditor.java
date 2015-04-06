package org.ndexbio.task.audit.network;



import java.util.Map;
import java.util.Set;

import org.ndexbio.model.object.network.NetworkElement;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;



/*
 * Represents a generic class that supports basic tracking of NdexObject collections.
 * It creates a Set of a particular subclass of NdexObjects (e.g. Edge, Node, Support , etc) 
 * and supports a method that allows individual set entries to be removed.
 * It also provides a method that will list the JdexId of entries remaining in the set.
 * 
 */
public class NdexObjectAuditor<T> {

	private Set<Long> idSet;
	private final Class<T> ndexClass;

	public NdexObjectAuditor(Class<T> aClass) {
		Preconditions.checkArgument(null != aClass,
				"An subclass of NdexObject is required");
		this.ndexClass = aClass;
		this.idSet = Sets.newConcurrentHashSet();
	}

	public void registerJdexIds(Map<Long, T> ndexMap) {
		idSet.addAll(ndexMap.keySet());
	}

	public void removeProcessedNdexObject(T obj) {
		Preconditions.checkArgument(null != obj, "An NdexObject  is required");

		if ( -1 != ((NetworkElement) obj).getId() && this.idSet.contains(((NetworkElement) obj).getId())) {
			this.idSet.remove(((NetworkElement) obj).getId());
			
		}
	}

	public String displayUnprocessedNdexObjects() {
		if (this.idSet.isEmpty()) {
			return "\nAll " + this.ndexClass.getSimpleName()
					+ " entries were processed";
		}
		StringBuffer sb = new StringBuffer("\nUnprocessed "
				+ this.ndexClass.getSimpleName() + " objects\n");
		for (Long id : Lists.newArrayList(this.idSet)) {
			sb.append(id + " ");
		}
		return sb.toString();
	}
}