/*
 * Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import org.gradle.api.tasks.bundling.Zip

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.zipline.gradle.plugin)
    alias(libs.plugins.spotubeGradle)
}

kotlin {
    applyDefaultHierarchyTemplate()

    js {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":plugin_interfaces"))

            api(libs.zipline.core)
            api(libs.kotlinx.coroutines.core)
            api(libs.semver)
            implementation(libs.kotlinx.serialization.json)
        }

        jsMain.dependencies {}
    }
}

zipline {
    mainFunction.set("org.spotube.spotify_connect_plugin.main")
}

plugins.withType<YarnPlugin> {
    the<YarnRootExtension>().yarnLockAutoReplace = true
}

// Packages the Zipline compiler output (manifest.zipline.json + compiled modules) together with
// plugin.json into the .smplug zip the app's "Installa un plugin" flow expects: it unzips this
// straight into the plugin's own folder and requires both files at the root.
fun registerPackageTask(flavor: String, compileTaskName: String) {
    val capitalizedFlavor = flavor.replaceFirstChar { it.uppercase() }

    tasks.register<Zip>("package${capitalizedFlavor}Smplug") {
        group = "distribution"
        description = "Packages the $flavor Zipline executable and plugin.json into a smplug."

        dependsOn(tasks.named(compileTaskName))

        archiveFileName.set("spotify-connect-plugin-$flavor.smplug")
        destinationDirectory.set(layout.buildDirectory.dir("distributions"))

        from(layout.buildDirectory.dir("zipline/$capitalizedFlavor")) {
            include("**/*")
        }
        from(layout.projectDirectory.file("plugin.json"))

        inputs.file(layout.projectDirectory.file("plugin.json"))
    }
}

registerPackageTask("development", "compileDevelopmentExecutableKotlinJsZipline")
registerPackageTask("production", "compileProductionExecutableKotlinJsZipline")
