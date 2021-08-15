package jack.android.gradle.embedfunction

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.AndroidSourceSet
import jack.android.gradle.embedfunction.transform.EmbedFunctionTransform
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.util.function.Consumer


class EmbedFunctionPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        if(!isAndroidProject(project)) return
        val tempSourceFolder = File(project.rootDir,project.name+"-extension")
        if(!tempSourceFolder.exists()){
            tempSourceFolder.mkdir()
        }
        configureTempSourceFolder(project,tempSourceFolder)

        val appExtension = project.extensions.findByType(AppExtension::class.java)
        appExtension?.registerTransform(EmbedFunctionTransform(project))
    }

    private fun configureTempSourceFolder(project: Project, tempSourceFile: File) {
        val appExtension = project.extensions.getByType(AppExtension::class.java)
        val javaSourceFolder = File(tempSourceFile,"src/main/java")
        if(!javaSourceFolder.exists()){
            javaSourceFolder.mkdirs()
        }
        val resSourceFolder = File(tempSourceFile,"src/main/res")
        if(!resSourceFolder.exists()){
            resSourceFolder.mkdirs()
        }
        appExtension.sourceSets.forEach(Consumer { sourceSet: AndroidSourceSet ->
            sourceSet.java.srcDirs(javaSourceFolder.absolutePath)
            sourceSet.res.srcDirs(resSourceFolder.absoluteFile)
        })
    }

    private inline fun isAndroidProject(project: Project):Boolean {
        return project.plugins.hasPlugin(AppPlugin::class.java)||project.plugins.hasPlugin(LibraryPlugin::class.java)
    }
}