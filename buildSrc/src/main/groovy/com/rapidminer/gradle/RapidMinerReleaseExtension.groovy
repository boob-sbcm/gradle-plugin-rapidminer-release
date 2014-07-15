package com.rapidminer.gradle

/**
 * 
 * @author nwoehler
 *
 */
class RapidMinerReleaseExtension {

	/**
	 * Specifies whether to skip the check for illegal release dependencies. Defaults to 'false'.
	 */
	boolean skipIllegalDependenciesCheck = false
	
	/**
	 * The remote to fetch from and push to. Defaults to {@code origin}.
	 */
	String remote = 'origin'

	/**
	 * Tasks that should be executed before the release is tagged, branched, and
	 * pushed to the remote. Defaults to an empty list.
	 */
	Iterable releaseTasks = []

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
	boolean pushChangesToRemote = true

	/**
	 * Specifies whether to delete release branch after merging changes to master branch. 
	 * The deletion will only be performed if the release branch isn't develop itself and if {@link #mergeToDevelop} is set to 'true'.
	 * Defaults to 'true'.
	 */
	boolean deleteReleaseBranch = true
}
