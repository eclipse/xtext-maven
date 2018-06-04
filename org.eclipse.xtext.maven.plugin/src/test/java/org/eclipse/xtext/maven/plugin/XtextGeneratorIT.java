package org.eclipse.xtext.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.shared.utils.cli.CommandLineUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.base.Joiner;

import junit.framework.Assert;

public class XtextGeneratorIT {

	private static String ROOT = "/it/generate";

	@BeforeClass
	static public void setUpOnce() throws IOException, VerificationException {
		File mvnExecutable = new File(new Verifier(ROOT).getExecutable());
		if (!mvnExecutable.exists()) {
			String mavenHome = findMaven();
			if (mavenHome != null) {
				System.setProperty("maven.home", mavenHome);
			} else {
				Assert.fail("Maven home not found. Tried to call '" + mvnExecutable
						+ "'.\nConsider to set the envVar 'M2_HOME' or System property 'maven.home'.\n"
						+ "Current settings are: maven.home=" + System.getProperty("maven.home") + " M2_HOME="
						+ CommandLineUtils.getSystemEnvVars().getProperty("M2_HOME"));
			}
		}
		new XtextGeneratorIT().verifyErrorFreeLog(ROOT, false, "clean", "install");
	}

	private static String findMaven() {
		// TODO add more mavens here
		String[] mavens = new String[] { System.getProperty("maven.home"), "/opt/local/share/java/maven3/" };
		for (String maven : mavens) {
			if (new File(maven + "/bin/mvn").exists()) {
				return maven;
			}
		}
		return null;
	}

	private final static boolean debug = Boolean.getBoolean("xtext.it.tests.debug");

	@Test
	public void simpleLang() throws Exception {
		Verifier verifier = verifyErrorFreeLog(ROOT + "/simple-lang");
		verifier.assertFileMatches(verifier.getBasedir() + "/src-gen/RefModel.nojdt.txt", "People to greet\\: Test");
	}

	@Test
	public void mavenConfiguration() throws Exception {
		Verifier verifier = verifyErrorFreeLog(ROOT + "/maven-config");
		verifier.assertFileMatches(verifier.getBasedir() + "/model2-output/Model.nojdt.txt", "People to greet\\: maven2");
		verifier.assertFilePresent(verifier.getBasedir() + "/model-output/IntegrationTestXbase.java");
	}

	@Test
	public void purexbase() throws Exception {
		Verifier verifier = verifyErrorFreeLog(ROOT + "/purexbase");
		verifier.assertFilePresent(verifier.getBasedir() + "/src-gen/IntegrationTestXbase.java");
		verifier.assertFilePresent(verifier.getBasedir() + "/target/xtext-temp/classes/IntegrationTestXbase.class");
	}

	@Test
	public void clustering() throws Exception {
		Verifier verifier = verifyErrorFreeLog(ROOT + "/clustering");
		verifier.assertFilePresent(verifier.getBasedir() + "/src-gen/IntegrationTestXbase.java");
		verifier.assertFilePresent(verifier.getBasedir() + "/src-gen/IntegrationTestXbase2.java");
		verifier.assertFilePresent(verifier.getBasedir() + "/target/xtext-temp/classes/IntegrationTestXbase.class");
		verifier.assertFilePresent(verifier.getBasedir() + "/target/xtext-temp/classes/IntegrationTestXbase2.class");
	}

	@Test
	public void outputPerSource() throws Exception {
		Verifier verifier = verifyErrorFreeLog(ROOT + "/output-per-source");
		verifier.assertFilePresent(verifier.getBasedir() + "/src-gen/IntegrationTestXbase.java");
		verifier.assertFilePresent(verifier.getBasedir() + "/target/xtext-temp/classes/IntegrationTestXbase.class");
		verifier.assertFilePresent(verifier.getBasedir() + "/other-gen/OtherIntegrationTestXbase.java");
		verifier.assertFilePresent(
				verifier.getBasedir() + "/target/xtext-temp/classes/OtherIntegrationTestXbase.class");
	}

	@Test
	public void javaLangBiRef() throws Exception {
		Verifier verifier = verifyErrorFreeLog(ROOT + "/java-lang-bi-ref");
		verifier.assertFilePresent(verifier.getBasedir() + "/src-gen/XbaseReferToJava.java");
		verifier.assertFilePresent(verifier.getBasedir() + "/target/xtext-temp/classes/XbaseReferToJava.class");
		verifier.assertFilePresent(verifier.getBasedir() + "/target/xtext-temp/classes/JavaClazz.class");
	}

	@Test
	public void aggregation() throws Exception {
		Verifier verifier = verifyErrorFreeLog(ROOT + "/aggregate");
		verifier.assertFilePresent(verifier.getBasedir() + "/purexbase/src-gen/IntegrationTestXbase.java");
		verifier.assertFilePresent(
				verifier.getBasedir() + "/purexbase/target/xtext-temp/classes/IntegrationTestXbase.class");
	}

	@Test
	public void xcore() throws Exception {
		Verifier verifier = verifyErrorFreeLog(ROOT + "/xcore-lang",true, "clean", "verify");
		verifier.assertFilePresent(verifier.getBasedir() + "/src-gen/org/eclipse/xcoretest/MyClass2.java");
		verifier.assertFilePresent(
				verifier.getBasedir() + "/target/xtext-temp/classes/org/eclipse/xcoretest/MyClass2.class");
		verifier.assertFileMatches(verifier.getBasedir() + "/src-gen/org/eclipse/xcoretest/MyEnum.java",
				"(?s).*MY_FIRST_LITERAL\\(-7.*MY_SECOND_LITERAL\\(137.*");
	}
	
	@Test
	public void xcoreMapping() throws Exception {
		verifyErrorFreeLog(ROOT + "/xcore-mapping", true, "clean", "verify");
	}
	
	@Test
	public void xcoreAutoMapping() throws Exception {
		verifyErrorFreeLog(ROOT + "/xcore-auto-mapping", true, "clean", "verify");
	}

	@Test
	public void bug463946() throws Exception {
		Verifier verifier = verifyErrorFreeLog(ROOT + "/bug463946");
		verifier.assertFilePresent(verifier.getBasedir() + "/src-gen/xcore/bug463946/pack/MyModel.java");
	}

	private Verifier verifyErrorFreeLog(String pathToTestProject) throws IOException, VerificationException {
		return verifyErrorFreeLog(pathToTestProject, false, "clean", "verify");
	}

	private Verifier verifyErrorFreeLog(String pathToTestProject, boolean updateSnapshots, String... goals)
			throws IOException, VerificationException {
		if(goals == null || goals.length < 1) {
			throw new IllegalArgumentException("You need to pass at least one goal to verify log");
		}
		Verifier verifier = newVerifier(pathToTestProject);
		String localRepo = new File(System.getProperty("testProjectDir")+"/../../.m2/repository/").getAbsoluteFile().getAbsolutePath();
		verifier.setLocalRepo(localRepo);
		
		if(updateSnapshots) {
			verifier.addCliOption("-U");
		}
		for (String goal : goals) {
			verifier.executeGoal(goal);
		}
		verifier.verifyErrorFreeLog();
		if (debug) {
			List<String> lines = verifier.loadFile(verifier.getBasedir(), verifier.getLogFileName(), false);
			System.out.println(Joiner.on('\n').join(lines));
		}
		verifier.resetStreams();
		return verifier;
	}

	private Verifier newVerifier(String pathToTestProject) throws IOException, VerificationException {
		File testDir = ResourceExtractor.simpleExtractResources(getClass(), pathToTestProject);
		Verifier verifier = new Verifier(testDir.getAbsolutePath(), true);
//		verifier.addCliOption("-U");
//		verifier.setForkJvm(!debug);
		String mvnOpts = CommandLineUtils.getSystemEnvVars().getProperty("MAVEN_OPTS");
		String modMvnOpts = (mvnOpts != null ? mvnOpts + " " : "") + "-Xmx1g -XX:MaxPermSize=256m";
		verifier.setEnvironmentVariable("MAVEN_OPTS", modMvnOpts);
		if (debug) {
			verifier.setMavenDebug(debug);
			System.out.println("Modified Maven Opts: " + modMvnOpts);
		}
		return verifier;
	}

	@AfterClass
	static public void tearDownOnce() throws IOException, VerificationException {
		if (!debug)
			new XtextGeneratorIT().verifyErrorFreeLog(ROOT, false, "clean");
	}

}
