package net.corda.osgi.node

import net.corda.core.crypto.Crypto
import org.osgi.framework.BundleActivator
import org.osgi.framework.BundleContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class NodeActivator : BundleActivator {

    companion object {

        val logger: Logger = LoggerFactory.getLogger(NodeActivator::class.java)

    } //~ companion

    override fun start(
            context: BundleContext?) {
        logger.info("Start...")
        Crypto.registerProviders()
        logger.info("Crypto provider registered")
        logger.info("Started.")
    }

    override fun stop(
            context: BundleContext?) {
        logger.info("Stop.")
    }

}