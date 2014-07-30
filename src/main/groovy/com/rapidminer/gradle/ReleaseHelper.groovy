package com.rapidminer.gradle

import org.gradle.api.GradleException
import org.gradle.api.Project

/**
 *
 * Utility class for release constants and helper methods.
 *
 * @author Nils Woehler
 *
 */
class ReleaseHelper {

	protected static final String GRADLE_PROPERTIES = "gradle.properties"
	protected static final String SNAPSHOT = "-SNAPSHOT"
	protected static final String RC = "-RC"

	private static final String LINE_SEP = System.getProperty('line.separator')
	private static final String PROMPT = "${LINE_SEP}??>"

	/**
	 * If set the release version will be inferred from this property rather than from gradle.properties.
	 */
	protected static final String PROPERTY_RELEASE_VERSION = 'release.version'

	/**
	 * Allows to define whether 'releasePrepare' should ask for user feedback. If set to <code>false</code>
	 * the version defined in 'gradle.properties' or defined via 'release.version' project
	 * property will be used as release version and no sanity check will be done.
	 */
	protected static final String PROPERTY_RELEASE_INTERACTIVE = 'release.interactive'

	/**
	 * Allows to define whether 'release' should depend on 'releasePrepare'. Useful for CI server environments
	 * where the CI server should do as less as possible.
	 */
	protected static final String PROPERTY_RELEASE_PREPARE = 'release.prepare'

	/**
	 * Allows to define whether 'release' should be finalized by 'releaseFinalize'. Useful for CI server environments
	 * where the CI server should do as less as possible.
	 */
	protected static final String PROPERTY_RELEASE_FINALIZE = 'release.finalize'

	/**
	 * @return the root project's 'gradle.properties' file path
	 */
	protected static final File getGradlePropertiesFile(project) {
		return project.rootProject.file(GRADLE_PROPERTIES)
	}

	/**
	 * @return the properties loaded from the root project's 'gradle.properties' file.
	 */
	protected static final Properties getGradleProperties(project) {
		def gradlePropFile = getGradlePropertiesFile(project)
		if(!gradlePropFile.exists()){
			throw new RuntimeException("Could not find 'gradle.properties' in root project!")
		}
		def gradleProperties = new Properties()
		gradlePropFile.withReader { reader ->
			gradleProperties.load(reader)
		}
		return gradleProperties
	}

	/**
	 * Writes a message to the console. Uses System.out.println if no console is available.
	 * 
	 * @param message the message to write
	 */
	public static final void println(String message) {
		if(System.console()) {
			System.console().out.write(message + LINE_SEP)
		} else {
			println message
		}
	}

	/**
	 * Reads user input from the console.
	 *
	 * @param message Message to display
	 * @param defaultValue (optional) default value to display
	 * @return User input entered or default value if user enters no data
	 */
	public static final String readLine(String message, String defaultValue = null) {
		String _message = "$PROMPT $message" + (defaultValue ? " [$defaultValue] " : "")
		if (System.console()) {
			return System.console().readLine(_message) ?: defaultValue
		}
		println "$_message (WAITING FOR INPUT BELOW)"
		return System.in.newReader().readLine() ?: defaultValue
	}
}