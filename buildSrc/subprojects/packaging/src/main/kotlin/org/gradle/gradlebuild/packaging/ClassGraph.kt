/*
 * Copyright 2018 the original author or authors.
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

package org.gradle.gradlebuild.packaging

import java.io.File


internal
class ClassGraph(
    private val keepPackages: PackagePatterns,
    val unshadedPackages: PackagePatterns,
    private val ignorePackages: PackagePatterns,
    shadowPackage: String
) {

    private
    val classes: MutableMap<String, ClassDetails> = linkedMapOf()

    val entryPoints: MutableSet<ClassDetails> = linkedSetOf()
    val resources: MutableSet<ResourceDetails> = linkedSetOf()
    var manifest: ResourceDetails? = null

    internal
    val shadowPackagePrefix =
        if (shadowPackage.isEmpty()) ""
        else shadowPackage.replace('.', '/') + "/"

    fun addResource(resource: ResourceDetails) {
        resources.add(resource)
    }

    operator fun get(className: String) =
        classes.computeIfAbsent(className) {
            val outputClassName = if (unshadedPackages.matches(className)) className else shadowPackagePrefix + className
            ClassDetails(className, outputClassName).also { classDetails ->
                classes[className] = classDetails
                if (keepPackages.matches(className) && !ignorePackages.matches(className)) {
                    entryPoints.add(classDetails)
                }
            }
        }
}


internal
class ResourceDetails(val resourceName: String, val sourceFile: File)


internal
class ClassDetails(val className: String, val outputClassName: String) {
    var visited: Boolean = false
    val dependencies: MutableSet<ClassDetails> = linkedSetOf()
    val outputClassFilename
        get() = "$outputClassName.class"
}


internal
class PackagePatterns(givenPrefixes: Set<String>) {

    private
    val prefixes: MutableSet<String> = hashSetOf()

    private
    val names: MutableSet<String> = hashSetOf()

    init {
        givenPrefixes.map { it.replace('.', '/') }.forEach { internalName ->
            names.add(internalName)
            prefixes.add("$internalName/")
        }
    }

    fun matches(packageName: String): Boolean {
        if (names.contains(packageName)) {
            return true
        }
        for (prefix in prefixes) {
            if (packageName.startsWith(prefix)) {
                names.add(packageName)
                return true
            }
        }
        return false
    }
}
