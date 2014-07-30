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

import groovy.lang.Closure;

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
	def Closure generateTagName
	def String masterBranch

	@TaskAction
	def performChecks() {
		
		// Check if current branch is the defined master branch
		if(!scmProvider.currentBranch.equals(getMasterBranch())) {
			throw new GradleException("Release task was not executed on defined master branch '${getMasterBranch()}' but on '${scmProvider.currentBranch}'")
		}
		
		// Check for upstream changes
		scmProvider.ensureNoUpstreamChanges()
		
		// Check for uncommitted changes
		scmProvider.ensureNoUncommittedChanges()
		
		// Ensure the current commit isn't already a tag
		scmProvider.ensureNoTag(ReleaseHelper.execClosure(project.version, getGenerateTagName()))
	}
}