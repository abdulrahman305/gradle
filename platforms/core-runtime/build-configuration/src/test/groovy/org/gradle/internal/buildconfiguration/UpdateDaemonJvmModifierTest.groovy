/*
 * Copyright 2024 the original author or authors.
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

package org.gradle.internal.buildconfiguration

import org.gradle.api.JavaVersion
import org.gradle.internal.buildconfiguration.tasks.UpdateDaemonJvmModifier
import org.gradle.internal.jvm.inspection.JvmVendor
import org.gradle.jvm.toolchain.JvmImplementation
import org.gradle.test.fixtures.file.TestFile
import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.junit.Rule
import spock.lang.Specification

class UpdateDaemonJvmModifierTest extends Specification {
    @Rule
    final TestNameTestDirectoryProvider tmpDir = new TestNameTestDirectoryProvider(getClass())

    final TestFile daemonJVMPropertiesFile = tmpDir.file(DaemonJVMPropertiesDefaults.DAEMON_JVM_PROPERTIES_FILE)

    def "writes expected properties into file"() {
        when:
        UpdateDaemonJvmModifier.updateJvmCriteria(daemonJVMPropertiesFile, JavaVersion.VERSION_11, JvmVendor.fromString("IBM"), JvmImplementation.VENDOR_SPECIFIC)
        then:
        def props = daemonJVMPropertiesFile.properties
        props[DaemonJVMPropertiesDefaults.TOOLCHAIN_VERSION_PROPERTY] == "11"
        props[DaemonJVMPropertiesDefaults.TOOLCHAIN_VENDOR_PROPERTY] == "IBM"
        props[DaemonJVMPropertiesDefaults.TOOLCHAIN_IMPLEMENTATION_PROPERTY] == "vendor-specific"
        daemonJVMPropertiesFile.text.contains("#This file is generated by " + DaemonJVMPropertiesConfigurator.TASK_NAME)
    }

    def "writes only non-null properties into file"() {
        when:
        UpdateDaemonJvmModifier.updateJvmCriteria(daemonJVMPropertiesFile, JavaVersion.VERSION_11, null, JvmImplementation.VENDOR_SPECIFIC)
        then:
        def props = daemonJVMPropertiesFile.properties
        props[DaemonJVMPropertiesDefaults.TOOLCHAIN_VERSION_PROPERTY] == "11"
        props[DaemonJVMPropertiesDefaults.TOOLCHAIN_VENDOR_PROPERTY] == null
        props[DaemonJVMPropertiesDefaults.TOOLCHAIN_IMPLEMENTATION_PROPERTY] == "vendor-specific"
    }

    def "writes only java version when no other properties are given"() {
        when:
        UpdateDaemonJvmModifier.updateJvmCriteria(daemonJVMPropertiesFile, JavaVersion.VERSION_11, null, null)
        then:
        def props = daemonJVMPropertiesFile.properties
        props[DaemonJVMPropertiesDefaults.TOOLCHAIN_VERSION_PROPERTY] == "11"
        props[DaemonJVMPropertiesDefaults.TOOLCHAIN_VENDOR_PROPERTY] == null
        props[DaemonJVMPropertiesDefaults.TOOLCHAIN_IMPLEMENTATION_PROPERTY] == null
    }

    def "existing properties are removed when null is passed"() {
        given:
        daemonJVMPropertiesFile.text = """
            ${DaemonJVMPropertiesDefaults.TOOLCHAIN_VERSION_PROPERTY}=11
            ${DaemonJVMPropertiesDefaults.TOOLCHAIN_VENDOR_PROPERTY}=IBM
            ${DaemonJVMPropertiesDefaults.TOOLCHAIN_IMPLEMENTATION_PROPERTY}=vendor-specific
        """
        when:
        UpdateDaemonJvmModifier.updateJvmCriteria(daemonJVMPropertiesFile, JavaVersion.VERSION_15, null, null)
        then:
        def props = daemonJVMPropertiesFile.properties
        props[DaemonJVMPropertiesDefaults.TOOLCHAIN_VERSION_PROPERTY] == "15"
        props[DaemonJVMPropertiesDefaults.TOOLCHAIN_VENDOR_PROPERTY] == null
        props[DaemonJVMPropertiesDefaults.TOOLCHAIN_IMPLEMENTATION_PROPERTY] == null
    }

    def "existing unrecognized properties are not preserved"() {
        daemonJVMPropertiesFile.text = """
            # this comment is not preserved
            com.example.foo=bar
            ${DaemonJVMPropertiesDefaults.TOOLCHAIN_VERSION_PROPERTY}=15
        """
        when:
        UpdateDaemonJvmModifier.updateJvmCriteria(daemonJVMPropertiesFile, JavaVersion.VERSION_11, JvmVendor.fromString("IBM"), JvmImplementation.VENDOR_SPECIFIC)
        then:
        def props = daemonJVMPropertiesFile.properties
        props.size() == 3
        props[DaemonJVMPropertiesDefaults.TOOLCHAIN_VERSION_PROPERTY] == "11"
        props[DaemonJVMPropertiesDefaults.TOOLCHAIN_VENDOR_PROPERTY] == "IBM"
        props[DaemonJVMPropertiesDefaults.TOOLCHAIN_IMPLEMENTATION_PROPERTY] == "vendor-specific"
        !daemonJVMPropertiesFile.text.contains("# this comment is not preserved")
    }
}
