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

import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Tag
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger

/**
 *
 * Utility class for performing Git SCM actions.
 *
 * @author Nils Woehler
 *
 */
class GitScmProvider {

	private final Grgit repo
	private final Logger logger
	private final ReleaseExtension ext

	protected GitScmProvider(File rootDirPath, Logger logger, ReleaseExtension ext) {
		this.repo = Grgit.open(rootDirPath)
		this.logger = logger
		this.ext = ext
	}

	/**
	 * @return the name of the current branch
	 */
	def String getCurrentBranch() {
		return repo.branch.current.name
	}

	/**
	 * @return the tracked remote branch of the current branch
	 */
	def getCurrentTrackingBranch() {
		return repo.branch.current.trackingBranch
	}

	/**
	 * 
	 * @param message
	 * @param files
	 * @return
	 */
	def commit(String message, List<File> files) {
		grgitCommand("Committed ${files}") {
			repo.add(patterns: files)
			repo.commit(message: message)
		}
	}

	/**
	 * 
	 * @param branch
	 * @return
	 */
	def switchToBranch(String branch) {
		grgitCommand("Switched to branch: ${branch}") {
			repo.checkout(branch: branch, createBranch: false)
		}
	}

	/**
	 * 
	 * @param branchToMerge
	 * @return
	 */
	def merge(String branchToMerge) {
		grgitCommand("Merged '${branchToMerge}' into current branch.") {
			repo.merge(head: branchToMerge)
		}
	}

	/**
	 * 
	 * @param remote
	 * @param refs
	 * @param tags
	 * @return
	 */
	def push(List<String> refs, boolean tags) {
		grgitCommand("Pushed ${refs} to remote repository '${ext.remote}'") {
			repo.push(remote: ext.remote, refsOrSpecs: refs, tags: tags)
		}
	}

	/**
	 * 
	 * @param tagname
	 * @return
	 */
	def tag(String tagname, String message = null) {
		grgitCommand("Created tag: ${tagname}") {
			repo.tag.add(name: tagname, message: message ?: "Creating ${tagname}", annotate: true)
		}
	}

	/**
	 * 
	 * @param toDelete
	 * @return
	 */
	def remove(toRemove) {
		grgitCommand("Removed branches: ${toRemove}") {
			repo.branch.remove(names: toRemove)
		}
	}

	/**
	 * Checks whether there are uncommitted changes in the Git repository.
	 */
	def ensureNoUncommittedChanges() {
		logger.info('Checking for uncommitted changes in Git repository.')
		if(!repo.status().clean) {
			throw new GradleException('Git repository has uncommitted changes.')
		}
	}

	/**
	 * Fetch changes from remote ensure that current branch isn't behind remote branch afterwards.
	 */
	def ensureNoUpstreamChanges(){
		// Only check for upstream changes only if current branch is tracking a remote branch
		if(repo.branch.current.trackingBranch){
			grgitCommand("Fetched changes from  '${ext.remote}'.") {
				repo.fetch(remote: ext.remote)
			}

			logger.info('Verifying current branch is not behind remote.')
			def branchStatus = repo.branch.status(branch: repo.branch.current.fullName)
			if (branchStatus.behindCount > 0) {
				throw new GradleException("Current branch is behind '${ext.remote}' by ${branchStatus.behindCount} commits. Cannot proceed.")
			}
		} else {
			logger.info("No remote branch for ${repo.branch.current.name}. Skipping check for upstream changes.")
		}
	}

	/**
	 * Ensures that the current commit is not already a tag.
	 */
	def ensureNoTag(String tagName) {
		def currentCommit = repo.log(maxCommits: 1)[0]

		// fetch all tags
		def reason = null
		def tag = repo.tag.list().find { Tag tag ->
			if(tag.commit.id.equals(currentCommit.id)) {
				reason = "Current commit is tag '${tag.name}'!"
				return true
			}
			if(tag.name.equals(tagName)) {
				reason ="Tag with name '${tagName}' already exists!"
				return true
			}
			return false
		}

		if(tag) {
			throw new GradleException("Cannot create new release. ${reason}")
		}

	}

	/**
	 * @param successMessage the message that should be logged in success
	 * @param command the command to be executed
	 */
	private final void grgitCommand(String successMessage, Closure command) {
		try {
			command.delegate = this
			command()
			logger.info(successMessage)
		} catch(ex) {
			throw new GradleException("Error executing Git command!", ex)
		}
	}
	
	/**
	 * Releases file handles of the current repository.
	 */
	public void close() {
		repo?.close()
	}
}