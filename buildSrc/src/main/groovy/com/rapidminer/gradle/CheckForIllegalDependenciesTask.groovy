package com.rapidminer.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction


/**
 * A task that checks whether any project specifies a SNAPSHOT dependency in any configuration.
 *
 * @author Nils Woehler
 *
 */
class CheckForIllegalDependenciesTask extends DefaultTask {

	/**
	 * Checks whether the project specifies any snapshot dependencies.
	 */
	@TaskAction
	def checkForSnapshotDependencies() {
		logger.info("Checking for SNAPSHOT dependencies...")
		def illegalDependencies = []
		project.allprojects.each { p ->
			p.configurations.each { config ->
				config.dependencies.each { dep ->
					def lowerCaseVersion = dep.version?.toLowerCase()
					if(lowerCaseVersion && isIllegal(lowerCaseVersion)) {
						illegalDependencies << [project: p.name, conf: config.name, dependency: dep]
					}
				}
			}
		}
		if(illegalDependencies.size() != 0) {
			println "Project depends on following illegal release dependencies: "
			illegalDependencies.each { found -> println "  Project: '${found.project}', Configuration: '${found.conf}', Artefact: '${found.dependency.group}:${found.dependency.name}:${found.dependency.version}'" }
			throw new IllegalStateException("Project depends on dependencies that are forbidden in a release version!")
		}
	}
	
	def isIllegal(lowerCaseVersion) {
		if(lowerCaseVersion.endsWith(ReleaseHelper.SNAPSHOT.toLowerCase())) {
			return true
		}
		if(lowerCaseVersion.contains(ReleaseHelper.RC.toLowerCase())) {
			return true
		}
		return false
	}
}