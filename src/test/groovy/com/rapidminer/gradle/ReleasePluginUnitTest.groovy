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

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.testfixtures.ProjectBuilder

/**
 * A test specification for the {@link ReleasePlugin} class.
 * 
 * @author Nils Woehler
 *
 */
class ReleasePluginUnitTest extends AbstractGitSpecification {

	def 'should apply ReleasePlugin plugin and has tasks'() {
		given:
		project.apply plugin: ReleasePlugin

		expect:
		project.plugins.findPlugin(ReleasePlugin)
		project.tasks.findByPath(ReleasePlugin.RELEASE_TASK_NAME)
		project.tasks.findByPath(ReleasePlugin.FINALIZE_TASK_NAME)
		project.tasks.findByPath(ReleasePlugin.RELEASE_CHECK_TASK_NAME)
		project.tasks.findByPath(ReleasePlugin.CHECK_FOR_ILLEGAL_DEPENDENCIES_NAME)
		project.tasks.findByPath(ReleasePlugin.PREPARE_RELEASE_TASK_NAME)
		project.tasks.findByPath(ReleasePlugin.REFRESH_ARTIFACTS_TASK_NAME)
	}

	def 'check releaseRefreshArtifacts mustRunAfter'() {
		given:
		project.apply plugin: ReleasePlugin
		Task refresh = project.tasks.findByPath(ReleasePlugin.REFRESH_ARTIFACTS_TASK_NAME)
		Task prepare = project.tasks.findByPath(ReleasePlugin.PREPARE_RELEASE_TASK_NAME)

		expect:
		refresh.getMustRunAfter().getDependencies(refresh).contains(prepare)
	}

	def 'check prepareReleaseTask dependsOn illegal dependencies check'() {
		given:
		project.apply plugin: ReleasePlugin
		Task prepare = project.tasks.findByPath(ReleasePlugin.PREPARE_RELEASE_TASK_NAME)
		Task check = project.tasks.findByPath(ReleasePlugin.CHECK_FOR_ILLEGAL_DEPENDENCIES_NAME)

		when:
		project.evaluate()

		then:
		prepare.getDependsOn().contains(check)
	}

	def 'check prepareRelease task depends not on illegal dependencies check if specified'() {
		given:
		project.apply plugin: ReleasePlugin
		project.release { skipIllegalDependenciesCheck = true }
		Task prepare = project.tasks.findByPath(ReleasePlugin.PREPARE_RELEASE_TASK_NAME)
		Task check = project.tasks.findByPath(ReleasePlugin.CHECK_FOR_ILLEGAL_DEPENDENCIES_NAME)

		when:
		project.evaluate()

		then:
		!prepare.getDependsOn().contains(check)
	}

	def 'check prepareRelease task dependsOn preparationTasks tasks'() {
		given:
		def taskName1 = 'prepare1'
		def taskName2 = 'prepare2'
		Task t1 = project.tasks.create(name: taskName1)
		project.tasks.create(name: taskName2)
		project.apply plugin: ReleasePlugin
		project.release {
			preparationTasks = [t1, taskName2]
		}
		Task prepare = project.tasks.findByPath(ReleasePlugin.PREPARE_RELEASE_TASK_NAME)

		when:
		project.evaluate()

		then:
		prepare.getDependsOn().contains(t1)
		prepare.getDependsOn().contains(taskName2)
	}

	def 'check releaseCheck mustRunAfter'() {
		given:
		project.apply plugin: ReleasePlugin
		Task releaseCheck = project.tasks.findByPath(ReleasePlugin.RELEASE_CHECK_TASK_NAME)
		Task prepare = project.tasks.findByPath(ReleasePlugin.PREPARE_RELEASE_TASK_NAME)
		Task refresh = project.tasks.findByPath(ReleasePlugin.REFRESH_ARTIFACTS_TASK_NAME)
		def mustRunAfter = releaseCheck.getMustRunAfter().getDependencies(releaseCheck)

		expect:
		mustRunAfter.contains(prepare)
		mustRunAfter.contains(refresh)
	}

	/*
	 * Cannot check for releaseFinalize task as this requires the taskGraph to be
	 * created.
	 */
	def 'check release depends on on releasePrepare and releaseRefreshArtifacts by default'() {
		given:
		project.apply plugin: ReleasePlugin
		Task releaseTask = project.tasks.findByPath(ReleasePlugin.RELEASE_TASK_NAME)
		Task check = project.tasks.findByPath(ReleasePlugin.RELEASE_CHECK_TASK_NAME)

		when:
		project.evaluate()

		then:
		releaseTask.getDependsOn().contains(check)
		releaseTask.getDependsOn().contains(ReleasePlugin.PREPARE_RELEASE_TASK_NAME)
		releaseTask.getDependsOn().contains(ReleasePlugin.REFRESH_ARTIFACTS_TASK_NAME)
	}

	def 'check release depends not on releasePrepare and releaseRefreshArtifacts if disabled'() {
		given:
		project.ext { prepareRelease = false }
		project.apply plugin: ReleasePlugin
		Task releaseTask = project.tasks.findByPath(ReleasePlugin.RELEASE_TASK_NAME)
		Task check = project.tasks.findByPath(ReleasePlugin.RELEASE_CHECK_TASK_NAME)

		when:
		project.evaluate()

		then:
		releaseTask.getDependsOn().contains(check)
		!releaseTask.getDependsOn().contains(ReleasePlugin.PREPARE_RELEASE_TASK_NAME)
		!releaseTask.getDependsOn().contains(ReleasePlugin.REFRESH_ARTIFACTS_TASK_NAME)
	}

	def 'check release depends not on releaseFinalize if disabled'() {
		given:
		project.ext { finalizeRelease = false }
		project.apply plugin: ReleasePlugin
		Task releaseTask = project.tasks.findByPath(ReleasePlugin.RELEASE_TASK_NAME)
		Task check = project.tasks.findByPath(ReleasePlugin.RELEASE_CHECK_TASK_NAME)

		when:
		project.evaluate()

		then:
		releaseTask.getDependsOn().contains(check)
		releaseTask.getDependsOn().contains(ReleasePlugin.PREPARE_RELEASE_TASK_NAME)
		releaseTask.getDependsOn().contains(ReleasePlugin.REFRESH_ARTIFACTS_TASK_NAME)
	}

	def 'check release task dependsOn releaseTasks tasks'() {
		given:
		def taskName1 = 'release1'
		def taskName2 = 'release2'
		Task t1 = project.tasks.create(name: taskName1)
		Task t2 = project.tasks.create(name: taskName2)
		project.apply plugin: ReleasePlugin
		
		// Test task and String definition of release tasks
		project.release { releaseTasks = [t1, taskName2] }
		
		Task release = project.tasks.findByPath(ReleasePlugin.RELEASE_TASK_NAME)
		Task check = project.tasks.findByPath(ReleasePlugin.RELEASE_CHECK_TASK_NAME)
		Task prepare = project.tasks.findByPath(ReleasePlugin.PREPARE_RELEASE_TASK_NAME)
		Task refresh = project.tasks.findByPath(ReleasePlugin.REFRESH_ARTIFACTS_TASK_NAME)

		when:
		project.evaluate()

		then:
		release.getDependsOn().contains(t1)
		release.getDependsOn().contains(taskName2)
		t1.getMustRunAfter().getDependencies(t1).contains(prepare)
		t1.getMustRunAfter().getDependencies(t1).contains(check)
		t1.getMustRunAfter().getDependencies(t1).contains(refresh)
		t2.getMustRunAfter().getDependencies(t2).contains(prepare)
		t2.getMustRunAfter().getDependencies(t2).contains(check)
		t2.getMustRunAfter().getDependencies(t2).contains(refresh)
	}
}
