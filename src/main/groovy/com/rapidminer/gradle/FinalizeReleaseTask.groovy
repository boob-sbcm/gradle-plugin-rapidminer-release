package com.rapidminer.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.ajoberstar.grgit.*

/**
 *
 * @author Nils Woehler
 *
 */
class FinalizeReleaseTask extends DefaultTask {

	protected static final String DEVELOP = "develop"

	/**
	 * The repository to perform the release on.
	 */
	def Grgit grgit
	def releaseBranch
	def masterBranch
	def remote
	def mergeToDevelop
	def	pushChangesToRemote
	def	deleteReleaseBranch
	def pushTags
	def releaseVersion

	@TaskAction
	def finalizeRelease() {
		if(!releaseVersion) {
			throw new IllegalStateException("Release version not set! Finalize release task must run after prepare release task.")
		}
		
		/*
		 * 1. Switch back to release branch
		 */
		logger.info("Switching back to to '${releaseBranch}' branch.")
		grgit.checkout(branch: releaseBranch)

		// remember the remote release branch
		def trackingReleaseBranch = grgit.branch.current.trackingBranch
		if(trackingReleaseBranch) {
			logger.info("Release branch is tracking branch ${trackingReleaseBranch.fullName}")
		}

		/*
		 * 2. Increase version in 'gradle.properties' to new version 
		 */
		def gradleProperties = ReleaseHelper.getGradleProperties(project)
		logger.info("Adapting version to next development cycle and committing 'gradle.properties'.")
		gradleProperties.version = getNextVersion(releaseVersion)
		gradleProperties.store(ReleaseHelper.getGradlePropertiesFile(project).newWriter(), null)

		/*
		 * 3. Add and commit changed gradle.properties
		 */
		grgit.add(patterns : [
			ReleaseHelper.GRADLE_PROPERTIES
		])

		grgit.commit(message: "Preparing next development cycle (${gradleProperties.version})")

		// A list of all branches that will be pushed to remote
		def toPush = [masterBranch]

		// If release branch is not develop, check if changes should be merged back to develop
		if(mergeBackToDevelop()) {
			logger.info("Release branch wasn't 'develop' branch. Merging release branch to develop.")
			toPush << DEVELOP

			logger.info("Switching to 'develop' branch.")
			grgit.checkout(branch: DEVELOP, createBranch: false)

			logger.info("Merging '${releaseBranch}' branch to 'develop' branch.")
			grgit.merge(head: releaseBranch)
		} else {
			logger.info("Will not merge back changes to develop. Either because we are on develop or because it has been disabled.")
		}
		
		/*
		 * Check if release branch should be deleted
		 */
		if(mergeBackToDevelop() && deleteReleaseBranch) {
			def toRemove = [releaseBranch]
			// also delete branch on remote if available
			if(trackingReleaseBranch) {
				toRemove << trackingReleaseBranch.fullName
			}
			logger.info("Deleting release branches ${toRemove}.")
			grgit.branch.remove(names: toRemove)
		} else {
			// release branch should not be deleted and might even be develop -> push it to remote
			toPush << releaseBranch
		}

		/*
		 * Push changes to remote
		 */
		if(pushChangesToRemote) {
			logger.info("Pushing following branches to remote: ${toPush}")
			grgit.push(remote: remote, refsOrSpecs: toPush, tags: pushTags)
		}

	}

	/**
	 * @return <code>true</code> if release branch is not develop and {@link #mergeBackToDevelop()} was set to true.
	 */
	def boolean mergeBackToDevelop() {
		return !DEVELOP.equals(releaseBranch) && mergeToDevelop
	}

	/**
	 * @return the version stored after release preparations are done
	 */
	def String getNextVersion(String releaseVersion) {
		def newPatchLevel = releaseVersion.substring(releaseVersion.length() - 1).toInteger() + 1
		return releaseVersion.substring(0, releaseVersion.length() - 1) + newPatchLevel + ReleaseHelper.SNAPSHOT
	}
}