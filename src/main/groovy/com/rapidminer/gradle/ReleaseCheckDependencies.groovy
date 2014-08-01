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

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction


/**
 * A task that checks whether any project specifies an illegal (e.g. SNAPSHOT) dependency in any configuration.
 *
 * @author Nils Woehler
 *
 */
class ReleaseCheckDependencies extends DefaultTask {

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
			ReleaseHelper.println "Project depends on following illegal release dependencies: "
			illegalDependencies.each { found -> ReleaseHelper.println "  Project: '${found.project}', Configuration: '${found.conf}', Artefact: '${found.dependency.group}:${found.dependency.name}:${found.dependency.version}'" }
			throw new GradleException("Project depends on dependencies that are forbidden in a release version!")
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