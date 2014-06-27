package org.ndexbio.common.util;

import java.util.UUID;
import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;

public enum NdexUUIDFactory {
	INSTANCE;
	private static final EthernetAddress addr = EthernetAddress.fromInterface();
	private static final TimeBasedGenerator uuidGenerator = Generators.timeBasedGenerator(addr);
	public UUID getNDExUUID() {
		return  uuidGenerator.generate();
		
	}
}
