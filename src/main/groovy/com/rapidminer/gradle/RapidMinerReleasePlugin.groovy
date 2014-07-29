package com.rapidminer.gradle

import org.ajoberstar.grgit.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task



/**
 *
 * @author Nils Woehler
 *
 */
class RapidMinerReleasePlugin implements Plugin<Project> {

	protected static final String TASK_GROUP = "Release"
	protected static final String CHECK_FOR_ILLEGAL_DEPENDENCIES_NAME = "releaseCheckDependencies"
	protected static final String PREPARE_TASK_NAME = "releasePrepare"
	protected static final String FINALIZE_TASK_NAME = "releaseFinalize"
	protected static final String RELEASE_TASK_NAME = "release"
	protected static final String REFRESH_TASK_NAME = "releaseRefreshArtifacts"

	protected Project project

	@Override
	void apply(Project project) {
		this.project = project

		RapidMinerReleaseExtension extension = project.extensions.create('release', RapidMinerReleaseExtension)
		Grgit grgit = Grgit.open(project.file('.'))
		def String releaseBranch = grgit.branch.current.name

		addReleaseFinalizeTask(project, extension, grgit, releaseBranch)
		addCheckForIllegalDependencies(project, extension)
		addPrepareTask(project, extension, grgit, releaseBranch)
		addReleaseTask(project, extension, grgit)
	}

	def addCheckForIllegalDependencies(Project project, RapidMinerReleaseExtension extension) {
		project.tasks.create(name: CHECK_FOR_ILLEGAL_DEPENDENCIES_NAME, type: CheckForIllegalDependenciesTask){
			description = 'Ensures that no illegal release dependency is referenced by the project.'
			group = TASK_GROUP
		}
	}

	def addPrepareTask(Project project, RapidMinerReleaseExtension extension, Grgit gr, String currentBranch) {
		def Task prepareReleaseTask = project.tasks.create(name : PREPARE_TASK_NAME, type: PrepareReleaseTask){
			description = 'Ensures the project is ready to be released.'
			group = TASK_GROUP
			grgit = gr
			releaseBranch = currentBranch
		}
		// Use conventionMapping for lazy initialization of task variables
		prepareReleaseTask.conventionMapping.with {
			remote = { extension.remote }
			masterBranch = { extension.masterBranch }
			createTag = { extension.createTag }
			generateTagName = { extension.generateTagName }
			generateTagMessage = { extension.generateTagMessage }
		}

		project.afterEvaluate {
			if(!extension.skipIllegalDependenciesCheck) {
				project.logger.info("Adding illegal dependecy check as release preparation task dependecy")
				prepareReleaseTask.dependsOn CHECK_FOR_ILLEGAL_DEPENDENCIES_NAME
			}
			extension.preparationTasks.each { Task task ->
				project.logger.info("Adding release preparation task dependecy ${task.name}")
				prepareReleaseTask.dependsOn task
			}

			def Task refreshMavenArtifactsTask = project.tasks.create(name: REFRESH_TASK_NAME, type: RefreshArtifactsTask) {
				description = 'Ensures that the Maven publications have the correct version and coordinates after releasePrepare task has finished.'
				group = TASK_GROUP
				mustRunAfter prepareReleaseTask
			}
			refreshMavenArtifactsTask.conventionMapping.with {
				releaseRepoUrl = { extension.releaseRepositoryUrl }
				snapshotRepoUrl = { extension.snapshotRepositoryUrl }
			}
		}
	}

	def addReleaseFinalizeTask(Project project, RapidMinerReleaseExtension extension, Grgit gr, String relBranch) {
		def Task finalizeTask = project.tasks.create(name : FINALIZE_TASK_NAME, type: FinalizeReleaseTask){
			description = 'Finalizes the release by merging changes from release branch back to develop and deletes the release branch (if configured).'
			group = TASK_GROUP
			grgit = gr
			releaseBranch = relBranch
		}

		// use conventionMapping for lazy initialization of variables
		finalizeTask.conventionMapping.with {
			remote = { extension.remote }
			masterBranch = { extension.masterBranch }
			mergeToDevelop = { extension.mergeToDevelop }
			pushChangesToRemote = { extension.pushChangesToRemote }
			deleteReleaseBranch = { extension.deleteReleaseBranch }
			pushTags = { extension.createTag }
		}
	}

	def addReleaseTask(Project project, RapidMinerReleaseExtension extension, Grgit grgit) {
		def releaseTask = project.tasks.create(name : RELEASE_TASK_NAME){
			description = 'Releases the project by first preparing a release and than invoking the actual release tasks.'
			group = TASK_GROUP
			dependsOn PREPARE_TASK_NAME, REFRESH_TASK_NAME
			finalizedBy FINALIZE_TASK_NAME
		}

		project.afterEvaluate {
			extension.releaseTasks.each { Task task ->
				project.logger.info("Adding release task dependecy ${task.name}")
				releaseTask.dependsOn task
				task.mustRunAfter PREPARE_TASK_NAME, REFRESH_TASK_NAME
			}
		}
	}
}
