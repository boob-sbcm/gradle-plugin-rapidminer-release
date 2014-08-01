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

/**
 * An abstract integration test specification for all tasks of the
 * release plugin.
 * 
 * @author Nils Woehler
 *
 */
abstract class AbstractReleaseTaskSpecification extends IntegrationSpec {
	
	/*
	 * Use Spock's setup() hook to initialize a Git repository for each test.
	 */
	def setup() {
		// Initialize Git repository
		Grgit grgit = Grgit.init(dir: projectDir)
		grgit.close()
		
		buildFile << "apply plugin: 'rapidminer-release'"
		logLevel = LogLevel.INFO
	}

}
