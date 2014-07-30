## Change Log

#### 0.2.0
* Changed release tasks to mustRunAfter instead of dependsOn releasePreparation task
* Change project version when finalizing releasePreparation task to release version
* Adds 'releaseRefreshArtifacts' task that deals with the dynamic version change of 'releasePrepare'
* Adds new project properties: 
** release.version: Release version will be infered from property instead of gradle.properties if set
** release.interactive: Allows to define whether the 'releasePrepare' task should ask for user feedback
** release.prepare: Allows to define if 'releasePrepare' should be executed before 'release'
** release.finalize: Allows to define if 'releaseFinalize' should be executed after 'release'
* Adds releaseCheck task which checks if release is done from defined master branch
* Moved tagging of release from 'releasePrepare' to 'release' task
* Added Gradle 2.1 compatible plugin name 'com.rapidminer.gradle.release'

#### 0.1.5
* Fixes retrieval of tag message and tag name

#### 0.1.4
* Fixes wrong declaration of release preperation and release task dependencies

#### 0.1.3
* Adds preparationTasks property

#### 0.1.2
* Fixes wrong releaseVersion property check

#### 0.1.1
* Ensure that gradle.properties contains the correct version before merging release branch to master branch

#### 0.1.0 
* Extension release
