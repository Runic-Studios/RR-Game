package com.runicrealms.game.common.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File
import org.slf4j.LoggerFactory

class GameYamlLoader {

    val yamlMapper =
        ObjectMapper(YAMLFactory())
            .registerKotlinModule() {
                enable(KotlinFeature.NullIsSameAsDefault)
                enable(KotlinFeature.NullToEmptyMap)
                enable(KotlinFeature.NullToEmptyCollection)
            }
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)

    /**
     * Reads all YAML files in a given file or directory. Supports multiple YAML files existing
     * within a single directory.
     *
     * T must be a POJO object that uses Jackson databind's annotations. Parses YAML directly into
     * T, and throws exceptions if it fails.
     */
    inline fun <reified T : Any> readYaml(fileOrDirectory: File): List<T> {
        val logger = LoggerFactory.getLogger("common")
        if (!fileOrDirectory.exists()) return listOf()
        if (!fileOrDirectory.isDirectory) {
            try {
                return yamlMapper
                    .readerFor(T::class.java)
                    .readValues<T>(fileOrDirectory)
                    .readAll()
                    .toList()
            } catch (exception: Exception) {
                logger.error("Error parsing YAML file ${fileOrDirectory.name}, skipping", exception)
                return listOf()
            }
        } else {
            val extensions = listOf("yml", "yaml")
            return fileOrDirectory
                .walkTopDown()
                .filter { it.extension in extensions }
                .flatMap { file ->
                    try {
                        yamlMapper
                            .readerFor(T::class.java)
                            .readValues<T>(file)
                            .readAll()
                            .toList()
                    } catch (exception: Exception) {
                        logger.error("Error parsing YAML file ${file.name}, skipping", exception)
                        return@flatMap listOf()
                    }
                }
                .toList()
        }
    }
}
