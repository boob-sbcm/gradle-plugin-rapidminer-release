package com.rapidminer.gradle

import groovy.lang.Closure;

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * The release task is performed after all other release tasks (e.g. build, publish, etc.)
 * have finished. It tags the release with the current project version.
 *
 * @author Nils Woehler
 *
 */
class Release extends DefaultTask {

	def GitScmProvider scmProvider

	// Variables below will be defined by the conventionalMapping
	def Closure generateTagName
	def Closure generateTagMessage
	def	boolean pushToRemote
	def boolean createTag

	@TaskAction
	def tagRelease() {
		if(isCreateTag()){
			scmProvider.tag(getTagName(project.version), getTagMessage(project.version))
			if(isPushToRemote()) {
				scmProvider.push([scmProvider.currentBranch] as List, true)
			}
		}
	}

	/**
	 * @return the name of the tag that will be created
	 */
	def getTagName(String version){
		Closure closure = getGenerateTagName()
		closure.delegate = this
		closure(version)
	}

	/**
	 * @return the message of the tag that will be created
	 */
	def getTagMessage(String version){
		Closure closure = getGenerateTagMessage()
		closure.delegate = this
		closure(version)
	}
}