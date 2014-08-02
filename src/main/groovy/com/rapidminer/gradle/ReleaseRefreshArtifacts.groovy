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
import org.gradle.api.publish.maven.MavenArtifact
import org.gradle.api.publish.maven.tasks.GenerateMavenPom
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.tasks.TaskAction

/**
 * This task is used to refresh artifacts published via the maven-publish 
 * mechanism after running a task which changes the project version during 
 * runtime (e.g. like 'releasePrepare').
 *
 * @author Nils Woehler
 *
 */
class ReleaseRefreshArtifacts extends DefaultTask {

	String releaseRepoUrl
	String snapshotRepoUrl

	@TaskAction
	def refreshArtifacts() {

		// Marker needed as refreshing the version of one publish tasks
		// also updates the version of any other publish task.
		// This way we skip all artifacts only if version was up-to-date
		// when invoking the task.
		def versionChecked = false
		def oldVersion = null

		// Refresh all Maven publishing tasks
		project.tasks.withType(PublishToMavenRepository) { PublishToMavenRepository publishTask ->

			if(!versionChecked) {
				if(publishTask.publication.version == project.version) {
					project.logger.info("Skipping refresh of ${publishTask} artifacts. Version already up-to-date.")
					return
				} else {
					project.logger.info("Updating publication version from ${publishTask.publication.version} to ${project.version}")
					versionChecked = true
					oldVersion = publishTask.publication.version
					publishTask.publication.version = project.version
				}
			}

			project.logger.info("Refreshing artifacts of ${publishTask}")

			def snapshotURL = getSnapshotRepoUrl()
			def releaseURL = getReleaseRepoUrl()

			assert snapshotURL, 'No snapshot repository URL defined. Cannot update artifacts.'
			assert releaseURL, 'No release repository URL defined. Cannot update artifacts.'

			// First remember and remove old artifacts
			def oldArtifacts = publishTask.publication.artifacts.toArray()
			publishTask.publication.artifacts.clear()

			// Update publish task with new artifacts with correct source
			oldArtifacts.each({ MavenArtifact artifact ->
				project.logger.info("Updating artifact ${artifact.file}")
				def newPath = artifact.file.absolutePath.replaceAll(oldVersion, project.version)
				project.logger.info("New path: ${newPath}")
				publishTask.publication.artifacts.artifact(
						source:      	newPath,
						classifier:  	artifact.classifier,
						extension:  	artifact.extension
						)
			})

			// If repository URL contains release or snapshot repository
			def snapshotURI = new URI(snapshotURL)
			def releaseURI = new URI(releaseURL)
			if(publishTask.repository.url.equals(releaseURI) || publishTask.repository.url.equals(snapshotURI)) {
				// adapt URL according to current version
				def newURL = null
				if(project.version.endsWith(ReleaseHelper.SNAPSHOT)) {
					newURL = snapshotURI
				} else {
					newURL = releaseURI
				}
				project.logger.info("Updating publish repository from ${publishTask.repository.url} to ${newURL}")
				publishTask.repository.url = newURL
			} else {
				project.logger.info("Skipping repository update for repositoy with URL '${publishTask.repository.url}'")
			}
		}
		// Also update POM generation tasks with current version
		project.tasks.withType(GenerateMavenPom).each { GenerateMavenPom generateMavenPomTask ->
			if(generateMavenPomTask.pom.getProjectIdentity().version != project.version) {
				project.logger.info "Updating POM from version ${generateMavenPomTask.pom.getProjectIdentity().version} to version ${project.version}"
				generateMavenPomTask.pom.getProjectIdentity().version = project.version
			}
		}
	}
}