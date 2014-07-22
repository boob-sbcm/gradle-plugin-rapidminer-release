package com.rapidminer.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.OutputFile
import org.ajoberstar.grgit.*

/**
 *
 * @author Nils Woehler
 *
 */
class PrepareReleaseTask extends DefaultTask {

	def Grgit grgit
	def String releaseBranch

	// Variables below will be defined by the conventionalMapping
	def String remote
	def String masterBranch
	def boolean createTag
	def Closure generateTagName
	def Closure generateTagMessage

	@TaskAction
	def prepareRelease() {

		if(releaseBranch.equals(getMasterBranch())){
			throw new RuntimeException("Cannot prepare release. We are on master branch!.")
		}
		
		/*
		 * 1. Ensuring all changes in the repository have been committed.
		 */
		ensureNoUncommitedChanges()

		/*
		 * 2. Ensuring there aren't any commits in the upstream branch that haven't been merged yet.
		 */
		ensureNoUpstreamChanges()

		/*
		 * 3. Look for 'gradle.properties' in root project and load properties
		 */
		def gradleProperties = ReleaseHelper.getGradleProperties(project)
		if(!gradleProperties.version){
			throw new RuntimeException("Could not find 'version' property in root project's gradle.properties file.")
		}

		/*
		 * 4. Ask user for release version number 
		 */

		def console = System.console()
		if(!console){
			throw new RuntimeException("Cannot get console.")
		}
		def releaseVersion = askForReleaseVersion(gradleProperties.version, console)
		logger.info("Current branch is: ${releaseBranch}")
		verifyReleaseInput(console, releaseBranch, releaseVersion)
		
		/*
		 * 5. Change gradle.properties to release version
		 */
		logger.info("Changing 'gradle.properties' to new version '${releaseVersion}'.")
		gradleProperties.version = releaseVersion
		gradleProperties.store(ReleaseHelper.getGradlePropertiesFile(project).newWriter(), null)

		grgit.add(patterns : [
			ReleaseHelper.GRADLE_PROPERTIES
		])

		grgit.commit(message: "Prepare gradle.properties for release of version ${releaseVersion}")

		/*
		 * 6. Switch to '${masterBranch}'
		 */
		logger.info("Switching to '${getMasterBranch()}' branch.")
		grgit.checkout(branch: getMasterBranch())

		/*
		 * 7. Ensuring there aren't any commits in the upstream branch that haven't been merged yet.
		 */
		ensureNoUpstreamChanges()

		/*
		 * 8. Merge release branch into 'master'
		 */
		logger.info("Merging '${releaseBranch}' to '${getMasterBranch()}' branch.")
		grgit.merge(head: releaseBranch)

		/*
		 * 9. Tag master with new version
		 */
		if(isCreateTag()){
			logger.info("Creating tag '" + getTagName(releaseVersion) + "' on '${getMasterBranch()}' branch.")
			grgit.tag.add(name: getTagName(releaseVersion), message: getTagMessage(releaseVersion))
		}
	}

	/**
	 * Asks the user for the next version to be released. By default it will be the version 
	 * defined in gradle.properties without the SNAPSHOT tag. But the user is also able to 
	 * change the version (e.g. for hotfix releases).
	 * 
	 * @param currentVersion the current version written in gradle.properties
	 * @return the version that will be released
	 */
	def String askForReleaseVersion(currentVersion, console){
		def releaseVersion = currentVersion.replace(ReleaseHelper.SNAPSHOT, '')
		println "\nNext release version is: ${releaseVersion}"

		// ask for new version number
		def correctInput = false
		def iterations = 0
		while(!correctInput) {
			def changeVersion = console.readLine("\n> Is release version '${releaseVersion}' correct? [YES, no]: ")
			if(changeVersion){
				if(changeVersion.toLowerCase().startsWith("n")){
					while(!correctInput){
						releaseVersion = console.readLine('> Please enter new release version: ')

						// check whether specified version is a valid version number
						correctInput = (releaseVersion ==~ /[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}/)
						if(!correctInput){
							if(++iterations == 2){
								println "I'm getting made. Dont drive me crazy. It can't be that hard too hard to specify a version number :) ###"
							}
							println "Invalid release version number: '${releaseVersion}'. A version must contain a major version (up to three digits), a minor version (up to three digits), and a patch level (up to three digits) separated by dots (e.g. 1.0.003)."
						}
					}
				} else if(!changeVersion.toLowerCase().startsWith("y")){
					println "Undefined input. Please type 'yes' or 'no'!"
				} else {
					correctInput = true
				}
			} else {
				correctInput = true
			}
		}
		return releaseVersion
	}

	/**
	 * Lets the user verify that the release preparation configuration is correct.
	 * 
	 * @param releaseBranch the current branch
	 * @param releaseVersion the version to release
	 */
	def verifyReleaseInput(console, releaseBranch, releaseVersion) {

		println "Please verify release configuration"
		println "-----------------------------------------------------"
		println "Current branch: '${releaseBranch}'"
		println "Version to release: '${releaseVersion}'"
		println "Release actions:"
		println "* Merge '${releaseBranch}' to '${getMasterBranch()}'"
		if(isCreateTag()){
			println "* Create tag on '${getMasterBranch()}'"
			println " - Tag name: '" + getTagName(releaseVersion) + "'"
			println " - Tag message: '" + getTagMessage(releaseVersion) + "'"
		}
		println "Finalize release "
		println "-----------------------------------------------------"
		println ""

		def i = 0
		while(i++ <= 2){
			Random random = new Random()
			int a = random.nextInt(10) + 1
			int b = random.nextInt(10) + 1
			def correctResult = a + b
			def result = console.readLine("\nPlease acknowledge: ${a} + ${b} = ")
			if(result.toInteger() != correctResult) {
				println "Wrong result (${a} + ${b} = ${correctResult})! Please try again..."
			} else {
				println "Release preparation configuration acknowledged! Preparing release..."
				return
			}
		}
		throw new RuntimeException("Aborting after trying three times to acknowledge release preparation configuration.")
	}

	/**
	 * Checks whether there are uncommited changes in the Git repository.
	 */
	def ensureNoUncommitedChanges() {
		logger.info('Checking for uncommitted changes in repo.')
		def status = grgit.status()
		if (!status.clean) {
			println 'Repository has uncommitted changes:'
			(status.staged.allChanges + status.unstaged.allChanges).each { change -> println "\t${change}" }
			throw new IllegalStateException('Repository has uncommitted changes.')
		}
	}

	/**
	 * Fetch changes from remote ensure that current branch isn't behind remot branch afterwards.
	 */
	def ensureNoUpstreamChanges(){
		// Only check for upstream changes only if current branch is tracking a remote branch
		if(grgit.branch.current.trackingBranch){
			logger.info('Fetching changes from  (${getRemote()}).')
			grgit.fetch(remote: getRemote())

			logger.info('Verifying current branch is not behind remote.')
			def branchStatus = grgit.branch.status(branch: grgit.branch.current.fullName)
			if (branchStatus.behindCount > 0) {
				println "Current branch is behind by ${branchStatus.behindCount} commits. Cannot proceed."
				throw new IllegalStateException("Current branch is behind ${getRemote()}.")
			}
		} else {
			logger.info("No remote branch for ${grgit.branch.current.name}. Skipping check for upstream changes.")
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