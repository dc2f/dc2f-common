package com.dc2f.common

import com.dc2f.api.edit.EditApiConfig
import com.dc2f.api.edit.ratpack.RatpackDc2fServer
import com.dc2f.util.Dc2fSetup
import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.swoval.files.*
import com.swoval.functional.Either
import dev.vishna.watchservice.asWatchChannel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import mu.KotlinLogging
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.file.*
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

private val logger = KotlinLogging.logger {}


class Serve<ROOT_CONTENT : com.dc2f.Website<*>>(
    private val config: Dc2fConfig<ROOT_CONTENT>
) :
    CliktCommand(help = "Runs server to live-render website.") {
    private val port: Int? by option(help = "port to bind to").int()

    override fun run() {
        val secret = UUID.randomUUID().toString()
        val config = EditApiConfig(
            config.setupClass.createInstance(),
            secret,
            FileSystems.getDefault().getPath(config.contentDirectory),
            staticRoot = config.staticDirectory
        )
        watchForChanges2(config)
//        EditApi(
//            config
//        ).serve()
        RatpackDc2fServer(config).serve(
            port = port
        )
        logger.info { "Done." }
    }

    private fun watchForChanges2(config: EditApiConfig<ROOT_CONTENT>) {
        val watcher = PathWatchers.get(true)
        watcher.addObserver(object : FileTreeViews.Observer<PathWatchers.Event> {

            override fun onError(t: Throwable) {
                logger.error(t) { "Error while observing paths."}
            }

            override fun onNext(t: PathWatchers.Event) {
                logger.debug { "Detected path $t change ${t.typedPath.path}" }
                runBlocking { pathChanged(config, t.typedPath.path) }
            }

        })
        val result = watcher.register(config.contentRoot, Int.MAX_VALUE)
        if (result is Either.Left) {
            logger.error(result.value) { "Error while watching directory." }
        } else {
            require(result is Either.Right)
            logger.info { "Successfully watching directory ${result.value}"}
        }
    }

    @UseExperimental(ExperimentalCoroutinesApi::class)
    @Suppress("unused")
    private fun watchForChanges(config: EditApiConfig<ROOT_CONTENT>) {
        val contentRootFile = config.contentRoot.toFile()
        val channel = contentRootFile.asWatchChannel()
        GlobalScope.launch(Dispatchers.IO) {
            channel.consumeEach { event ->
                val changedPath = event.file.toPath()
                pathChanged(config, changedPath)
            }
        }
    }

    private suspend fun pathChanged(config: EditApiConfig<ROOT_CONTENT>, changedPath: Path) {
        logger.info { "change: $changedPath" }
//                config.deps.
        val deps = config.deps
        val rootPath = config.contentRoot.toAbsolutePath()
        var contentFsPath = changedPath.toAbsolutePath()
        logger.debug { "Change for fs path: $contentFsPath" }
//                if (contentFsPath.endsWith("_index.yml")) {
//                    contentFsPath
//                }
        while (contentFsPath.startsWith(rootPath)) {
//                    if (contentFsPath.p)
            val contentPath = deps.context.findContentPath(contentFsPath.resolve("_index.yml"))
            logger.debug { "Change for fs path: $contentFsPath ($contentPath)" }
            if (contentPath != null) {
                val content = requireNotNull(deps.context.contentByPath[contentPath])
                config.deps.reload(content)
                break
            }
            contentFsPath = contentFsPath.parent
        }
    }
}

class Build<ROOT_CONTENT : com.dc2f.Website<*>>(
    private val config: Dc2fConfig<ROOT_CONTENT>
) :
    CliktCommand(
        help = "Builds the website into public/ output directory."
    ) {
    override fun run() {
        val setup = config.setupClass.createInstance()
        setup.loadWebsite(config.contentDirectory) { loadedWebsite, context ->
            logger.info { "loaded website $loadedWebsite." }
            val targetPath = FileSystems.getDefault().getPath("public")
            setup.renderToPath(targetPath, loadedWebsite, context) {
                // TODO do we need to render antyhing more here?
            }

            // FIXME workaround for now to copy over some assets only referenced by css (fonts)
            FileUtils.copyDirectory(File("web", "static"), targetPath.toFile())
        }
    }
}

data class Dc2fConfig<ROOT_CONTENT : com.dc2f.Website<*>>(
    val contentDirectory: String,
    val staticDirectory: String,
    val setupClass: KClass<out Dc2fSetup<ROOT_CONTENT>>
)


class GeneratorCommand<ROOT_CONTENT : com.dc2f.Website<*>>(
    config: Dc2fConfig<ROOT_CONTENT>,
    name: String = "dc2f"
) :
    CliktCommand(name = name, printHelpOnEmptyArgs = true) {

    init {
        subcommands(Serve(config), Build(config))
    }

    override fun run() {

    }


}
class Generator<ROOT_CONTENT : com.dc2f.Website<*>>(
    private val config: Dc2fConfig<ROOT_CONTENT>
) {
    fun main(argv: Array<String>) = GeneratorCommand(
        config,
        name = System.getenv("DC2F_ARG0")
    ).main(argv)
}
