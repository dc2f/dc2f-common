package com.dc2f.common

import com.dc2f.api.edit.EditApiConfig
import com.dc2f.api.edit.ratpack.RatpackDc2fServer
import com.dc2f.common.theme.Dc2fEnv
import com.dc2f.render.*
import com.dc2f.util.Dc2fConfig
import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.*
import com.swoval.files.*
import com.swoval.functional.Either
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.apache.commons.io.FileUtils
import java.io.*
import java.nio.file.*
import java.util.*
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger {}


class Serve<ROOT_CONTENT : com.dc2f.Website<*>>(
    private val config: Dc2fConfig<ROOT_CONTENT>
) :
    CliktCommand(help = "Runs server to live-render website.") {
    private val port: Int? by option(help = "port to bind to").int()

    override fun run() {
        val secret = UUID.randomUUID().toString()
        val config = EditApiConfig(
            config,
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
        val assetPath = (this.config as? GeneratorDc2fConfig<*>)?.assetBaseDirectory?.let {
            FileSystems.getDefault().getPath(it).toAbsolutePath()
        }

        val watcher = PathWatchers.get(true)
        watcher.addObserver(object : FileTreeViews.Observer<PathWatchers.Event> {

            override fun onError(t: Throwable) {
                logger.error(t) { "Error while observing paths." }
            }

            override fun onNext(t: PathWatchers.Event) {
                logger.debug { "Detected path $t change ${t.typedPath.path}" }
                runBlocking {
                    if (assetPath != null) {
                        logger.debug { "vs. $assetPath" }
                        if (t.typedPath.path.startsWith(assetPath)) {
                            config.deps.context.imageCache.assetPipelineCache.clear()
                            logger.debug { "Done clearing asset cache." }
                            config.deps.triggerRefreshListeners()
                        }
                    }
                    pathChanged(config, t.typedPath.path)
                }
            }

        })
        assetPath?.let {
            watcher.register(assetPath, Int.MAX_VALUE)
        }
        val result = watcher.register(config.contentRoot, Int.MAX_VALUE)
        if (result is Either.Left<IOException, *>) {
            logger.error(result.value) { "Error while watching directory." }
        } else {
            require(result is Either.Right)
            logger.info { "Successfully watching directory ${result.value}" }
        }
    }

    val changeChannel = Channel<PathWatchers.Event>(capacity = Channel.UNLIMITED);

//    @UseExperimental(ExperimentalCoroutinesApi::class)
//    @Suppress("unused")
//    private fun watchForChanges(config: EditApiConfig<ROOT_CONTENT>) {
//        val contentRootFile = config.contentRoot.toFile()
//        val channel = contentRootFile.asWatchChannel()
//        GlobalScope.launch(Dispatchers.IO) {
//            channel.consumeEach { event ->
//                try {
//                    val changedPath = event.file.toPath()
//                    pathChanged(config, changedPath)
//                } catch (e: Exception) {
//                    logger.warn(e) { "Error while watching for changes." }
//                }
//            }
//            logger.info { "Finished consuming changes." }
//        }
//    }

    val ignoreChanges = arrayOf(
        Regex(""".*___jb_old___$"""),
        Regex(""".*___jb_tmp___$""")
    )

    private suspend fun pathChanged(config: EditApiConfig<ROOT_CONTENT>, changedPath: Path) {
        logger.info { "change: $changedPath" }
//                config.deps.
        val deps = config.deps
        val rootPath = config.contentRoot.toAbsolutePath()
        var contentFsPath = changedPath.toAbsolutePath()
        if (ignoreChanges.any { it.matches(contentFsPath.toString()) }) {
            logger.debug { "Ignoring fs path: $contentFsPath" }
            return
        }
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
        config.deps.triggerRefreshListeners()
        logger.debug { "finished processing change." }
    }
}

class Build<ROOT_CONTENT : com.dc2f.Website<*>>(
    private val config: Dc2fConfig<ROOT_CONTENT>
) :
    CliktCommand(
        help = "Builds the website into public/ output directory."
    ) {
    val env: Dc2fEnv? by option().enum<Dc2fEnv> { it.id }

    override fun run() {
        env?.let { Dc2fEnv.current = it }
        config.loadWebsite(config.contentDirectory) { loadedWebsite, context ->
            logger.info { "loaded website ${loadedWebsite.content.name}." }
            val targetPath = FileSystems.getDefault().getPath("public")
            config.renderToPath(targetPath, loadedWebsite, context) {
                // TODO do we need to render anything more here?
            }

            // FIXME workaround for now to copy over some assets only referenced by css (fonts)
            FileUtils.copyDirectory(File("web", "static"), targetPath.toFile())
        }
    }
}


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
        name = System.getenv("DC2F_ARG0") ?: "dc2f"
    ).main(argv)
}

open class GeneratorDc2fConfig<ROOT_CONTENT : com.dc2f.Website<*>>(
    contentDirectory: String,
    staticDirectory: String,
    rootContentType: KClass<ROOT_CONTENT>,
    urlConfigFromRootContent: (rootConfig: ROOT_CONTENT) -> UrlConfig,
    theme: Theme,
    /**
     * Optional base directory for assets, only used during
     * [Serve]
     */
    val assetBaseDirectory: String?
) : Dc2fConfig<ROOT_CONTENT>(
    contentDirectory,
    staticDirectory,
    rootContentType,
    urlConfigFromRootContent,
    theme
)
