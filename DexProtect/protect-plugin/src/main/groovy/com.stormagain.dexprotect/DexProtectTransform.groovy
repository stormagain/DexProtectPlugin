package com.stormagain.dexprotect

import com.android.build.api.transform.*
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.build.gradle.internal.transforms.DexTransform
import org.gradle.api.Project

/**
 * Created by liujian on 2018/4/20.
 */

class DexProtectTransform extends Transform {

    Project project
    ApplicationVariant variant
    DexTransform transform
    String buildType

    DexProtectTransform(Project project, ApplicationVariant variant, String taskName, DexTransform transform) {
        this.project = project
        this.variant = variant
        this.transform = transform
        //variant.buildType.name not reliable
        buildType = taskName.replace('transformDexWithDexFor', '').toLowerCase()
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        transform.transform(transformInvocation)

        File outputDir = transformInvocation.getOutputProvider().getContentLocation("main", TransformManager.CONTENT_DEX, TransformManager.SCOPE_FULL_PROJECT, Format.DIRECTORY)
        File outputDex = new File(outputDir.getAbsolutePath() + File.separator + Constant.COMM_DEX)
        File originDex = new File(outputDir.getAbsolutePath() + File.separator + Constant.ORIGIN_DEX)
        outputDex.renameTo(originDex)

        File targetDex = new File(outputDir.getAbsolutePath() + File.separator + Constant.TARGET_DEX)
        if (targetDex.exists()) {
            targetDex.delete()
        }

        Utils.makeEncryptDex(originDex.absolutePath, targetDex.absolutePath)

        def assetsPath = new File(variant.packageApplication.assets.asPath).getParent() + File.separator + buildType + File.separator
        File assetsFile = new File(assetsPath)
        if (!assetsFile.exists()) {
            assetsFile.mkdirs()
        }
        File encDexFile = new File(assetsFile, Constant.ENCRYPT_FILE_NAME)
        targetDex.renameTo(encDexFile)

        File shellDex = new File(project.rootProject.project(Constant.SHELL_MODULE).projectDir.absolutePath + File.separator + Constant.SHELL_DEX)
        Utils.copyFile(shellDex, outputDex)

        originDex.delete()
        targetDex.delete()
    }

    @Override
    String getName() {
        return transform.getName()
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return transform.getInputTypes()
    }

    @Override
    Set<QualifiedContent.ContentType> getOutputTypes() {
        return transform.getOutputTypes()
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return transform.getScopes()
    }

    @Override
    boolean isIncremental() {
        return transform.isIncremental()
    }

    @Override
    Set<? super QualifiedContent.Scope> getReferencedScopes() {
        return transform.getReferencedScopes()
    }

    @Override
    Collection<SecondaryFile> getSecondaryFiles() {
        return transform.getSecondaryFiles()
    }

    @Override
    Collection<File> getSecondaryFileOutputs() {
        return transform.getSecondaryFileOutputs()
    }

    @Override
    Collection<File> getSecondaryDirectoryOutputs() {
        return transform.getSecondaryDirectoryOutputs()
    }

    @Override
    Map<String, Object> getParameterInputs() {
        return transform.getParameterInputs()
    }

    @Override
    boolean isCacheable() {
        return transform.isCacheable()
    }
}
