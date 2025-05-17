package com.runicrealms.game.common.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

class GameYamlLoader {

    val yamlMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

    /**
     * Reads all YAML files in a given file or directory. Supports multiple YAML files existing
     * within a single directory.
     *
     * T must be a POJO object that uses Jackson databind's annotations. Parses YAML directly into
     * T, and throws exceptions if it fails.
     */
    inline fun <reified T : Any> readYaml(fileOrDirectory: File): List<T> {
        // TODO don't throw exception when just one file fails
        if (!fileOrDirectory.exists()) return listOf()
        if (fileOrDirectory.isDirectory) {
            return yamlMapper
                .readerFor(T::class.java)
                .readValues<T>(fileOrDirectory)
                .asSequence()
                .toList()
        } else {
            val extensions = listOf("yml", "yaml")
            return fileOrDirectory
                .walkTopDown()
                .filter { it.extension in extensions }
                .flatMap { file ->
                    yamlMapper.readerFor(T::class.java).readValues<T>(file).asSequence().toList()
                }
                .toList()
        }
    }
}
