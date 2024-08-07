/*
 * Copyright 2023 the original author or authors.
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

package org.gradle.buildinit.plugins.internal

import org.gradle.api.file.Directory
import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.junit.Rule
import spock.lang.Specification

import static org.gradle.util.internal.TextUtil.toPlatformLineSeparators

class VersionCatalogGeneratorTest extends Specification {

    @Rule
    TestNameTestDirectoryProvider tmpDir = new TestNameTestDirectoryProvider(getClass())

    Directory target = Mock()
    File versionCatalogFile = tmpDir.file("gradle/libs.versions.toml")
    BuildContentGenerationContext buildContentGenerationContext
    VersionCatalogDependencyRegistry versionCatalogDependencyRegistry
    VersionCatalogGenerator versionCatalogGenerator

    def setup() {
        target.getAsFile() >> tmpDir.file(".")
        versionCatalogDependencyRegistry = new VersionCatalogDependencyRegistry(true)
        buildContentGenerationContext = new BuildContentGenerationContext(versionCatalogDependencyRegistry)
        versionCatalogGenerator = VersionCatalogGenerator.create(target)
    }

    def "generates empty gradle/libs.versions.toml file for empty registry"() {
        when:
        versionCatalogGenerator.generate(buildContentGenerationContext, false)

        then:
        versionCatalogFile.file
        versionCatalogFile.text == ""
    }

    def "generates a comment in gradle/libs.versions.toml file for empty registry"() {
        when:
        versionCatalogGenerator.generate(buildContentGenerationContext, true)

        then:
        versionCatalogFile.file
        versionCatalogFile.text == toPlatformLineSeparators("""# This file was generated by the Gradle 'init' task.
# https://docs.gradle.org/current/userguide/platforms.html#sub::toml-dependencies-format
""")
    }

    def "generates version and library based on module"() {
        setup:
        versionCatalogDependencyRegistry.registerLibrary("com.example.group:long", "v1")

        when:
        versionCatalogGenerator.generate(buildContentGenerationContext, false)

        then:
        versionCatalogFile.file
        versionCatalogFile.text == toPlatformLineSeparators("""[versions]
com-example-group-long = "v1"

[libraries]
com-example-group-long = { module = "com.example.group:long", version.ref = "com-example-group-long" }
""")
    }

    def "merges multiple libraries when encountering identical coordinates"() {
        setup:
        versionCatalogDependencyRegistry.registerLibrary("group:artifact", "1.1")
        versionCatalogDependencyRegistry.registerLibrary("group:artifact", "1.1")

        when:
        versionCatalogGenerator.generate(buildContentGenerationContext, false)

        then:
        versionCatalogFile.file
        versionCatalogFile.text == toPlatformLineSeparators("""[versions]
group-artifact = "1.1"

[libraries]
group-artifact = { module = "group:artifact", version.ref = "group-artifact" }
""")
    }

    def "generates multiple versions when encountering different versions"() {
        setup:
        versionCatalogDependencyRegistry.registerLibrary("group:long", "1.1")
        versionCatalogDependencyRegistry.registerLibrary("group:long", "1.2")

        when:
        versionCatalogGenerator.generate(buildContentGenerationContext, false)

        then:
        versionCatalogFile.file
        versionCatalogFile.text == toPlatformLineSeparators("""[versions]
group-long = "1.1"
group-long-x1 = "1.2"

[libraries]
group-long = { module = "group:long", version.ref = "group-long" }
group-long-x1 = { module = "group:long", version.ref = "group-long-x1" }
""")
    }

    def "generates valid identifiers"() {
        setup:
        versionCatalogDependencyRegistry.registerLibrary("JUnit:something", "4")
        versionCatalogDependencyRegistry.registerLibrary("group:artifact_5", "1.1.1")

        when:
        versionCatalogGenerator.generate(buildContentGenerationContext, false)

        then:
        versionCatalogFile.file
        versionCatalogFile.text == toPlatformLineSeparators("""[versions]
group-artifact-v5 = "1.1.1"
junit-something = "4"

[libraries]
group-artifact-v5 = { module = "group:artifact_5", version.ref = "group-artifact-v5" }
junit-something = { module = "JUnit:something", version.ref = "junit-something" }
""")
    }

    def "generates plugin"() {
        setup:
        versionCatalogDependencyRegistry.registerPlugin("com.example.long", "1337", null)

        when:
        versionCatalogGenerator.generate(buildContentGenerationContext, false)

        then:
        versionCatalogFile.file
        versionCatalogFile.text == toPlatformLineSeparators("""[plugins]
com-example-long = { id = "com.example.long", version = "1337" }
""")
    }

    def "generates plugin with explicit alias"() {
        setup:
        versionCatalogDependencyRegistry = new VersionCatalogDependencyRegistry(false)
        buildContentGenerationContext = new BuildContentGenerationContext(versionCatalogDependencyRegistry)

        versionCatalogDependencyRegistry.registerPlugin("com.example.long", "1337", "example-alias")

        when:
        versionCatalogGenerator.generate(buildContentGenerationContext, false)

        then:
        versionCatalogFile.file
        versionCatalogFile.text == toPlatformLineSeparators("""[plugins]
example-alias = { id = "com.example.long", version = "1337" }
""")
    }
}
