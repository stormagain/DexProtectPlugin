package com.stormagain.dexprotect

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.pipeline.TransformTask
import com.android.build.gradle.internal.transforms.DexTransform
import groovy.xml.Namespace
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.execution.TaskExecutionGraphListener

import java.lang.reflect.Field

/**
 * Created by liujian on 2018/4/20.
 */
class DexProtectPlugin implements Plugin<Project> {

    def PROXY_APPLICATION = "com.stormagain.dexshell.ProxyApplication"
    def META_KEY = "REAL_APP"
    def dexProtectEnable

    @Override
    void apply(Project project) {

        if (!project.plugins.hasPlugin('com.android.application')) {
            throw new GradleException('Android Application plugin required')
        }
        project.extensions.create('protect', DexProtectExtension)
        project.afterEvaluate {
            dexProtectEnable = project.extensions.protect.dexProtectEnable
            if (!dexProtectEnable) {
                return
            }
            def android = project.extensions.getByType(AppExtension)
            try {
                //close preDexLibraries
                android.dexOptions.preDexLibraries = false
                //open jumboMode
                android.dexOptions.jumboMode = true
                //disable dex archive mode
                disableArchiveDex()
            } catch (Throwable e) {
                //no preDexLibraries field, just continue
            }

            android.applicationVariants.all {
                variant ->
                    checkInstantRun(project, variant)
                    checkMultiDex(variant)
                    inject(project, variant)
                    variant.outputs.each {
                        output ->
                            output.processManifest.doLast {

                                output.processManifest.outputs.files.each { File file ->
                                    def manifestFile = null;
                                    //在gradle plugin 3.0.0之前，file是文件，且文件名为AndroidManifest.xml
                                    //在gradle plugin 3.0.0之后，file是目录，且不包含AndroidManifest.xml，需要自己拼接
                                    //除了目录和AndroidManifest.xml之外，还可能会包含manifest-merger-debug-report.txt等不相干的文件，过滤它
                                    if ((file.name.equalsIgnoreCase("AndroidManifest.xml") && !file.isDirectory()) || file.isDirectory()) {
                                        if (file.isDirectory()) {
                                            //3.0.0之后，自己拼接AndroidManifest.xml
                                            manifestFile = new File(file, "AndroidManifest.xml")
                                        } else {
                                            //3.0.0之前，直接使用
                                            manifestFile = file
                                        }
                                        //检测文件是否存在
                                        if (manifestFile != null && manifestFile.exists()) {
                                            def space = new Namespace('http://schemas.android.com/apk/res/android', 'android')
                                            def root = new XmlParser().parse(manifestFile)
                                            def realApp = root.application[0].attributes().get(space.name)
                                            if (realApp == null || "".equals(realApp)) {
                                                throw new GradleException("no application find")
                                            }

                                            root.application[0].attributes().put(space.name, PROXY_APPLICATION)
                                            root.application[0].appendNode('meta-data', [(space.name): META_KEY, (space.value): realApp])

                                            def updatedContent = groovy.xml.XmlUtil.serialize(root)
                                            manifestFile.write(updatedContent, 'UTF-8')
                                        }
                                    }
                                }

                            }
                    }
            }
        }
    }

    private void checkMultiDex(def variant) {
        boolean multiDexEnabled = variant.variantData.variantConfiguration.isMultiDexEnabled()
        if (multiDexEnabled) {
            throw new GradleException(
                    "DexProtect does not support multiDex currently."
            )
        }
    }

    private void checkInstantRun(Project project, ApplicationVariant variant) {
        def variantName = variant.name.capitalize()
        def instantRunTask = getInstantRunTask(project, variantName)
        if (instantRunTask != null) {
            throw new GradleException(
                    "DexProtect does not support instant run mode, please trigger build"
                            + " by assemble${variantName} or disable instant run"
                            + " in 'File->Settings...'."
            )
        }
    }

    void disableArchiveDex() {
        try {
            def booleanOptClazz = Class.forName('com.android.build.gradle.options.BooleanOption')
            def enableDexArchiveField = booleanOptClazz.getDeclaredField('ENABLE_DEX_ARCHIVE')
            enableDexArchiveField.setAccessible(true)
            def enableDexArchiveEnumObj = enableDexArchiveField.get(null)
            def defValField = enableDexArchiveEnumObj.getClass().getDeclaredField('defaultValue')
            defValField.setAccessible(true)
            defValField.set(enableDexArchiveEnumObj, false)
        } catch (Throwable thr) {
            // To some extends, class not found means we are in lower version of android gradle
            // plugin, so just ignore that exception.
            if (!(thr instanceof ClassNotFoundException)) {
                project.logger.error("reflectDexArchiveFlag error: ${thr.getMessage()}.")
            }
        }
    }

    void inject(Project project, ApplicationVariant variant) {

        if (project.name.equals('dexshell')) {
            return
        }

        project.getGradle().getTaskGraph().addTaskExecutionGraphListener(new TaskExecutionGraphListener() {
            @Override
            public void graphPopulated(TaskExecutionGraph taskGraph) {
                for (Task task : taskGraph.getAllTasks()) {
                    if (task instanceof TransformTask) {
                        if (((TransformTask) task).getTransform() instanceof DexTransform && !(((TransformTask) task).getTransform() instanceof DexProtectTransform)) {
                            DexTransform dexTransform = task.transform
                            DexProtectTransform hookDexTransform = new DexProtectTransform(project, variant, task.name, dexTransform)

                            Field field = TransformTask.class.getDeclaredField("transform")
                            field.setAccessible(true)
                            field.set(task, hookDexTransform)
                            break
                        }
                    }
                }
            }
        })
    }

    Task getInstantRunTask(Project project, String variantName) {
        String instantRunTask = "transformClassesWithInstantRunFor${variantName}"
        return project.tasks.findByName(instantRunTask)
    }

}