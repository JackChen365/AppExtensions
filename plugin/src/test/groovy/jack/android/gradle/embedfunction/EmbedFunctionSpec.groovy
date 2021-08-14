package jack.android.gradle.embedfunction

import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification

class EmbedFunctionSpec extends Specification{
    @Rule TemporaryFolder testProjectDir = new TemporaryFolder(new File("build/tmp"))
    @Shared private def inputAssetsProvider = new TestAssetsProvider("TestApp")
    def "test the embed function plugin"(){
        given:
        FileUtils.copyDirectory(inputAssetsProvider.functionalAssetsDir,testProjectDir.root)
        def tmpLocalProperties = new File(testProjectDir.root,"local.properties")
        tmpLocalProperties.append("sdk.dir="+getAndroidSdkDir())

        def appBuildScript = new File(testProjectDir.root,"app/build.gradle")
        appBuildScript.text = appBuildScript.text.replace("//id 'plugin-placeholder'","id 'embed-function'")
        expect:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments(':app:assembleRelease')
                .withDebug(true)
                .forwardOutput()
                .withPluginClasspath()
                .build()
        null != result
    }

    private def getAndroidSdkDir() {
        def localPropertiesFile = new File('../local.properties')
        if (localPropertiesFile.exists()) {
            Properties local = new Properties()
            local.load(new FileReader(localPropertiesFile))
            if (local.containsKey('sdk.dir')) {
                def property = local.getProperty("sdk.dir")
                if (null != property) {
                    File sdkDir = new File(property)
                    if (sdkDir.exists()) {
                        return property
                    }
                }
            }
        }
        return new NullPointerException("Can not found the initial android SDK configuration.")
    }
}
