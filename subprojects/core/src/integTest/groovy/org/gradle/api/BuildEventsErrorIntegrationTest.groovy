/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.ToBeFixedForConfigurationCache
import org.gradle.integtests.fixtures.UnsupportedWithConfigurationCache

import static org.gradle.integtests.fixtures.ToBeFixedForConfigurationCache.Skip.INVESTIGATE

class BuildEventsErrorIntegrationTest extends AbstractIntegrationSpec {

    def expectBuildScopeListenerDeprecation(String invocation) {
        executer.expectDocumentedDeprecationWarning("Listener registration using $invocation() has been deprecated. This will fail with an error in Gradle 9.0. Consult the upgrading guide for further information: https://docs.gradle.org/current/userguide/upgrading_version_7.html#task_execution_events")
    }

    def "produces reasonable error message when taskGraph.whenReady closure fails"() {
        buildFile << """
    gradle.taskGraph.whenReady {
        throw new RuntimeException('broken')
    }
    task a
"""

        when:
        fails()

        then:
        failure.assertHasDescription("broken")
                .assertHasNoCause()
                .assertHasFileName("Build file '$buildFile'")
                .assertHasLineNumber(3)
    }

    @ToBeFixedForConfigurationCache(skip = INVESTIGATE)
    def "produces reasonable error message when taskGraph.whenReady action fails"() {
        buildFile << """
    def action = {
            throw new RuntimeException('broken')
    } as Action
    gradle.taskGraph.whenReady(action)
    task a
"""

        when:
        fails()

        then:
        failure.assertHasDescription("broken")
                .assertHasNoCause()
                .assertHasFileName("Build file '$buildFile'")
                .assertHasLineNumber(3)
    }

    def "produces reasonable error message when taskGraph listener fails"() {
        buildFile << """
    def listener = {
            throw new RuntimeException('broken')
    } as TaskExecutionGraphListener
    gradle.taskGraph.addTaskExecutionGraphListener(listener)
    task a
"""

        when:
        fails()

        then:
        failure.assertHasDescription("broken")
                .assertHasNoCause()
                .assertHasFileName("Build file '$buildFile'")
                .assertHasLineNumber(3)
    }

    def "produces reasonable error when Gradle.allprojects action fails"() {
        initScriptFile """
            allprojects {
                throw new RuntimeException("broken")
            }
        """
        when:
        executer.usingInitScript(initScriptFile)
        fails "a"

        then:
        failure.assertHasDescription("broken")
                .assertHasNoCause()
                .assertHasFileName("Initialization script '$initScriptFile'")
                .assertHasLineNumber(3)
    }

    @UnsupportedWithConfigurationCache(iterationMatchers = ".*Gradle.buildFinished.*")
    def "produces reasonable error when Gradle.#method closure fails"() {
        settingsFile << """
gradle.${method} {
    throw new RuntimeException("broken")
}
gradle.rootProject { task a }
"""
        when:
        if (deprecation) {
            expectBuildScopeListenerDeprecation("Gradle.${method}")
        }
        fails "a"

        then:
        failure.assertHasDescription("broken")
                .assertHasNoCause()
                .assertHasFileName("Settings file '$settingsFile'")
                .assertHasLineNumber(3)

        where:
        method              | deprecation
        "settingsEvaluated" | false
        "projectsLoaded"    | false
        "projectsEvaluated" | false
        "buildFinished"     | true
    }

    @UnsupportedWithConfigurationCache(iterationMatchers = ".*Gradle.buildFinished.*")
    def "produces reasonable error when Gradle.#method action fails"() {
        settingsFile << """
def action = {
    throw new RuntimeException("broken")
} as Action
gradle.${method}(action)
gradle.rootProject { task a }
"""
        when:
        if (deprecation) {
            expectBuildScopeListenerDeprecation("Gradle.${method}")
        }
        fails "a"

        then:
        failure.assertHasDescription("broken")
                .assertHasNoCause()
                .assertHasFileName("Settings file '$settingsFile'")
                .assertHasLineNumber(3)

        where:
        method              | deprecation
        "settingsEvaluated" | false
        "projectsLoaded"    | false
        "projectsEvaluated" | false
        "buildFinished"     | true
    }

    @UnsupportedWithConfigurationCache
    def "produces reasonable error when BuildListener.#method method fails"() {
        settingsFile << """
def listener = new BuildAdapter() {
    @Override
    void ${method}(${params}) {
        throw new RuntimeException("broken")
    }
}

gradle.addListener(listener)
gradle.rootProject { task a }
"""
        when:
        expectBuildScopeListenerDeprecation("Gradle.addListener")
        fails "a"

        then:
        failure.assertHasDescription("broken")
                .assertHasNoCause()
                .assertHasFileName("Settings file '$settingsFile'")
                .assertHasLineNumber(5)

        where:
        method              | params
        "settingsEvaluated" | "Settings settings"
        "projectsLoaded"    | "Gradle gradle"
        "projectsEvaluated" | "Gradle gradle"
        "buildFinished"     | "BuildResult result"
    }

    @UnsupportedWithConfigurationCache
    def "produces reasonable error message when build fails and Gradle.buildFinished closure also fails"() {
        buildFile << """
    gradle.buildFinished {
        throw new RuntimeException('broken closure')
    }
    task broken {
        doLast { throw new RuntimeException('broken task') }
    }
"""

        when:
        expectBuildScopeListenerDeprecation("Gradle.buildFinished")
        fails("broken")

        then:
        failure.assertHasFailures(2)
        failure.assertHasDescription("Execution failed for task ':broken'.")
                .assertHasCause("broken task")
                .assertHasFileName("Build file '$buildFile'")
                .assertHasLineNumber(6)
        failure.assertHasDescription("broken closure")
                .assertHasFileName("Build file '$buildFile'")
                .assertHasLineNumber(3)
    }
}
