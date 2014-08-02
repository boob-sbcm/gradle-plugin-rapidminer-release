/*
 * Copyright 2013-2014 RapidMiner GmbH.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

	protected static final String GRADLE_PROPERTIES = 'gradle.properties'
	protected static final String SNAPSHOT = '-SNAPSHOT'
	protected static final String RC = '-RC'

	private static final String LINE_SEP = System.getProperty('line.separator')
	private static final String PROMPT = "${LINE_SEP}??>"

	/**
	 * If set the release version will be inferred from this property rather than from gradle.properties.
	 */
	protected static final String PROPERTY_RELEASE_VERSION = 'releaseVersion'

	/**
	 * Allows to define whether 'releasePrepare' should ask for user feedback. If set to <code>false</code>
	 * the version defined in 'gradle.properties' or defined via 'release.version' project
	 * property will be used as release version and no sanity check will be done.
	 */
	protected static final String PROPERTY_RELEASE_INTERACTIVE = 'interactiveRelease'

	/**
	 * Allows to define whether 'release' should depend on 'releasePrepare'. Useful for CI server environments
	 * where the CI server should do as less as possible.
	 */
	protected static final String PROPERTY_RELEASE_PREPARE = 'prepareRelease'

	/**
	 * Allows to define whether 'release' should be finalized by 'releaseFinalize'. Useful for CI server environments
	 * where the CI server should do as less as possible.
	 */
	protected static final String PROPERTY_RELEASE_FINALIZE = 'finalizeRelease'

	/**
	 * @return the root project's 'gradle.properties' file path
	 */
	protected static final File getGradlePropertiesFile(Project project) {
		return project.rootProject.file(GRADLE_PROPERTIES)
	}

	/**
	 * @return the properties loaded from the root project's 'gradle.properties' file.
	 */
	protected static final Properties getGradleProperties(Project project) {
		def gradlePropFile = getGradlePropertiesFile(project)
		if(!gradlePropFile.exists()){
			throw new GradleException("Could not find 'gradle.properties' in root project!")
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
			System.out.println message
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
		message = "$PROMPT $message" + (defaultValue ? " [$defaultValue] " : "")
		if (System.console()) {
			return System.console().readLine(message) ?: defaultValue
		}
		println "$message (WAITING FOR INPUT BELOW)"
		return System.in.newReader().readLine() ?: defaultValue
	}
	
	/**
	 * @return the result of the provided closure
	 */
	public static final execClosure(String version, Closure closure){
		closure.delegate = this
		closure(version)
	}
}