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
import org.ajoberstar.grgit.Grgit
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

/**
 * A test specification for the {@link ReleasePlugin} class.
 * 
 * @author Nils Woehler
 *
 */
class ReleasePluginIntegrationTest extends IntegrationSpec {

	//TODO finalize task check can only be done if project is executed
//	def 'check release depends not on releasePrepare and releaseRefreshArtifacts if disabled'() {
//		given:
//		project.ext { prepareRelease = false }
//		project.apply plugin: ReleasePlugin
//		Task releaseTask = project.tasks.findByPath(ReleasePlugin.RELEASE_TASK_NAME)
//		Task check = project.tasks.findByPath(ReleasePlugin.RELEASE_CHECK_TASK_NAME)
//		Task finalize = project.tasks.findByPath(ReleasePlugin.FINALIZE_TASK_NAME)
//		
//		when:
//		project.evaluate()
//		
//		then:
//		releaseTask.getDependsOn().contains(check)
//		// Finalize task should only be run if prepare task has run before
//		// --> If prepare task is disabled, finalize should also be disabled
//		!releaseTask.getDependsOn().contains(finalize)
//		!releaseTask.getDependsOn().contains(ReleasePlugin.PREPARE_RELEASE_TASK_NAME)
//		!releaseTask.getDependsOn().contains(ReleasePlugin.REFRESH_ARTIFACTS_TASK_NAME)
//	}
//	
//	def 'check release depends not on releaseFinalize if disabled'() {
//		given:
//		project.ext { finalizeRelease = false }
//		project.apply plugin: ReleasePlugin
//		Task releaseTask = project.tasks.findByPath(ReleasePlugin.RELEASE_TASK_NAME)
//		Task check = project.tasks.findByPath(ReleasePlugin.RELEASE_CHECK_TASK_NAME)
//		Task finalize = project.tasks.findByPath(ReleasePlugin.FINALIZE_TASK_NAME)
//		
//		when:
//		project.evaluate()
//		
//		then:
//		releaseTask.getDependsOn().contains(check)
//		!releaseTask.getDependsOn().contains(finalize)
//		releaseTask.getDependsOn().contains(ReleasePlugin.PREPARE_RELEASE_TASK_NAME)
//		releaseTask.getDependsOn().contains(ReleasePlugin.REFRESH_ARTIFACTS_TASK_NAME)
//	}
}
