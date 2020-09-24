package net.corda.osgi.wrap

import org.apache.felix.framework.resolver.ResourceNotFoundException
import org.osgi.framework.Bundle
import org.osgi.framework.BundleContext
import org.osgi.framework.Constants
import org.osgi.framework.launch.FrameworkFactory
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.URL
import java.util.*
import java.util.function.Consumer
import kotlin.system.exitProcess

class Wrap {

    companion object {

        val logger = LoggerFactory.getLogger(Wrap::class.java)

        val osgiFrameworkFactoryResource = "META-INF/services/org.osgi.framework.launch.FrameworkFactory"

        val jarFileExtension = ".jar"

    } //~ companion

}

@Throws(ResourceNotFoundException::class)
private fun getFrameworkFactory(): FrameworkFactory {
    val frameworkFactoryUrl = Wrap::class.java.classLoader.getResource(Wrap.osgiFrameworkFactoryResource)
    if (frameworkFactoryUrl != null) {
        val bufferedReader = BufferedReader(InputStreamReader(frameworkFactoryUrl.openStream()))
        bufferedReader.use { reader ->
            var frameworkFactoryClassName = reader.readLine()
            while (frameworkFactoryClassName != null) {
                frameworkFactoryClassName = frameworkFactoryClassName.trim { it <= ' ' }
                // Try to load first non-empty, non-commented line.
                if (frameworkFactoryClassName.length > 0 && frameworkFactoryClassName[0] != '#') {
                    return Class.forName(frameworkFactoryClassName).newInstance() as FrameworkFactory
                }
                frameworkFactoryClassName = reader.readLine()
            }
        }
    }
    throw ResourceNotFoundException("Could not find framework factory.")
}

private fun getJarList(): ArrayList<File> {
    val bundleUrl: URL = Wrap::class.java.classLoader.getResource("components")
    val bundleDir = File(bundleUrl.toURI())
    val files: Array<File> = bundleDir.listFiles()
    val jarList: ArrayList<File> = ArrayList(files.filter { it.name.toLowerCase().endsWith(Wrap.jarFileExtension) })
    jarList.sort()
    return jarList
}

private fun isFragment(bundle: Bundle): Boolean {
    return bundle.headers[Constants.FRAGMENT_HOST] != null
}


fun main(args: Array<String>) {
    Wrap.logger.info("Run...")

    val jarList = getJarList()

    // Start the OSGi framework.
    val frameworkFactory = getFrameworkFactory()
    Wrap.logger.info("${frameworkFactory::class.java.canonicalName} OSGi framework factory set.")
    val propertyMap: MutableMap<String, String> = HashMap<String, String>()
    for (property: MutableMap.MutableEntry<Any, Any> in System.getProperties().entries) {
        propertyMap[property.key as String] = property.value as String
    }
    propertyMap[Constants.FRAMEWORK_STORAGE_CLEAN] = "onFirstInit"
    val osgiFramework = frameworkFactory.newFramework(propertyMap)
    Wrap.logger.info("${osgiFramework::class.java.canonicalName} OSGi framework start...")
    Runtime.getRuntime().addShutdownHook(object : Thread() {
        // Stop the OSGi framework.
        override fun run() {
            try {
                Wrap.logger.info("${osgiFramework::class.java.canonicalName} OSGi framework stopping...")
                osgiFramework.stop()
                osgiFramework.waitForStop(0)
                Wrap.logger.info("${osgiFramework::class.java.canonicalName} OSGi framework stop.")
            } catch (ex: Exception) {
                Wrap.logger.info("${osgiFramework::class.java.canonicalName} OSGi framework error: $ex!")
            }
        }
    })
    osgiFramework.start()

    val bundleContext: BundleContext = osgiFramework.bundleContext
    val bundleList = ArrayList<Bundle>()
    for(jar: File in jarList) {
        val bundleLocation = jar.toURI().toString()
        Wrap.logger.info("$bundleLocation install...")
        val bundle = bundleContext.installBundle(bundleLocation)
        bundleList.add(bundle)
        Wrap.logger.info("$bundleLocation ID ${bundle.bundleId} installed.")
    }
    for(bundle: Bundle in bundleList!!) {
        if (!isFragment(bundle)) {
            bundle.start()
        }
    }


    // Wait the OSGi framework stops.
    osgiFramework!!.waitForStop(0)
    Wrap.logger.info("Exit.")
    exitProcess(0)
}