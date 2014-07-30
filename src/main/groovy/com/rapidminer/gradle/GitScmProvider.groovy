package com.rapidminer.gradle

import org.ajoberstar.grgit.Grgit
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
		return grgit.branch.current.trackingBranch
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
		grgitCommand("Pushed ${refs} to remote repository '${remote}'") {
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
		repo.branch.remove(names: toRemove)
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
	def ensureNoTag() {
		//TODO
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
}