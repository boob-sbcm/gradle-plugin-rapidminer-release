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
import org.ajoberstar.grgit.*

/**
 * The finalize release task is called if 'releasePrepare' has been called before.
 * This means we are performing a local release where the release branch changes will be merged
 * back to develop.
 *
 * @author Nils Woehler
 *
 */
class ReleaseFinalize extends DefaultTask {

	protected static final String DEVELOP = "develop"

	def GitScmProvider scmProvider
	def String releaseBranch

	// Variables below will be defined by the conventionalMapping
	def String remote
	def boolean mergeToDevelop
	def	boolean pushToRemote
	def	boolean deleteReleaseBranch
	def boolean pushTags
	def boolean createTag

	@TaskAction
	def finalizeRelease() {

		if(!releaseBranch) {
			throw new GradleException('Unknown release branch.'+
			' Release finalize cannot be run as standalone task!')
		}

		/*
		 * 1. Switch back to release branch
		 */
		scmProvider.switchToBranch(releaseBranch)

		// remember the remote release branch
		def trackingReleaseBranch = scmProvider.currentTrackingBranch
		if(trackingReleaseBranch) {
			logger.info("Release branch is tracking branch ${trackingReleaseBranch.fullName}")
		}

		/*
		 * 2. Increase version in 'gradle.properties' to new version 
		 */
		def gradleProperties = ReleaseHelper.getGradleProperties(project)
		logger.info "Adapting version to next development cycle and committing 'gradle.properties'."
		gradleProperties.version = getNextVersion(gradleProperties.version)
		gradleProperties.store(ReleaseHelper.getGradlePropertiesFile(project).newWriter(), null)

		/*
		 * 3. Add and commit changed gradle.properties
		 */
		scmProvider.commit "Prepare gradle.properties for next development cycle (${gradleProperties.version})", [
			ReleaseHelper.GRADLE_PROPERTIES
		]

		// A list of all branches that will be pushed to remote
		def toPush = []

		// If release branch is not develop ...
		if(!isReleaseDevelop()) {

			logger.info 'Release branch was not develop. Check if changes should be merged back to develop.'

			// ... check if changes should be merged back to develop
			if(isMergeToDevelop()) {
				logger.info "Release branch wasn't 'develop' branch. Merging release branch to develop."

				logger.info "Switching to 'develop' branch."
				scmProvider.switchToBranch(DEVELOP)

				logger.info "Merging changes from '${releaseBranch}' branch to 'develop' branch."
				scmProvider.merge(releaseBranch)
				
				// Ensure that changes on develop will be pushed (if enabled)
				if(scmProvider.currentTrackingBranch) {
					toPush << DEVELOP
				}
				
				// Check if release branch should be deleted
				if(isDeleteReleaseBranch()) {
					def toRemove = [releaseBranch]
					// also delete branch on remote if available
					if(trackingReleaseBranch) {
						toRemove << trackingReleaseBranch.fullName
					}
					logger.info "Deleting release branches ${toRemove}."
					scmProvider.remove(toRemove)
				} else {
					logger.info 'Skipping deletion of release branch as it is disabled.'
				}

			} else {
				logger.info "Merging changes from '${releaseBranch}' back to develop is disabled."
				// Otherwise push it to the configured remote repository
				if(trackingReleaseBranch) {
					toPush << releaseBranch
				}
			}
	
		} else {
			logger.info 'Release was made from develop. No need to merge changes back.'
			if(scmProvider.currentTrackingBranch) {
				toPush << DEVELOP
			}
		}

		/*
		 * Push changes to remote if any available
		 */
		if(isPushToRemote() && toPush) {
			logger.info("Pushing following branches to remote: ${toPush}")
			scmProvider.push(toPush, false)
		} 

	}

	/**
	 * @return checks whether the release branch was develop
	 */
	def boolean isReleaseDevelop() {
		return DEVELOP.equals(releaseBranch)
	}

	/**
	 * @return the version stored after release preparations are done
	 */
	def String getNextVersion(String releaseVersion) {
		def newPatchLevel = releaseVersion.substring(releaseVersion.length() - 1).toInteger() + 1
		return releaseVersion.substring(0, releaseVersion.length() - 1) + newPatchLevel + ReleaseHelper.SNAPSHOT
	}
}