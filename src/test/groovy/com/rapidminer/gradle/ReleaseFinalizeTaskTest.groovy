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

import nebula.test.IntegrationSpec
import nebula.test.functional.ExecutionResult

import org.ajoberstar.grgit.Grgit
import org.gradle.api.GradleException

/**
 * An integration test specification for the {@link FinalizeRelease} class
 * and the 'releaseFinalize' task.
 * 
 * @author Nils Woehler
 *
 */
class ReleaseFinalizeTaskTest extends AbstractReleaseTaskSpecification {

	def 'check standalone run of finalize will fail'() {
		when:
		ExecutionResult result = runTasksWithFailure(ReleasePlugin.FINALIZE_TASK_NAME)

		then:
		result.failure.cause.cause.message == 'Unknown release branch. Release finalize cannot be run as standalone task!'
	}
	
	def 'check finalize without remote branch and release branch develop'() {
		given:
		buildFile << '''
			releaseFinalize {
				releaseBranch = 'develop'
			}
		'''
		
		when:
		ExecutionResult result = runTasks(ReleasePlugin.FINALIZE_TASK_NAME)

		then:
		result.standardOutput.contains 'Release was made from develop. No need to merge changes back.'
		propertiesFile.text.contains 'version=1.0.1-SNAPSHOT'
		scmProvider.currentBranch == 'develop'
	}
	
	def 'check finalize without remote branch and release branch release'() {
		given:
		repo.checkout(branch: 'release', createBranch: true)
		scmProvider.switchToBranch('master')
		
		buildFile << '''
			releaseFinalize {
				releaseBranch = 'release'
			}
		'''
		
		when:
		ExecutionResult result = runTasks(ReleasePlugin.FINALIZE_TASK_NAME)

		then:
		result.standardOutput.contains 'Switched to branch: release'
		result.standardOutput.contains 'Release branch wasn\'t \'develop\' branch. Merging release branch to develop.'
		result.standardOutput.contains 'Merged \'release\' into current branch.'
		result.standardOutput.contains 'Deleting release branches [release].'
		propertiesFile.text.contains 'version=1.0.1-SNAPSHOT'
		scmProvider.currentBranch == 'develop'
	}
	
	def 'check finalize without remote branch and release branch release without merge to develop'() {
		given:
		repo.checkout(branch: 'release', createBranch: true)
		scmProvider.switchToBranch('master')
		
		buildFile << '''
			releaseFinalize {
				releaseBranch = 'release'
			}

			release {
				mergeToDevelop = false
			}
		'''
		
		when:
		ExecutionResult result = runTasks(ReleasePlugin.FINALIZE_TASK_NAME)

		then:
		result.standardOutput.contains 'Switched to branch: release'
		result.standardOutput.contains 'Release branch was not develop. Check if changes should be merged back to develop.'
		result.standardOutput.contains 'Merging changes from \'release\' back to develop is disabled.'
		propertiesFile.text.contains 'version=1.0.1-SNAPSHOT'
		scmProvider.currentBranch == 'release'
	}

	def 'check finalize without remote branch and do not delete release branch'() {
		given:
		repo.checkout(branch: 'release', createBranch: true)
		scmProvider.switchToBranch('master')
		
		buildFile << '''
			releaseFinalize {
				releaseBranch = 'release'
			}

			release {
				deleteReleaseBranch = false
			}
		'''
		
		when:
		ExecutionResult result = runTasks(ReleasePlugin.FINALIZE_TASK_NAME)

		then:
		result.standardOutput.contains 'Switched to branch: release'
		result.standardOutput.contains 'Release branch wasn\'t \'develop\' branch. Merging release branch to develop.'
		result.standardOutput.contains 'Merging changes from \'release\' branch to \'develop\' branch.'
		result.standardOutput.contains 'Skipping deletion of release branch as it is disabled.'
		propertiesFile.text.contains 'version=1.0.1-SNAPSHOT'
		scmProvider.currentBranch == 'develop'
	}

}
