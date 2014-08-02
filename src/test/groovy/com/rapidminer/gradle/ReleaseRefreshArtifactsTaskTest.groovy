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

/**
 * An integration test specification for the {@link ReleaseRefreshArtifactsTest} class
 * and the 'releaseRefreshArtifacts' task.
 * 
 * @author Nils Woehler
 *
 */
class ReleaseRefreshArtifactsTaskTest extends AbstractReleaseTaskSpecification {

	def 'fail on no snapshot url defined'() {
		given:
		buildFile << '''
            apply plugin: 'java'
			apply plugin: 'maven-publish'

			version = "1.0.0-SNAPSHOT"
			
			task updateVersion << {
				version = "1.0.0"
			}

			publishing {
				publications {
					mavenJava(MavenPublication) {
						from components.java
					}
				}
				repositories {
					maven {
						url "http://artifactory.test.com/${->project.version.contains('-SNAPSHOT') ?  'libs-snapshot-local' : 'libs-release-local'}"
						credentials {
							username = "user"
							password = "password"
						}
					}
				}
			}

        '''.stripIndent()

		when:
		ExecutionResult result = runTasksWithFailure('updateVersion', 'releaseRefreshArtifacts')

		then:
		result.failure.cause.cause.message.contains('No snapshot repository URL defined.')
	}
	
	def 'fail on no release url defined'() {
		given:
		buildFile << '''
            apply plugin: 'java'
			apply plugin: 'maven-publish'

			version = "1.0.0-SNAPSHOT"
			
			task updateVersion << {
				version = "1.0.0"
			}

			release {
				snapshotRepositoryUrl 'http://artifactory.test.com/libs-snapshot-local'
			}

			publishing {
				publications {
					mavenJava(MavenPublication) {
						from components.java
					}
				}
				repositories {
					maven {
						url "http://artifactory.test.com/${->project.version.contains('-SNAPSHOT') ?  'libs-snapshot-local' : 'libs-release-local'}"
						credentials {
							username = "user"
							password = "password"
						}
					}
				}
			}

        '''.stripIndent()

		when:
		ExecutionResult result = runTasksWithFailure('updateVersion', 'releaseRefreshArtifacts')

		then:
		result.failure.cause.cause.message.contains('No release repository URL defined.')
	}
	
	def 'check skip up-to-date version artifacts'() {
		given:
		buildFile << '''
            apply plugin: 'java'
			apply plugin: 'maven-publish'

			version = "1.0.0-SNAPSHOT"

			release {
				snapshotRepositoryUrl 'http://artifactory.test.com/libs-snapshot-local'
				releaseRepositoryUrl 'http://artifactory.test.com/libs-release-local'
			}

			publishing {
				publications {
					mavenJava(MavenPublication) {
						from components.java
					}
				}
				repositories {
					maven {
						url "http://artifactory.test.com/${->project.version.contains('-SNAPSHOT') ?  'libs-snapshot-local' : 'libs-release-local'}"
						credentials {
							username = "user"
							password = "password"
						}
					}
					maven {
						url 'http://artifactory.another.test/'
					}
				}
			}

        '''.stripIndent()

		when:
		ExecutionResult result = runTasksSuccessfully('releaseRefreshArtifacts')

		then:
		result.standardOutput.contains('Version already up-to-date.')
		!result.standardOutput.contains('Refreshing artifacts of')
	}
	
	def 'check refresh dependencies'() {
		given:
		buildFile << '''
            apply plugin: 'java'
			apply plugin: 'maven-publish'

			version = "1.0.0-SNAPSHOT"

			release {
				snapshotRepositoryUrl 'http://artifactory.test.com/libs-snapshot-local'
				releaseRepositoryUrl 'http://artifactory.test.com/libs-release-local'
			}

			task updateVersion << {
				version = "1.0.0"
			}

			publishing {
				publications {
					mavenJava(MavenPublication) {
						from components.java
					}
				}
				repositories {
					maven {
						url "http://artifactory.test.com/libs-snapshot-local"
						credentials {
							username = "user"
							password = "password"
						}
					}
					maven {
						url 'http://artifactory.another.test/'
					}
				}
			}

        '''.stripIndent()

		when:
		ExecutionResult result = runTasksSuccessfully('updateVersion', 'releaseRefreshArtifacts')

		then:
		result.standardOutput.contains('Updating publication version from 1.0.0-SNAPSHOT to 1.0.0')
		result.standardOutput.contains("Skipping repository update for repositoy with URL 'http://artifactory.another.test/'")
		result.standardOutput.contains('Updating publish repository from http://artifactory.test.com/libs-snapshot-local to http://artifactory.test.com/libs-release-local')
	}
}
