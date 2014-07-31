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

/**
 * @author Nils Woehler
 */
class ReleaseExtension {

	/**
	 * Specifies whether to skip the check for illegal release dependencies. Defaults to 'false'.
	 */
	boolean skipIllegalDependenciesCheck = false

	/**
	 * The remote to fetch from and push to. Defaults to 'origin'.
	 */
	String remote = 'origin'

	/**
	 * Tasks that should be executed after releasePrepare has finished (e.g. building of release Jars, upload to Maven repository, etc.).
	 * Defaults to an empty list.
	 */
	Iterable releaseTasks = []

	/**
	 * Tasks that should be executed before releasePrepare is started (e.g. code checks, unit tests, etc.).
	 * Defaults to an empty list.
	 */
	Iterable preparationTasks = []

	/**
	 * The branch from which releases are created. Default is 'master'.
	 */
	String masterBranch = 'master'

	/**
	 * Specifies whether to create a tag.
	 */
	boolean createTag = true

	/**
	 * Closure to generate the tag name used when tagging releases.
	 * Is passed {@link #version} after it is inferred. Should return
	 * a string. Defaults to "${version}".
	 */
	Closure generateTagName = { version -> "${version}" }

	/**
	 * Closure to generate the message used when tagging releases.
	 * Is passed {@link #version} after it is inferred. Should return
	 * a string. Defaults to "Release of version ${version}".
	 */
	Closure generateTagMessage = { version -> "Release of version ${version}" }

	/**
	 * In case the release branch is not the 'develop' branch, changes will also be merged to develop.
	 * If set to 'true' the release task will end on branch develop. Otherwise it will end on the release branch.
	 * Defaults to 'true'.
	 */
	boolean mergeToDevelop = true

	/**
	 * Specifies whether to push all changes to the specified remote repository. Defaults to 'true'.
	 */
	boolean pushToRemote = true

	/**
	 * Specifies whether to delete release branch after merging changes to master branch.
	 * The deletion will only be performed if the release branch isn't develop itself and if {@link #mergeToDevelop} is set to 'true'.
	 * Defaults to 'true'.
	 */
	boolean deleteReleaseBranch = true
	
	/**
	 * TODO do not compile URL into extension
	 */
	String releaseRepositoryUrl
	
	/**
	 * TODO do not compile URL into extension
	 */
	String snapshotRepositoryUrl
}
