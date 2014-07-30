package com.rapidminer.gradle

import org.ajoberstar.grgit.*
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

/**
 * The release check task performs final checks like checking if release is made from correct branch,
 * no upstream changes available, etc.
 * 
 * @author Nils Woehler
 *
 */
class ReleaseCheck extends DefaultTask {

	def GitScmProvider scmProvider
	
	// Variables below will be defined by the conventionalMapping
	def String masterBranch

	@TaskAction
	def performChecks() {
		
		// Check if current branch is the defined master branch
		if(scmProvider.currentBranch.equals(getMasterBranch())) {
			throw new GradleException("Release task was not executed on defined master branch '${getMasterBranch}' but on ${scmProvider.currentBranch}")
		}
		
		// Check for upstream changes
		scmProvider.ensureNoUpstreamChanges()
		
		// Check for uncommitted changes
		scmProvider.ensureNoUncommittedChanges()
		
		// Ensure the current commit isn't already a tag
		scmProvider.ensureNoTag()
	}
}