package com.rapidminer.gradle

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

	protected static final File getGradlePropertiesFile(project) {
		return project.rootProject.file(GRADLE_PROPERTIES)
	}
	
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
}