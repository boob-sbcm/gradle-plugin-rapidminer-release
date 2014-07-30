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

import org.ajoberstar.grgit.Grgit
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

/**
 * A test specification for the {@link ReleasePlugin} class.
 * 
 * @author Nils Woehler
 *
 */
class TestReleasePlugin extends GitSpecification {

	private static final String TEST_TXT = 'test.txt'
	
	Project project
	ReleaseExtension ext
	GitScmProvider scmProvider

	/*
	 * Use Spock's setup() hook to initialize the properties for each test.
	 */
	def setup() {
		project = ProjectBuilder.builder().withName('releasePluginTest').withProjectDir(localRepoDir).build()
		project.apply plugin: ReleasePlugin

		// Write current version into gradle.properties
		def props = project.file(ReleaseHelper.GRADLE_PROPERTIES)
		props.withWriter { it << "version=${project.version}" }

		// Write test file
		project.file(TEST_TXT).withWriter {it << 'test'}

		// Commit added files
		grgitLocal.add(patterns: [ReleaseHelper.GRADLE_PROPERTIES, TEST_TXT] as List)
		grgitLocal.commit(message: 'Adds gradle.properties and test file')
	}

	// TODO add more test cases
	// https://github.com/townsfolk/gradle-release/blob/master/src/test/groovy/release/GitReleasePluginTests.groovy

	def 'should apply ReleasePlugin plugin'() {
		expect:
		project.plugins.findPlugin(ReleasePlugin)
	}

}
