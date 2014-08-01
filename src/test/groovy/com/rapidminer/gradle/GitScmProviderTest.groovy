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

import com.rapidminer.gradle.AbstractGitSpecification.RepoType

/**
 * A test specification for the {@link GitScmProvider} class.
 * 
 * @author Nils Woehler
 *
 */
class GitScmProviderTest extends AbstractGitSpecification {

	Project project
	ReleaseExtension ext
	GitScmProvider scmProvider

	/*
	 * Use Spock's setup() hook to initialize the properties for each test.
	 */
	def setup() {
		project = ProjectBuilder.builder().build()
		ext = new ReleaseExtension()
		scmProvider = new GitScmProvider(projectDir, project.logger, ext)
	}
	
	def cleanup() {
		scmProvider.close()
	}

	def 'switchToBranch: switched branch'() {
		given:
		commit(RepoType.LOCAL)
		grgitLocal.branch.add(name: BRANCH_1)
		commit(RepoType.LOCAL)
		commit(RepoType.LOCAL)
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
	
	def 'commit: all committed'() {
		given:
		commit(RepoType.LOCAL)
		commit(RepoType.LOCAL)
		addContent(RepoType.LOCAL)
		
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
	
	def 'push: push changes'() {
		given:
		ext.remote = 'origin'
		commit(RepoType.LOCAL)
		def localContent =  new File(projectDir.absolutePath, FILE_1).text
		
		when:
		scmProvider.push(['master'] as List, false)
		
		then:
		localContent ==  new File(projectDir.absolutePath, FILE_1).text
	}
	
	def 'merge: all merged'() {
		given:
		commit(RepoType.LOCAL)
		// Create two branches
		grgitLocal.branch.add(name: BRANCH_1)
		grgitLocal.branch.add(name: BRANCH_2)
		
		// Commit content to first branch
		scmProvider.switchToBranch(BRANCH_1)
		commit(RepoType.LOCAL, FILE_1)
		commit(RepoType.LOCAL, FILE_1)
		def contentBranch1 = new File(projectDir.absolutePath, FILE_1).text
		
		// commit content to second branch
		scmProvider.switchToBranch(BRANCH_2)
		commit(RepoType.LOCAL, FILE_2)
		commit(RepoType.LOCAL, FILE_2)
		def contentBranch2 = new File(projectDir.absolutePath, FILE_2).text
		
		
		when:
		scmProvider.merge(BRANCH_1)
		def mergeContentBranch1 = new File(projectDir.absolutePath, FILE_1).text
		def mergeContentBranch2 = new File(projectDir.absolutePath, FILE_2).text
		
		then:
		contentBranch1 == mergeContentBranch1
		contentBranch2 == mergeContentBranch2
	}
	
	
	def 'ensureNoUncommittedChanges: changes found'() {
		given:
		commit(RepoType.LOCAL)
		addContent(RepoType.LOCAL)
		
		when:
		scmProvider.ensureNoUncommittedChanges()
		
		then:
		GradleException e = thrown()
		e.message == 'Git repository has uncommitted changes.'
	}
	
	def 'ensureNoUncommittedChanges: all okay'() {
		given:
		commit(RepoType.LOCAL)
		
		when:
		scmProvider.ensureNoUncommittedChanges()
		
		then:
		GradleException e = notThrown()
	}
	
	def 'ensureNoUpstreamChanges: no upstream changes'() {
		given:
		commit(RepoType.LOCAL)
		
		when:
		scmProvider.ensureNoUpstreamChanges()
		
		then:
		GradleException e = notThrown()
	}
	
	def 'ensureNoUpstreamChanges: upstream changes'() {
		given:
		commit(RepoType.REMOTE)
		
		when:
		scmProvider.ensureNoUpstreamChanges()
		
		then:
		GradleException e = thrown()
	}

	def 'ensureNoTag: tag already exist'() {
		given:
		commit(RepoType.LOCAL)
		grgitLocal.tag.add(name: '1.0.000')
		commit(RepoType.LOCAL)

		when:
		scmProvider.ensureNoTag('1.0.000')

		then:
		GradleException e = thrown()
		e.message.contains('Tag with name')
	}

	def 'ensureNoTag: commit is tag'() {
		given:
		commit(RepoType.LOCAL)
		grgitLocal.tag.add(name: '1.0.000')

		when:
		scmProvider.ensureNoTag('1.0.000')

		then:
		GradleException e = thrown()
		e.message.contains('Current commit is tag')
	}

	def 'ensureNoTag: all okay'() {
		given:
		commit(RepoType.LOCAL)
		grgitLocal.tag.add(name: '1.0.000')
		commit(RepoType.LOCAL)

		when:
		scmProvider.ensureNoTag('1.0.001')

		then:
		GradleException e = notThrown()
	}

}
