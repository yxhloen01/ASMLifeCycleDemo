package kenny.lifecycle.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import groovy.io.FileType
import kenny.lifecycle.asm.LifecycleClassVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter

public class LifeCycleTransform extends Transform {

    @Override
    String getName() {
        return "LifeCycleTransform"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.PROJECT_ONLY
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        Collection<TransformInput> transformInputs = transformInvocation.inputs
        TransformOutputProvider outputProvider = transformInvocation.outputProvider

        transformInputs.each {
            TransformInput transformInput ->
                transformInput.jarInputs.each {
                    JarInput jarInput ->
                        File file = jarInput.file
                        System.out.println("Find jar input: " + file.name)
                        def dest = outputProvider.getContentLocation(jarInput.name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
                        FileUtils.copyFile(file, dest)
                }

                transformInput.directoryInputs.each {
                    DirectoryInput directoryInput ->
                        File dir = directoryInput.file
                        if (dir) {
                            //设置过滤文件为 .class 文件（去除文件夹类型），并打印文件名称。
                            dir.traverse(type: FileType.FILES, nameFilter: ~/.*\.class/) {
                                File file ->
                                    System.out.println("find class: " + file.name)
                                    ClassReader classReader = new ClassReader(file.bytes)
                                    ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
                                    ClassVisitor classVisitor = new LifecycleClassVisitor(classWriter)
                                    classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
                                    byte[] bytes = classWriter.toByteArray()
                                    FileOutputStream outputStream = new FileOutputStream(file.path)
                                    outputStream.write(bytes)
                                    outputStream.close()
                            }
                        }
                        def dest = outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                        FileUtils.copyDirectory(directoryInput.file, dest)
                }
        }
    }
}