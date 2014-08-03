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
import org.gradle.api.logging.LogLevel
import org.gradle.testfixtures.ProjectBuilder

/**
 * An abstract integration test specification for all tasks of the
 * release plugin.
 * 
 * @author Nils Woehler
 *
 */
abstract class AbstractReleaseTaskSpecification extends IntegrationSpec {

	Grgit repo
	GitScmProvider scmProvider
	File propertiesFile

	/*
	 * Use Spock's setup() hook to initialize a Git repository for each test.
	 */
	def setup() {
		// Initialize Git repository
		repo = Grgit.init(dir: projectDir)

		buildFile << "apply plugin: 'rapidminer-release'"
		logLevel = LogLevel.INFO

		scmProvider = new GitScmProvider(projectDir,
				ProjectBuilder.builder().build().logger, new ReleaseExtension())
		def gitignore = createFile('.gitignore')
		gitignore << '''
			settings.gradle
			.gradle-test-kit/
		'''
		propertiesFile = createFile('gradle.properties')
		propertiesFile << '''
			version = 1.0.0
		'''
		repo.add(patterns: [
			buildFile.name,
			gitignore.name,
			propertiesFile.name
		])
		repo.commit(message: 'Initial commit')
		repo.checkout(branch: 'develop', createBranch: true)
		repo.checkout(branch: 'master', createBranch: false)
	}

	def cleanup() {
		repo.close()
		assert(projectDir.deleteDir())
	}
}
