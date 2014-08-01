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

import nebula.test.ProjectSpec

import org.ajoberstar.grgit.Grgit
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

import spock.lang.Specification

import com.energizedwork.spock.extensions.TempDirectory

/**
 * An abstract test specification for all a Git backed project.
 * 
 * @author Nils Woehler
 *
 */
abstract class AbstractGitSpecification extends ProjectSpec {

	protected static final String FILE_1 = '1.txt'
	protected static final String FILE_2 = '2.txt'
	protected static final String BRANCH_1 = 'test-branch-1'
	protected static final String BRANCH_2 = 'test-branch-2'

	protected enum RepoType {
		LOCAL,
		REMOTE
	}

	Grgit grgitLocal

	@TempDirectory(baseDir='target/test/tmp/', clean=true)
	File remoteRepoDir
	Grgit grgitRemote

	/*
	 * Use Spock's setup() hook to initialize the properties for each test.
	 */
	def setup() {
		// Create the remote repository
		grgitRemote = Grgit.init(dir: remoteRepoDir)
		addContent(RepoType.REMOTE, FILE_1)
		grgitRemote.add(patterns: [FILE_1])
		grgitRemote.commit(message: 'Initial commit')

		// Create local repository by cloning the remote repository
		grgitLocal = Grgit.clone(dir: projectDir, uri: remoteRepoDir)
	}

	/*
	 * Use Spock's cleanup() hock to close Grgit instances 
	 */
	def cleanup() {
		grgitRemote.close()
		projectDir.deleteOnExit()
		projectDir.deleteDir()
	}

	protected void addContent(RepoType type, String fileName) {
		String path = remoteRepoDir.absolutePath
		if(type == RepoType.LOCAL) {
			path = projectDir.absolutePath
		}
		new File(path, fileName) << UUID.randomUUID().toString() + File.separator
	}

	protected void addContent(RepoType type) {
		addContent(type, FILE_1)
	}

	protected void commit(RepoType type, String fileName) {
		addContent(type, fileName)
		def Grgit grgit = grgitRemote
		if(type == RepoType.LOCAL) {
			grgit = grgitLocal
		}
		grgit.add(patterns: [fileName])
		grgit.commit(message: 'do')
	}

	protected void commit(RepoType type) {
		commit(type, FILE_1)
	}
}
