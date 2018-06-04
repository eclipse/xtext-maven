/**
 * 
 */
package org.eclipse.xtext.maven;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Sets.newLinkedHashSet;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.xtext.builder.standalone.LanguageAccess;
import org.eclipse.xtext.builder.standalone.LanguageAccessFactory;
import org.eclipse.xtext.builder.standalone.StandaloneBuilder;
import org.eclipse.xtext.builder.standalone.compiler.CompilerConfiguration;
import org.eclipse.xtext.builder.standalone.compiler.IJavaCompiler;
import org.eclipse.xtext.util.Strings;
import org.eclipse.xtext.xbase.lib.IterableExtensions;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * @author Dennis Huebner - Initial contribution and API
 * 
 * @goal generate
 * @phase generate-sources
 * @requiresDependencyResolution compile
 */
public class XtextGenerator extends AbstractMojo {

	/**
	 * Location of the generated source files.
	 * 
	 * @parameter expression="${project.build.directory}/xtext-temp"
	 */
	private String tmpClassDirectory;

	/**
	 * File encoding argument for the generator.
	 * 
	 * @parameter expression="${xtext.encoding}"
	 *            default-value="${project.build.sourceEncoding}"
	 */
	protected String encoding;

	/**
	 * The project itself. This parameter is set by maven.
	 * 
	 * @parameter expression="${project}"
	 * @readonly
	 * @required
	 */
	protected MavenProject project;

	/**
	 * Project classpath.
	 * 
	 * @parameter expression="${project.compileClasspathElements}"
	 * @readonly
	 * @required
	 */
	private List<String> classpathElements;

	/**
	 * Project source roots. List of folders, where the source models are located.<br>
	 * The default value is a reference to the project's ${project.compileSourceRoots}.<br>
	 * When adding a new entry the default value will be overwritten not extended. 
	 * @parameter
	 */
	private List<String> sourceRoots;
	
	/**
	 * Java source roots. List of folders, where the java source files are located.<br>
	 * The default value is a reference to the project's ${project.compileSourceRoots}.<br>
	 * When adding a new entry the default value will be overwritten not extended.<br>
	 * Used when your language needs java.
	 * 
	 * @parameter
	 */
	private List<String> javaSourceRoots;

	/**
	 * @parameter
	 * @required
	 */
	private List<Language> languages;

	/**
	 * @parameter expression="${xtext.generator.skip}" default-value="false"
	 */
	private Boolean skip;

	/**
	 * @parameter default-value="true"
	 */
	private Boolean failOnValidationError;

	/**
	 * @parameter expression="${maven.compiler.source}" default-value="1.6"
	 */
	private String compilerSourceLevel;

	/**
	 * @parameter expression="${maven.compiler.target}" default-value="1.6"
	 */
	private String compilerTargetLevel;

	/**
	 * RegEx expression to filter class path during model files look up
	 * 
	 * @parameter
	 */
	private String classPathLookupFilter;

	/**
	 * Clustering configuration to avoid OOME
	 *
	 * @parameter
	 */
	private ClusteringConfig clusteringConfig;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip) {
			getLog().info("skipped.");
		} else {
			new MavenLog4JConfigurator().configureLog4j(getLog());
			configureDefaults();
			addToPlatformResourceMap(project);
			internalExecute();
		}
	}

	protected void internalExecute() throws MojoExecutionException, MojoFailureException {
		Map<String, LanguageAccess> languages = new LanguageAccessFactory().createLanguageAccess(getLanguages(), this
				.getClass().getClassLoader());
		Injector injector = Guice.createInjector(new MavenStandaloneBuilderModule());
		StandaloneBuilder builder = injector.getInstance(StandaloneBuilder.class);
		builder.setBaseDir(project.getBasedir().getAbsolutePath());
		builder.setLanguages(languages);
		builder.setEncoding(encoding);
		builder.setClassPathEntries(getClasspathElements());
		builder.setClassPathLookUpFilter(classPathLookupFilter);
		builder.setSourceDirs(sourceRoots);
		builder.setJavaSourceDirs(javaSourceRoots);
		builder.setFailOnValidationError(failOnValidationError);
		builder.setTempDir(createTempDir().getAbsolutePath());
		builder.setDebugLog(getLog().isDebugEnabled());
		if(clusteringConfig != null)
			builder.setClusteringConfig(clusteringConfig.convertToStandaloneConfig());
		configureCompiler(builder.getCompiler());
		logState();
		boolean errorDetected = !builder.launch();
		if (errorDetected && failOnValidationError) {
			throw new MojoExecutionException("Execution failed due to a severe validation error.");
		}
	}

	private void configureCompiler(IJavaCompiler compiler) {
		CompilerConfiguration conf = compiler.getConfiguration();
		conf.setSourceLevel(compilerSourceLevel);
		conf.setTargetLevel(compilerTargetLevel);
		conf.setVerbose(getLog().isDebugEnabled());
	}

	private void logState() {
		getLog().info("Encoding: " + (encoding == null ? "not set. Encoding provider will be used." : encoding));
		getLog().info("Compiler source level: " + compilerSourceLevel);
		getLog().info("Compiler target level: " + compilerTargetLevel);
		if (getLog().isDebugEnabled()) {
			getLog().debug("Source dirs: " + IterableExtensions.join(sourceRoots, ", "));
			getLog().debug("Java source dirs: " + IterableExtensions.join(javaSourceRoots, ", "));
			getLog().debug("Classpath entries: " + IterableExtensions.join(getClasspathElements(), ", "));
		}
	}

	private File createTempDir() {
		File tmpDir = new File(tmpClassDirectory);
		if (!tmpDir.mkdirs() && !tmpDir.exists()) {
			throw new IllegalArgumentException("Couldn't create directory '" + tmpClassDirectory + "'.");
		}
		return tmpDir;
	}

	private Predicate<String> emptyStringFilter() {
		return new Predicate<String>() {
			public boolean apply(String input) {
				return !Strings.isEmpty(input.trim());
			}
		};
	}

	public Set<String> getClasspathElements() {
		Set<String> classpathElements = newLinkedHashSet();
		classpathElements.addAll(this.classpathElements);
		classpathElements.remove(project.getBuild().getOutputDirectory());
		classpathElements.remove(project.getBuild().getTestOutputDirectory());
		Set<String> nonEmptyElements = newLinkedHashSet(filter(classpathElements, emptyStringFilter()));
		return nonEmptyElements;
	}

	public List<Language> getLanguages() {
		return languages;
	}

	public void setLanguages(List<Language> languages) {
		this.languages = languages;
	}

	private void configureDefaults() {
		if (sourceRoots == null) {
			sourceRoots = Lists.newArrayList(project.getCompileSourceRoots());
		}
		if (javaSourceRoots == null) {
			javaSourceRoots = Lists.newArrayList(project.getCompileSourceRoots());
		}
	}

	/**
	 * Adds the given project and its child modules ({@link MavenProject#getModules()})
	 * to {@link EcorePlugin#getPlatformResourceMap()}. Furthermore, this traverses
	 * {@link MavenProject#getParent()} recursively, if set.
	 *
	 * @param project the project
	 */
	private static void addToPlatformResourceMap(final MavenProject project) {
		addToPlatformResourceMap(project.getBasedir());
		project.getModules().stream()
				.map(module -> new File(project.getBasedir(), module))
				.forEach(XtextGenerator::addToPlatformResourceMap);

		if (project.getParent() != null)
			addToPlatformResourceMap(project.getParent());
	}

	/**
	 * Adds the given file to EcorePlugin's platform resource map.
	 * The file's name will be used as the map entry's key.
	 *
	 * @param file a file to register
	 * @return the registered URI pointing to the given file
	 * @see EcorePlugin#getPlatformResourceMap()
	 */
	private static URI addToPlatformResourceMap(final File file) {
		final URI uri = URI.createURI(file.toURI().toString());
		return EcorePlugin.getPlatformResourceMap().put(file.getName(), uri);
	}
}
