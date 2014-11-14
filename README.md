## Introduction

The 'com.rapidminer.release' plugin is designed to ease the process of creating a project release that is managed via Git.
It assumes a that the 'gradle.properties' file in the root project of the applied project contains the current
project's version number.

When invoking the task _release_ with default configuration the following action will be executed:
* Check for illegal dependencies
* Execute specified preparation tasks
* Ask user for release version
* Adapt 'gradle.properties' to contain the release version
* Merge changes to master branch
* Tag master branch
* Execute specified release tasks
* Increase version on release branch
* Merge release branch back to develop (if release branch isn't develop itself)
* Delete release branch (if release branch isn't develop itself)
* Push all changes to 'origin' remote repository

A more detailed description is given below (see 'Added Tasks')

## How to use (requires Gradle 2.1+)
	plugins {
		id 'com.rapidminer.release' version «plugin version»
	}
	 
	release {
	 
		/*
		 * Specifies whether to skip the check for illegal release dependencies. Defaults to 'false'.
		 */
		skipIllegalDependenciesCheck = false
		
		/**
		 * The remote to fetch from and push to. Defaults to 'origin'.
		 */
		remote = 'origin'
	
		/**
		 * Tasks that should be executed before releasePrepare is started (e.g. code checks, unit tests, etc.).
		 * Defaults to an empty list.
		 */
		 preparationTasks << check
	
		/**
		 * Tasks that should be executed after releasePrepare has finished (e.g. building of release Jars, upload to Maven repository, etc.).
		 * Defaults to an empty list.
		 */
		releaseTasks = [clean, jar, publish]
		
		/**
		 * The branch from which releases are created. Default is 'master'.
		 */
		masterBranch = 'master'
	
		/*
		 * Specifies whether to create a tag.
		 */
		createTag = true
	
		/*
		 * Closure to generate the tag name used when tagging releases.
		 * Is passed {@link #version} after it is inferred. Should return
		 * a string. Defaults to "${version}".
		 */
		generateTagName = { version -> "${version}" }
	
		/*
		 * Closure to generate the message used when tagging releases.
		 * Is passed {@link #version} after it is inferred. Should return
		 * a string. Defaults to "Release of version ${version}".
		 */
		generateTagMessage = { version -> "Release of version ${version}" }
	
		/*
		 * In case the release branch is not the 'develop' branch, changes will also be merged to develop. 
		 * If set to 'true' the release task will end on branch develop. Otherwise it will end on the release branch.
		 * Defaults to 'true'.
		 */
		mergeToDevelop = true
	
		/*
		 * Specifies whether to push all changes to the specified remote repository. Defaults to 'true'.
		 */
		pushChangesToRemote = true
	
		/*
		 * Specifies whether to delete release branch after merging changes to master branch. 
		 * The deletion will only be performed if the release branch isn't develop itself and if {@link #mergeToDevelop} is set to 'true'.
		 * Defaults to 'true'.
		 */
		deleteReleaseBranch = true
	}
	
## Applied Plugins
_None_

## Added Tasks

##### releaseCheckDependencies
Checks whether any configuration depends on an illegal dependency (currently: dependencies with  SNAPSHOT and RC versions).
The build fails if an illegal dependency is found. The task will be executed before _releasePrepare_ unless _skipIllegalDependenciesCheck_ is set to true.

##### releasePrepare
This task will prepare the release. By default it will ensure that the current branch (aka the release branch) does not contain
uncommitted changes and has no upstream changes. Then it will load the version from 'gradle.properties' of the root project.
Next the user has to confirm the release configuration (version number, release branch, target branch, etc.).
If the user has confirmed, the task will change, add and commit the 'gradle.properties' with the release version number, 
then it will switch to the target branch and merge the release branch into the target branch if it does not have any upstream changes. 
Finally it will create a tag on the target branch. 

##### release
The task itself does no work at all. It depends on _releasePrepare_ and is finalized by _releaseFinalize_.
Furthermore it depends on all tasks defined by _releaseTasks_.

##### releaseFinalize
The task is called as finalizer of _release_. By default it will switch back to the release branch where it will increase the version by one patch level (e.g. release version: 1.0.0, next version: 1.0.1-SNAPSHOT).
Afterwards it will switch back to develop and merge to release branch into develop (only if release hasn't been made from develop of course).
Then it will delete the release branch (if release wasn't made from develop) and push all changes to the defined remote repository.