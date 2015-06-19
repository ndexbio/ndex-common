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
package org.ndexbio.task;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.List;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;


/*
 * An integration test to invoke the Open BEL belc shell script
 * on UNIX/Linux & OS-X NDEx installations
 *  The KAM compiler requires that two (2) environment variables be defined:
 * 1. BELFRAMEWORK_HOME
 * 2. BELCOMPILER_DIR
 * String kamStoreName = "small_corpus";
 	String description = "Small Corpus";
 	String[] belcArgs = new String[] {"-f",exportedFilename, "-k",kamStoreName, "-d",description};
 */
public class TestProcessBuilder {
	
	private static final String BELC_SH = "/opt/ndex/OpenBEL_Framework-3.0.0/belc.sh";
	private static final String DEFAULT_XBEL_FILE = "/tmp/ndex/corpus/small_corpus.xbel";
	
	private final String belFileName;
	
	public TestProcessBuilder(String aFileName){
		this.belFileName = aFileName;
	}
	
	private void performTests(){
		this.testRuntime();
		this.testProcessBuilder();
	}
	
	private void testRuntime() {
		System.out.println("Starting runtime exec");
		StringBuilder sb = new StringBuilder(BELC_SH);
		sb.append(" -f " );
		sb.append(DEFAULT_XBEL_FILE);
		sb.append(" -k test_kam ");
		sb.append(" -d TEST ");
		System.out.println("Running " +sb.toString());
		
		try {
			Process p = Runtime.getRuntime().exec(sb.toString());
			p.waitFor();
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void testProcessBuilder() {
		ProcessBuilder pb = this.createBelcProcessBuilder();
		try {
			System.out.println("Starting ProcessBuilder");
			for(String s : pb.command()){
				System.out.println(s);
			}
			pb.start();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		String fn =  DEFAULT_XBEL_FILE;
		TestProcessBuilder test = new TestProcessBuilder(fn);
		test.performTests();

	}
	
	private ProcessBuilder createBelcProcessBuilder() {
		List<String> command = Lists.newArrayList();
		command.add(BELC_SH);
		command.add("-f " +this.belFileName);
		command.add("-k test_kam");
		command.add("-d Test KAM");
		ProcessBuilder pb = new ProcessBuilder(command);
		// set envrionment variables
		pb.environment().put("BELFRAMEWORK_HOME", "/opt/ndex/OpenBEL_Framework-3.0.0");
		pb.environment().put("BELCOMPILER_DIR", "/opt/ndex/OpenBEL_Framework-3.0.0");
		pb.directory(new File("/tmp/belc"));
		File log = new File("belc.log");
		pb.redirectErrorStream(true);
		pb.redirectOutput(Redirect.appendTo(log));
		
		
		return pb;
	}

}
