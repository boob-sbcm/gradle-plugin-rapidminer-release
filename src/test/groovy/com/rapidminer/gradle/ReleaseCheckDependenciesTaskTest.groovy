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

import nebula.test.IntegrationSpec
import nebula.test.functional.ExecutionResult

import org.ajoberstar.grgit.Grgit
import org.gradle.api.GradleException

/**
 * An integration test specification for the {@link ReleaseCheckDependencies} class
 * and the 'releaseCheckDependencies' task.
 * 
 * @author Nils Woehler
 *
 */
class ReleaseCheckDependenciesTaskTest extends AbstractReleaseTaskSpecification {

	def 'run releaseCheckDependencies task without dependencies'() {
		when:
		ExecutionResult result = runTasks('releaseCheckDependencies')
		
		then: 
		result.failure == null
		noExceptionThrown()
	}
	
	def 'run releaseCheckDependencies task with correct dependencies'() {
		given:
		buildFile << '''
            apply plugin: 'java'

			dependencies {
				compile 'com.rapidminer.studio:rapidminer-osx-adapter:1.0.0'
			}
        '''.stripIndent()
		
		when:
		ExecutionResult result = runTasksSuccessfully('releaseCheckDependencies')
		
		then:
		result.failure == null
		noExceptionThrown()
	}
	
	def 'run releaseCheckDependencies with RC dependency'() {
		given:
		buildFile << '''
            apply plugin: 'java'

			dependencies {
				compile 'net.sf.buildbox:args-inject:1.0.0-rc-1'
			}
        '''.stripIndent()
		
		when:
		ExecutionResult result = runTasksWithFailure('releaseCheckDependencies')
		
		then:
		result.failure != null
		result.failure.cause.cause.message.equals('Project depends on dependencies that are forbidden in a release version!')
	}
	
	def 'run releaseCheckDependencies with SNAPSHOT dependency'() {
		given:
		buildFile << '''
            apply plugin: 'java'

			dependencies {
				compile 'com.rapidminer.studio:rapidminer-studio-commons:6.0.8-SNAPSHOT'
			}
        '''.stripIndent()
		
		when:
		ExecutionResult result = runTasksWithFailure('releaseCheckDependencies')
		
		then:
		result.failure != null
		result.failure.cause.cause.message.equals('Project depends on dependencies that are forbidden in a release version!')
	}

}
