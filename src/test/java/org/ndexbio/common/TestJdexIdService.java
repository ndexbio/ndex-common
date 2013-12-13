package org.ndexbio.common;

import org.ndexbio.service.JdexIdService;

public class TestJdexIdService {

	public static void main(String[] args) {
		for ( int i = 0; i < 20; i++){
			System.out.println("JDex ID: " +JdexIdService.INSTANCE.getNextJdexId());
		}

	}

}
