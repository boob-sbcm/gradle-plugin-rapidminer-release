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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * The RapidMiner release plugin provides tasks to ease the process of create a project release
 * for a project that is managed via Git. 
 *
 * @author Nils Woehler
 */
class ReleasePlugin implements Plugin<Project> {

	protected static final String TASK_GROUP = 'Release'
	protected static final String CHECK_FOR_ILLEGAL_DEPENDENCIES_NAME = 'releaseCheckDependencies'
	protected static final String PREPARE_RELEASE_TASK_NAME = 'releasePrepare'
	protected static final String REFRESH_ARTIFACTS_TASK_NAME = 'releaseRefreshArtifacts'
	protected static final String RELEASE_CHECK_TASK_NAME = 'releaseCheck'
	protected static final String RELEASE_TASK_NAME = 'release'
	protected static final String FINALIZE_TASK_NAME = 'releaseFinalize'

	protected Project project
	protected GitScmProvider gitProvider
	protected String releaseBranch

	@Override
	void apply(Project project) {
		this.project = project

		ReleaseExtension extension = project.extensions.create('release', ReleaseExtension)

		this.gitProvider = new GitScmProvider(project.file('.'), project.logger, extension)
		this.releaseBranch = gitProvider.currentBranch

		addPrepareTasks(project, extension)
		addReleaseTasks(project, extension)
	}

	/**
	 * Adds release preparation tasks like checking for illegal dependencies, 
	 * merging changes to master, etc.
	 */
	def addPrepareTasks(Project project, ReleaseExtension extension) {

		// Create and configure dependency check task
		def ReleaseCheckDependencies checkIllegalDeps = project.tasks.create(name: CHECK_FOR_ILLEGAL_DEPENDENCIES_NAME, type: ReleaseCheckDependencies){
			description = '''Ensures that no illegal release dependency is referenced 
							 by the project.'''
			group = TASK_GROUP
		}

		// Create and configure release preparation task
		def ReleasePrepare prepareReleaseTask = project.tasks.create(name: PREPARE_RELEASE_TASK_NAME, type: ReleasePrepare){
			description = '''Prepares the project for a release. It performs some initial checks,
							 asks the user for the new release version and merges the current
							 branch to the defined master branch.'''
			group = TASK_GROUP
		}
		prepareReleaseTask.releaseBranch = releaseBranch
		prepareReleaseTask.scmProvider = gitProvider

		// Use conventionMapping for lazy initialization of task variables
		prepareReleaseTask.conventionMapping.with {
			masterBranch = { extension.masterBranch }
			pushToRemote = { extension.pushToRemote }
			generateTagName = { extension.generateTagName }
		}

		// Create and configure Maven artifact refresh task
		def ReleaseRefreshArtifacts refreshMavenArtifactsTask = project.tasks.create(name: REFRESH_ARTIFACTS_TASK_NAME, type: ReleaseRefreshArtifacts) {
			description = 'Ensures that the Maven publications have the correct ' +
					'version and coordinates after \'releasePrepare\' task has ' +
					'finished and the project version was updated dynamically.'
			group = TASK_GROUP
			mustRunAfter prepareReleaseTask
		}
		refreshMavenArtifactsTask.conventionMapping.with {
			releaseRepoUrl = { extension.releaseRepositoryUrl }
			snapshotRepoUrl = { extension.snapshotRepositoryUrl }
		}

		/*
		 * Wait for the release extension to be configured to perform last configuration steps. 
		 */
		project.afterEvaluate {
			if(!extension.skipIllegalDependenciesCheck) {
				project.logger.info("Adding ${checkIllegalDeps} as release preparation task dependency.")
				prepareReleaseTask.dependsOn checkIllegalDeps
			}
			extension.preparationTasks.each { Task task ->
				project.logger.info("Adding ${task} as release preparation task dependency.")
				prepareReleaseTask.dependsOn task
			}
		}
	}

	/**
	 * Adds and configures actual release tasks.
	 */
	def addReleaseTasks(Project project, ReleaseExtension extension) {

		// Create and configure release check task
		def ReleaseCheck releaseCheckTask = project.tasks.create(name: RELEASE_CHECK_TASK_NAME, type: ReleaseCheck){
			description = 'Performs some final release checks (e.g. check if release is ' +
					'performed on configured master branch, etc.)'
			group = TASK_GROUP
			mustRunAfter PREPARE_RELEASE_TASK_NAME, REFRESH_ARTIFACTS_TASK_NAME
		}
		releaseCheckTask.scmProvider = gitProvider
		releaseCheckTask.conventionMapping.with {
			masterBranch = { extension.masterBranch }
			generateTagName = { extension.generateTagName }
		}

		// Gather release task dependencies
		def releaseTasksDependencies = [releaseCheckTask]

		def withReleasePrepare = true

		// Check if the user has specified to omit prepareRelease task
		if(project.hasProperty(ReleaseHelper.PROPERTY_RELEASE_PREPARE)) {
			withReleasePrepare = Boolean.valueOf(project.properties[ReleaseHelper.PROPERTY_RELEASE_PREPARE])
		}

		if(withReleasePrepare) {
			releaseTasksDependencies << PREPARE_RELEASE_TASK_NAME
			releaseTasksDependencies << REFRESH_ARTIFACTS_TASK_NAME
		}

		// Create and configure the release task
		def Release releaseTask = project.tasks.create(name : RELEASE_TASK_NAME, type: Release){
			description = 'Releases the project by first invoking the defined release tasks '+
					'and than creating a tag.'
			group = TASK_GROUP
			dependsOn releaseTasksDependencies
		}
		releaseTask.scmProvider = gitProvider
		releaseTask.conventionMapping.with {
			pushToRemote = { extension.pushToRemote }
			generateTagName = { extension.generateTagName }
			generateTagMessage = { extension.generateTagMessage }
			createTag = { extension.createTag }
		}

		// Create and configure finalize release task
		def ReleaseFinalize finalizeTask = project.tasks.create(name: FINALIZE_TASK_NAME, type: ReleaseFinalize){
			description = 'Finalizes the release by merging changes from release ' +
					'branch back to develop and deleting the release branch ' +
					'(if configured). Will only be executed if \'releasePrepare\' '+
					'was executed beforehand.'
			group = TASK_GROUP
		}
		finalizeTask.releaseBranch = releaseBranch
		finalizeTask.scmProvider = gitProvider

		// use conventionMapping for lazy initialization of variables
		finalizeTask.conventionMapping.with {
			remote = { extension.remote }
			mergeToDevelop = { extension.mergeToDevelop }
			pushToRemote = { extension.pushToRemote }
			deleteReleaseBranch = { extension.deleteReleaseBranch }
			createTag = { extension.createTag }
			pushTags = { extension.createTag }
		}

		// Run finalize release task only if releasePrepare task will be executed
		// and execution was not omitted by the user
		def withReleaseFinalize = true
		if(project.hasProperty(ReleaseHelper.PROPERTY_RELEASE_FINALIZE)) {
			withReleaseFinalize = Boolean.valueOf(project.properties[ReleaseHelper.PROPERTY_RELEASE_FINALIZE])
		}
		if(withReleaseFinalize) {
			project.gradle.taskGraph.whenReady {taskGraph ->
				if(taskGraph.hasTask(PREPARE_RELEASE_TASK_NAME)) {
					releaseTask.finalizedBy finalizeTask
				}
			}
		}

		/*
		 * Wait for the extension configuration to be finished to
		 * configure release tasks. 
		 */
		project.afterEvaluate {
			extension.releaseTasks.each { Task task ->
				project.logger.info("Adding release task dependecy ${task.name}")
				releaseTask.dependsOn task
				task.mustRunAfter releaseTasksDependencies
			}
		}
	}
}
