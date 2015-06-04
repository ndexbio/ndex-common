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
