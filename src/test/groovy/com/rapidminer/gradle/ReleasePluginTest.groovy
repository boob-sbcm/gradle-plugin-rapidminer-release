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
class ReleasePluginTest extends GitSpecification {

	Project project

	/*
	 * Use Spock's setup() hook to initialize the properties for each test.
	 */
	def setup() {
		project = ProjectBuilder.builder().withName('releasePluginTest').withProjectDir(localRepoDir).build()
		project.apply plugin: ReleasePlugin
	}

	// TODO add more test cases
	// https://github.com/townsfolk/gradle-release/blob/master/src/test/groovy/release/GitReleasePluginTests.groovy

	def 'should apply ReleasePlugin plugin'() {
		expect:
		project.plugins.findPlugin(ReleasePlugin)
	}
	
	def 'has release task'() {
		expect:
		project.tasks.findByPath(':release')
	}
	
	def 'has releaseFinalize task'() {
		expect:
		project.tasks.findByPath(':releaseFinalize')
	}
	
	def 'has releaseCheck task'() {
		expect:
		project.tasks.findByPath(':releaseCheck')
	}
	
	def 'has releaseCheckDependencies task'() {
		expect:
		project.tasks.findByPath(':releaseCheckDependencies')
	}
	
	def 'has releasePrepare task'() {
		expect:
		project.tasks.findByPath(':releasePrepare')
	}
	
	def 'has releaseRefreshArtifacts task'() {
		expect:
		project.tasks.findByPath(':releaseRefreshArtifacts')
	}

}
