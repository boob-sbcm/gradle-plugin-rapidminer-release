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

import nebula.test.functional.ExecutionResult

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

import spock.lang.Ignore

/**
 * An integration test specification for the {@link PrepareRelease} class
 * and the 'releasePrepare' task.
 * 
 * @author Nils Woehler
 *
 */
class ReleasePrepareTaskTest extends AbstractReleaseTaskSpecification {

	def setup() {
		scmProvider.switchToBranch('develop')

		def gradleProperties = new Properties()
		propertiesFile.withReader { reader ->
			gradleProperties.load(reader)
		}
		gradleProperties.vesion = '1.0.1-SNAPSHOT'
		gradleProperties.store(propertiesFile.newWriter(), null)
		scmProvider.commit('Update version', [propertiesFile.name])
	}

	def 'check error on master branch'() {
		given:
		scmProvider.switchToBranch('master')

		when:
		ExecutionResult result = runTasksWithFailure(ReleasePlugin.PREPARE_RELEASE_TASK_NAME)

		then:
		result.failure.cause.cause.message == 'Cannot prepare release. Release branch is master branch \'master\'!'
	}

	def 'check error on master-test branch'() {
		given:
		repo.checkout(branch: 'master-test', createBranch: true)
		buildFile << '''
			release {
				masterBranch = 'master-test'
			}
		'''.stripIndent()
		scmProvider.commit('Commit', [buildFile.name])

		when:
		ExecutionResult result = runTasksWithFailure(ReleasePlugin.PREPARE_RELEASE_TASK_NAME)

		then:
		result.failure.cause.cause.message == 'Cannot prepare release. Release branch is master branch \'master-test\'!'
	}

	@Ignore(value='Not yet implemented')
	def 'check upstream changes'() {
		//TODO add check for upstream changes
	}

	def 'check uncommitted changes'() {
		given:
		buildFile << '''
			release {
				masterBranch = 'master'
			}
		'''.stripIndent()

		when:
		ExecutionResult result = runTasksWithFailure(ReleasePlugin.PREPARE_RELEASE_TASK_NAME)

		then:
		result.failure.cause.cause.message == 'Git repository has uncommitted changes.'
	}

	def 'check wrong release version format'() {
		given:
		def gradleProperties = new Properties()
		propertiesFile.withReader { reader ->
			gradleProperties.load(reader)
		}
		gradleProperties.version = '1.0.1asd'
		gradleProperties.store(propertiesFile.newWriter(), null)
		scmProvider.commit('Update version', [propertiesFile.name])

		when:
		ExecutionResult result = runTasksWithFailure(ReleasePlugin.PREPARE_RELEASE_TASK_NAME)

		then:
		result.failure.cause.cause.message == 'Release version is of wrong format. Correct format example: \'1.0.003\'.'
	}

	def 'check predefined release version via project property'() {
		given:
		buildFile << '\next.releaseVersion=\'1.0.0asdfa\'\n'
		scmProvider.commit('Update', [buildFile.name])

		when:
		ExecutionResult result = runTasksWithFailure(ReleasePlugin.PREPARE_RELEASE_TASK_NAME)

		then:
		result.failure.cause.cause.message == 'Release version is of wrong format. Correct format example: \'1.0.003\'.'
	}

	def 'check non-interactive prepare process'() {
		given:
		buildFile << '\next.interactiveRelease=false\n'
		scmProvider.commit('Update', [buildFile.name])

		when:
		ExecutionResult result = runTasksSuccessfully(ReleasePlugin.PREPARE_RELEASE_TASK_NAME)

		then:
		scmProvider.currentBranch == 'master'
		propertiesFile.text.contains 'version=1.0.0'
		result.standardOutput.contains('Skip pushing of changes as master branch isn\'t tracking any remote repository')
	}

	def 'check askForReleaseVersion empty input'() {
		given:
		Project project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.configure(project) { apply plugin: ReleasePlugin }
		ReleasePrepare prepare = project.tasks.findByName(ReleasePlugin.PREPARE_RELEASE_TASK_NAME)

		ByteArrayInputStream input = new ByteArrayInputStream(''.bytes)
		System.setIn input

		expect:
		prepare.askForReleaseVersion('1.0.0') == '1.0.0'
	}
	
	def 'check askForReleaseVersion with yeah input'() {
		given:
		Project project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.configure(project) { apply plugin: ReleasePlugin }
		ReleasePrepare prepare = project.tasks.findByName(ReleasePlugin.PREPARE_RELEASE_TASK_NAME)

		ByteArrayInputStream input = new ByteArrayInputStream('yeah'.bytes)
		System.setIn input

		expect:
		prepare.askForReleaseVersion('1.0.0') == '1.0.0'
	}
	
	def 'check askForReleaseVersion with different release version'() {
		given:
		Project project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.configure(project) { apply plugin: ReleasePlugin }
		ReleasePrepare prepare = project.tasks.findByName(ReleasePlugin.PREPARE_RELEASE_TASK_NAME)
		
		ByteArrayInputStream input = new ByteArrayInputStream('no'.bytes)
		System.setIn input

		when:
		prepare.askForReleaseVersion('1.0.000')
		
		then:
		// Expect a GradleException as we cannot provide input to the askForUpdatedReleaseVersion method
		thrown(GradleException)
	}

	def 'check askForUpdatedReleaseVersion with invalid version number'() {
		given:
		Project project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.configure(project) { apply plugin: ReleasePlugin }
		ReleasePrepare prepare = project.tasks.findByName(ReleasePlugin.PREPARE_RELEASE_TASK_NAME)
		
		ByteArrayInputStream input = new ByteArrayInputStream('1.1.1asf'.bytes)
		System.setIn input
		
		when:
		prepare.askForUpdatedReleaseVersion()
		
		then:
		GradleException e = thrown(GradleException)
		e.message.startsWith "Invalid release version number: '1.1.1asf'"
	}
	
	def 'check askForUpdatedReleaseVersion with correct version number'() {
		given:
		Project project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.configure(project) { apply plugin: ReleasePlugin }
		ReleasePrepare prepare = project.tasks.findByName(ReleasePlugin.PREPARE_RELEASE_TASK_NAME)
		
		ByteArrayInputStream input = new ByteArrayInputStream('1.1.1'.bytes)
		System.setIn input
		
		expect:
		prepare.askForUpdatedReleaseVersion() == '1.1.1'
	}
	
	def 'check verifyReleaseInput'() {
		given:
		Project project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.configure(project) { apply plugin: ReleasePlugin }
		ReleasePrepare prepare = project.tasks.findByName(ReleasePlugin.PREPARE_RELEASE_TASK_NAME)
		// exchange random as we otherwise would have to parse the function from std.out
		prepare.random = new Random(0)
		
		ByteArrayInputStream input = new ByteArrayInputStream('10'.bytes)
		System.setIn input
		
		when:
		prepare.verifyReleaseInput('develop', '1.1.1')
		
		then:
		noExceptionThrown()
	}
	
	def 'check verifyReleaseInput with wrong result'() {
		given:
		Project project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.configure(project) { apply plugin: ReleasePlugin }
		ReleasePrepare prepare = project.tasks.findByName(ReleasePlugin.PREPARE_RELEASE_TASK_NAME)
		// exchange random as we otherwise would have to parse the function from std.out
		prepare.random = new Random(0)
		
		ByteArrayInputStream input = new ByteArrayInputStream('9'.bytes)
		System.setIn input
		
		when:
		prepare.verifyReleaseInput('develop', '1.1.1')
		
		then:
		GradleException e = thrown()
		e.message.startsWith 'Aborting after trying three times to acknowledge'
	}
}
