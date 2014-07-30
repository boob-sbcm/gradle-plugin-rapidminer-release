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

import org.ajoberstar.grgit.Grgit
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

import spock.lang.Specification

import com.energizedwork.spock.extensions.TempDirectory

/**
 * A test specification for the {@link GitScmProvider} class.
 * 
 * @author Nils Woehler
 *
 */
class TestGitScmProvider extends Specification {

	private static final String FILE_1 = '1.txt'
	private static final String FILE_2 = '2.txt'
	private static final String BRANCH_1 = 'test-branch-1'
	private static final String BRANCH_2 = 'test-branch-2'
	
	private enum Type {
		LOCAL,
		REMOTE
	}
	
	@TempDirectory(baseDir='target/test/tmp/', clean=true)
	File localRepoDir
	Grgit grgitLocal
	
	@TempDirectory(baseDir='target/test/tmp/', clean=true)
	File remoteRepoDir
	Grgit grgitRemote

	Project project
	ReleaseExtension ext
	GitScmProvider scmProvider

	/*
	 * Use Spock's setup() hook to initialize the properties for each test.
	 */
	def setup() {
		// Create the remote repository
		grgitRemote = Grgit.init(dir: remoteRepoDir)
		addContent(Type.REMOTE, FILE_1)
		grgitRemote.add(patterns: [FILE_1])
		grgitRemote.commit(message: 'Initial commit')
		
		// Create local repository by cloning the remote repository
		grgitLocal = Grgit.clone(dir: localRepoDir, uri: remoteRepoDir)
		
		project = ProjectBuilder.builder().build()
		ext = new ReleaseExtension()
		scmProvider = new GitScmProvider(localRepoDir, project.logger, ext)
	}

	def "switchToBranch: switched branch"() {
		given:
		commit(Type.LOCAL)
		grgitLocal.branch.add(name: BRANCH_1)
		commit(Type.LOCAL)
		commit(Type.LOCAL)
		grgitLocal.branch.add(name: BRANCH_2)
		
		when:
		scmProvider.switchToBranch(BRANCH_1)
		then:
		scmProvider.currentBranch == BRANCH_1
		
		when:
		scmProvider.switchToBranch(BRANCH_2)
		then:
		scmProvider.currentBranch == BRANCH_2
	}
	
	def "commit: all committed"() {
		given:
		commit(Type.LOCAL)
		commit(Type.LOCAL)
		addContent(Type.LOCAL)
		
		when:
		scmProvider.ensureNoUncommittedChanges()
		then: 
		GradleException e = thrown()
		
		when:
		scmProvider.commit('Committing random content', [FILE_1] as List)
		scmProvider.ensureNoUncommittedChanges()
		then:
		GradleException e1 = notThrown()
	}
	
	def "push: push changes"() {
		given:
		ext.remote = 'origin'
		commit(Type.LOCAL)
		def localContent =  new File(localRepoDir.absolutePath, FILE_1).text
		
		when:
		scmProvider.push(['master'] as List, false)
		
		then:
		localContent ==  new File(localRepoDir.absolutePath, FILE_1).text
	}
	
	def "merge: all merged"() {
		given:
		commit(Type.LOCAL)
		// Create two branches
		grgitLocal.branch.add(name: BRANCH_1)
		grgitLocal.branch.add(name: BRANCH_2)
		
		// Commit content to first branch
		scmProvider.switchToBranch(BRANCH_1)
		commit(Type.LOCAL, FILE_1)
		commit(Type.LOCAL, FILE_1)
		def contentBranch1 = new File(localRepoDir.absolutePath, FILE_1).text
		
		// commit content to second branch
		scmProvider.switchToBranch(BRANCH_2)
		commit(Type.LOCAL, FILE_2)
		commit(Type.LOCAL, FILE_2)
		def contentBranch2 = new File(localRepoDir.absolutePath, FILE_2).text
		
		
		when:
		scmProvider.merge(BRANCH_1)
		def mergeContentBranch1 = new File(localRepoDir.absolutePath, FILE_1).text
		def mergeContentBranch2 = new File(localRepoDir.absolutePath, FILE_2).text
		
		then:
		contentBranch1 == mergeContentBranch1
		contentBranch2 == mergeContentBranch2
	}
	
	
	def "ensureNoUncommittedChanges: changes found"() {
		given:
		commit(Type.LOCAL)
		addContent(Type.LOCAL)
		
		when:
		scmProvider.ensureNoUncommittedChanges()
		
		then:
		GradleException e = thrown()
		e.message == 'Git repository has uncommitted changes.'
	}
	
	def "ensureNoUncommittedChanges: all okay"() {
		given:
		commit(Type.LOCAL)
		
		when:
		scmProvider.ensureNoUncommittedChanges()
		
		then:
		GradleException e = notThrown()
	}
	
	def "ensureNoUpstreamChanges: no upstream changes"() {
		given:
		commit(Type.LOCAL)
		
		when:
		scmProvider.ensureNoUpstreamChanges()
		
		then:
		GradleException e = notThrown()
	}
	
	def "ensureNoUpstreamChanges: upstream changes"() {
		given:
		commit(Type.REMOTE)
		
		when:
		scmProvider.ensureNoUpstreamChanges()
		
		then:
		GradleException e = thrown()
	}

	def "ensureNoTag: tag already exist"() {
		given:
		commit(Type.LOCAL)
		grgitLocal.tag.add(name: '1.0.000')
		commit(Type.LOCAL)

		when:
		scmProvider.ensureNoTag('1.0.000')

		then:
		GradleException e = thrown()
		e.message.contains('Tag with name')
	}

	def "ensureNoTag: commit is tag"() {
		given:
		commit(Type.LOCAL)
		grgitLocal.tag.add(name: '1.0.000')

		when:
		scmProvider.ensureNoTag('1.0.000')

		then:
		GradleException e = thrown()
		e.message.contains('Current commit is tag')
	}

	def "ensureNoTag: all okay"() {
		given:
		commit(Type.LOCAL)
		grgitLocal.tag.add(name: '1.0.000')
		commit(Type.LOCAL)

		when:
		scmProvider.ensureNoTag('1.0.001')

		then:
		GradleException e = notThrown()
	}

	private void addContent(Type type, String fileName) {
		String path = remoteRepoDir.absolutePath
		if(type == Type.LOCAL) {
			path = localRepoDir.absolutePath
		}
		new File(path, fileName) << UUID.randomUUID().toString() + File.separator
	}
	
	private void addContent(Type type) {
		addContent(type, FILE_1)
	}
	
	private void commit(Type type, String fileName) {
		addContent(type, fileName)
		def Grgit grgit = grgitRemote
		if(type == Type.LOCAL) {
			grgit = grgitLocal
		}
		grgit.add(patterns: [fileName])
		grgit.commit(message: 'do')
	}
	
	private void commit(Type type) {
		commit(type, FILE_1)
	}
	
}
