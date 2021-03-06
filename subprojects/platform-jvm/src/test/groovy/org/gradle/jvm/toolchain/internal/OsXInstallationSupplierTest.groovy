/*
 * Copyright 2020 the original author or authors.
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

package org.gradle.jvm.toolchain.internal

import org.gradle.api.internal.provider.Providers
import org.gradle.api.provider.ProviderFactory
import org.gradle.internal.os.OperatingSystem
import org.gradle.process.internal.ExecHandleFactory
import spock.lang.Specification

class OsXInstallationSupplierTest extends Specification {

    def "is marked as auto-detecting"() {
        createSupplier() instanceof AutoDetectingInstallationSupplier
    }

    def "supplies no installations for absent output"() {
        given:
        def supplier = createSupplier("")

        when:
        def directories = supplier.get()

        then:
        directories.isEmpty()
    }


    def "supplies no installations for wrong os"() {
        given:
        def supplier = createSupplier(null, OperatingSystem.WINDOWS)

        when:
        def directories = supplier.get()

        then:
        directories.isEmpty()
    }

    def "supplies single installations for single candidate"() {
        given:
        def supplier = createSupplier("""
Matching Java Virtual Machines (1):
    1.7.0_80, x86_64:\t"Java SE 7"\t/Library/Java/JavaVirtualMachines/jdk1.7.0_80.jdk/Contents/Home
""")

        when:
        def directories = supplier.get()

        then:
        directories*.location == [new File("/Library/Java/JavaVirtualMachines/jdk1.7.0_80.jdk/Contents/Home")]
        directories*.source == ["macOS java_home"]
    }

    def "supplies multiple installations for multiple paths"() {
        given:
        def supplier = createSupplier("""
Matching Java Virtual Machines (3):
    9, x86_64:\t"Java SE 9-ea"\t/Library/Java/JavaVirtualMachines/jdk-9.jdk/Contents/Home
    1.8.0, x86_64:\t"Java SE 8"\t/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home
    1.7.0_17, x86_64:\t"Java SE 7"\t/Library/Java/JavaVirtualMachines/jdk1.7.0_17.jdk/Contents/Home

""")

        when:
        def directories = supplier.get()

        then:
        directories*.location.containsAll([
            new File("/Library/Java/JavaVirtualMachines/jdk-9.jdk/Contents/Home"),
            new File("/Library/Java/JavaVirtualMachines/jdk1.7.0_17.jdk/Contents/Home"),
            new File("/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home")
        ])
        directories*.source == ["macOS java_home", "macOS java_home", "macOS java_home"]
    }

    OsXInstallationSupplier createSupplier(String output, OperatingSystem os = OperatingSystem.MAC_OS) {
        new OsXInstallationSupplier(Mock(ExecHandleFactory), createProviderFactory(), os) {
            @Override
            def void executeCommand(ByteArrayOutputStream outputStream) {
                outputStream.write(output.bytes, 0, output.bytes.size())
            }
        }
    }

    ProviderFactory createProviderFactory() {
        def providerFactory = Mock(ProviderFactory)
        providerFactory.gradleProperty("org.gradle.java.installations.auto-detect") >> Providers.ofNullable("true")
        providerFactory
    }

}
