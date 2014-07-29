package com.rapidminer.gradle

import org.ajoberstar.grgit.*
import org.gradle.api.DefaultTask
import org.gradle.api.publish.maven.tasks.GenerateMavenPom
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.tasks.TaskAction

/**
 *
 * @author Nils Woehler
 *
 */
class RefreshArtifactsTask extends DefaultTask {

	String releaseRepoUrl
	String snapshotRepoUrl

	@TaskAction
	def refreshArtifacts() {
		// Refresh Maven artifacts
		project.tasks.withType(PublishToMavenRepository) { publishTask ->

			// First remember and remove old artifacts
			def oldArtifacts = publishTask.publication.artifacts.toArray()
			publishTask.publication.artifacts.clear()

			// Update publish task with new artifacts with correct version
			oldArtifacts.each({ artifact ->
				def newPath = artifact.file.getAbsolutePath().replaceAll(publishTask.publication.version, project.version)
				publishTask.publication.artifacts.artifact(
						source:      	newPath,
						classifier:  	artifact.classifier,
						extension:  	artifact.extension
						)
			})
			publishTask.publication.version = project.version

			// If repository URL contains release or snapshot repository
			def snapshotURI = new URI(getSnapshotRepoUrl())
			def releaseURI = new URI(getReleaseRepoUrl())
			if(publishTask.repository.url.equals(releaseURI) ||
				publishTask.repository.url.equals(snapshotURI)) {
				// adapt URL according to current version
				if(project.version.endsWith(ReleaseHelper.SNAPSHOT)) {
					publishTask.repository.url = snapshotURI
				} else {
					publishTask.repository.url = releaseURI
				}
			}
		}
		// Also update POM generation tasks with latest version
		project.tasks.withType(GenerateMavenPom).each { generateMavenPomTask ->
			generateMavenPomTask.pom.getProjectIdentity().version = project.version
		}
	}
}