// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ExternalLibraryDescriptor
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.idea.versions.LibraryJarDescriptor
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.compat.toNewPlatform

enum class ConfigureKotlinStatus {
    /** Kotlin is correctly configured using this configurator. */
    CONFIGURED,

    /** The configurator is not applicable to the current project type. */
    NON_APPLICABLE,

    /** The configurator is applicable to the current project type and can configure Kotlin automatically. */
    CAN_BE_CONFIGURED,

    /**
     * The configurator is applicable to the current project type and Kotlin is not configured,
     * but the state of the project doesn't allow to configure Kotlin automatically.
     */
    BROKEN
}

interface KotlinProjectConfigurator {

    fun getStatus(moduleSourceRootGroup: ModuleSourceRootGroup): ConfigureKotlinStatus

    @JvmSuppressWildcards
    fun configure(project: Project, excludeModules: Collection<Module>)

    val presentableText: String

    val name: String

    /*
    We have to provide default for newer 'targetPlatform.get()' through delegation to deprecated 'getTargetPlatform()',
    not the vice versa, to preserve binary compatibility with old inheritors which override only signature of 'getTargetPlatform'

    New clients are encouraged to override both methods
     */
    @JvmDefault
    val targetPlatform: TargetPlatform
        get() = @Suppress("DEPRECATION_ERROR") getTargetPlatform().toNewPlatform()

    @Suppress("DEPRECATION_ERROR")
    @Deprecated(
        message = "This accessor is deprecated and will be removed soon, use API from 'org.jetbrains.kotlin.platform.*' packages instead",
        replaceWith = ReplaceWith("targetPlatform"),
        level = DeprecationLevel.ERROR
    )
    fun getTargetPlatform(): org.jetbrains.kotlin.resolve.TargetPlatform

    fun updateLanguageVersion(
        module: Module,
        languageVersion: String?,
        apiVersion: String?,
        requiredStdlibVersion: ApiVersion,
        forTests: Boolean
    )

    fun changeGeneralFeatureConfiguration(
        module: Module,
        feature: LanguageFeature,
        state: LanguageFeature.State,
        forTests: Boolean
    )

    fun addLibraryDependency(
        module: Module,
        element: PsiElement,
        library: ExternalLibraryDescriptor,
        libraryJarDescriptor: LibraryJarDescriptor,
        scope: DependencyScope
    )

    companion object {
        val EP_NAME = ExtensionPointName.create<KotlinProjectConfigurator>("org.jetbrains.kotlin.projectConfigurator")
    }
}
