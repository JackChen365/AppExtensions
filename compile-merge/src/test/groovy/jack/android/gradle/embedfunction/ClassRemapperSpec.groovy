package jack.android.gradle.embedfunction

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.SimpleRemapper
import spock.lang.Specification

class ClassRemapperSpec extends Specification{
    def "test class remapper"(){
        given:
        def file = new File("src/test/assets/TestClasses/DecorateClass.class")
        String decorateClass = "jack/android/gradle/embedfunction/asm/DecorateClass"
        String targetClass = "jack/android/gradle/embedfunction/asm/TargetClass"
        def classSet = collectClasses(file)
        def map = new HashMap()
        classSet.each { className->
            if(className.contains(decorateClass)){
                map.put(className,className.replace(decorateClass,targetClass))
            }
        }
        def classReader = new ClassReader(file.bytes)
        ClassWriter classWriter = new ClassWriter(classReader,ClassWriter.COMPUTE_MAXS);
        ClassVisitor classRemapper = new ClassRemapper(classWriter, new SimpleRemapper(map))
        classReader.accept(classRemapper,ClassReader.EXPAND_FRAMES);

        File newFile = new File(file.parent,"newTargetClass.class")
        newFile.setBytes(classWriter.toByteArray())
        expect:
        newFile.exists()
    }

    def "test inner class remapper"(){
        given:
        def file = new File("src/test/assets/TestClasses/DecorateClass\$1.class")
        String decorateClassName = "jack/android/gradle/embedfunction/asm/DecorateClass"
        String targetClassName = "jack/android/gradle/embedfunction/asm/TargetClass"
        def classReader = new ClassReader(file.readBytes())
        def classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
        def simpleRemapper = new SimpleRemapper(decorateClassName,targetClassName)
        def classRemapper = new ClassRemapper(classWriter, simpleRemapper)
        classReader.accept(classRemapper, ClassReader.EXPAND_FRAMES)

        def targetFile = new File("src/test/assets/TestClasses/TargetClass\$1.class")
        targetFile.setBytes(classWriter.toByteArray())
        expect:
        targetFile.exists()
    }

    private Set<String> collectClasses(File file){
        def classReader = new ClassReader(file.bytes)
        ClassWriter cw = new ClassWriter(classReader,ClassWriter.COMPUTE_MAXS);
        Set<String> set = new HashSet()
        def simpleRemapper = new SimpleRemapper(null,null){
            @Override
            String mapType(final String internalName) {
                set.add(internalName)
                return super.mapType(internalName)
            }
        }
        ClassVisitor remapper = new ClassRemapper(cw, simpleRemapper)
        classReader.accept(remapper,ClassReader.EXPAND_FRAMES);
        return set
    }
}
