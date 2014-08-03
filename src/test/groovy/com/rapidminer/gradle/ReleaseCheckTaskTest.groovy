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
import org.gradle.testfixtures.ProjectBuilder

import spock.lang.Ignore;

/**
 * An integration test specification for the {@link ReleaseCheck} class
 * and the 'releaseCheck' task.
 * 
 * @author Nils Woehler
 *
 */
class ReleaseCheckTaskTest extends AbstractReleaseTaskSpecification {

	def 'check error when not on master branch'() {
		given:
		scmProvider.switchToBranch('develop')

		when:
		ExecutionResult result = runTasksWithFailure('releaseCheck')

		then:
		result.failure.cause.cause.message == "Release task was not executed on defined master branch 'master' but on 'develop'"
	}

	def 'check on master-test branch'() {
		given:
		repo.checkout(branch: 'master-test', createBranch: true)
		buildFile << '''
			release {
				masterBranch = 'master-test'
			}
		'''.stripIndent()
		scmProvider.commit('Commit', [buildFile.name])

		when:
		ExecutionResult result = runTasksSuccessfully('releaseCheck')

		then:
		noExceptionThrown()
	}

	
	@Ignore(value='Not yet implemented')
	def 'check upstream changes'() {
		//TODO add check for upstream changes
	}

	def 'check all okay with defaults'() {
		given:
		scmProvider.switchToBranch('master')

		when:
		ExecutionResult result = runTasksSuccessfully('releaseCheck')

		then:
		noExceptionThrown()
	}

	def 'check uncommitted changes'() {
		given:
		repo.checkout(branch: 'master-test', createBranch: true)
		buildFile << '''
			release {
				masterBranch = 'master-test'
			}
		'''.stripIndent()

		when:
		ExecutionResult result = runTasksWithFailure('releaseCheck')

		then:
		result.failure.cause.cause.message == 'Git repository has uncommitted changes.'
	}

	def 'check commit is tag'() {
		given:
		scmProvider.switchToBranch('master')
		buildFile << '''
			release {
				generateTagName = { version -> "v${version}" }
			}
		'''
		scmProvider.commit('Commit', [buildFile.name])
		scmProvider.tag('v1.0.0', 'Create tag with version 1.0.0')

		when:
		ExecutionResult result = runTasksWithFailure('releaseCheck')

		then:
		result.failure.cause.cause.message == 'Cannot create new release. Current commit is tag \'v1.0.0\'!'
	}
	
	def 'check tag already present'() {
		given:
		scmProvider.switchToBranch('master')
		buildFile << '''
			release {
				generateTagName = { version -> "v${version}" }
			}
		'''
		scmProvider.commit('Commit', [buildFile.name])
		scmProvider.tag('v1.0.0', 'Create tag with version 1.0.0')
		buildFile << '''
			apply plugin: 'java'
		'''
		scmProvider.commit('Commit', [buildFile.name])

		when:
		ExecutionResult result = runTasksWithFailure('releaseCheck')

		then:
		result.failure.cause.cause.message == 'Cannot create new release. Tag with name \'v1.0.0\' already exists!'
	}
}
