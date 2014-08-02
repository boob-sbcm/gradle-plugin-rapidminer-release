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
import org.gradle.testfixtures.ProjectBuilder

import sun.net.httpserver.WriteFinishedEvent

/**
 * An integration test specification for the {@link ReleaseCheck} class
 * and the 'releaseCheck' task.
 * 
 * @author Nils Woehler
 *
 */
class ReleaseHelperTest extends AbstractGitSpecification {

	def 'test getGradlePropertiesFile()'() {
		given:
		def gradPropFile = project.file(ReleaseHelper.GRADLE_PROPERTIES)
		gradPropFile << '''
				version=1.0.0
		'''.stripIndent()

		expect:
		ReleaseHelper.getGradlePropertiesFile(project) == gradPropFile
	}

	def 'test getGradlePropertiesFile() multiProject'() {
		given:
		def subProjectDir = project.file('sub/')
		subProjectDir.mkdirs()
		def subproject = ProjectBuilder.builder().withParent(project).withProjectDir(subProjectDir).build()
		def gradPropFile = project.file(ReleaseHelper.GRADLE_PROPERTIES)
		gradPropFile << '''
				version=1.0.0
		'''.stripIndent()

		expect:
		ReleaseHelper.getGradlePropertiesFile(subproject) == gradPropFile
	}

	def 'test getGradleProperties()'() {
		given:
		def gradPropFile = project.file(ReleaseHelper.GRADLE_PROPERTIES)
		gradPropFile << '''
				version=1.0.0
		'''.stripIndent()

		expect:
		ReleaseHelper.getGradleProperties(project).version == '1.0.0'
	}

	def 'test println() to System.out'() {
		given:
		def File file  = project.file('output')
		if(file.exists()) {
			file.delete()
		}
		def PrintStream printStream = new PrintStream(new FileOutputStream(file))
		System.setOut(printStream)
		
		when:
		ReleaseHelper.println('test')
		
		then:
		file.text == 'test\n'
	}
	
	def 'test readLine() from System.in'() {
		given:
		ByteArrayInputStream input = new ByteArrayInputStream('test'.bytes)
		System.setIn input
		
		expect:
		ReleaseHelper.readLine('Please give input:') == 'test'
	}
	
	def 'test default value for readLine() from System.in'() {
		given:
		ByteArrayInputStream input = new ByteArrayInputStream(''.bytes)
		System.setIn input
		
		expect:
		ReleaseHelper.readLine('Please give input:', 'all okay') == 'all okay'
	}
	
	def 'test execClosure'() {
		given:
		def Closure closure = { version ->
			return version
		}
		
		expect:
		ReleaseHelper.execClosure('1.0.000', closure) == '1.0.000'
	}
}
