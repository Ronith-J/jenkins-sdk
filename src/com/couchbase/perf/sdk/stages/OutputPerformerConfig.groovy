package com.couchbase.perf.sdk.stages


import com.couchbase.stages.servers.InitialiseCluster
import com.couchbase.stages.Stage
import groovy.json.JsonBuilder
import groovy.json.JsonGenerator
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.yaml.YamlBuilder
import com.couchbase.context.StageContext
import com.couchbase.perf.shared.config.PerfConfig
import com.couchbase.perf.shared.config.Run
import org.apache.groovy.yaml.util.YamlConverter

import java.time.Instant
import java.util.stream.Collectors

/**
 * Outputs the runner config
 */
@CompileStatic
class OutputPerformerConfig extends Stage {
    private final List<Run> runs
    private final String outputFilenameAbs
    private final PerfConfig.Cluster cluster
    private final PerfConfig.Implementation impl
    private final config
    private final InitialiseCluster stageCluster
    private final InitialiseSDKPerformer stagePerformer
    private final Object topLevelSettings

    OutputPerformerConfig(InitialiseCluster stageCluster,
                          InitialiseSDKPerformer stagePerformer,
                          config,
                          PerfConfig.Cluster cluster,
                          PerfConfig.Implementation impl,
                          List<Run> runs,
                          Object topLevelSettings,
                          String outputFilenameAbs) {
        this.stagePerformer = stagePerformer
        this.stageCluster = stageCluster
        this.impl = impl
        this.cluster = cluster
        this.runs = runs
        this.outputFilenameAbs = outputFilenameAbs
        this.config = config
        this.topLevelSettings = topLevelSettings
    }

    @Override
    String name() {
        return "Output performer config for ${runs.size()} runs to $outputFilenameAbs"
    }

    String outputFilenameAbs() {
        return outputFilenameAbs
    }

    @Override
    @CompileDynamic
    void executeImpl(StageContext ctx) {
        var runsAsYaml = runs.stream().map(run -> {
            def yaml = new YamlBuilder()
            yaml {
                uuid UUID.randomUUID().toString()
                operations(run.workload.operations)
                settings {
                    variables(run.workload.settings.variables.collect { it.asYaml() })
                    grpc(run.workload.settings.grpc)
                }
            }
            yaml.content
        }).collect(Collectors.toList())

        def gen = new JsonGenerator.Options()
            .excludeNulls()
            .build()

        // For apples-to-apples comparisons, we have to make sure that whenever we change something in the driver or
        // performer that might alter the results, that these are bumped.
        topLevelSettings.driverVer = 6
        topLevelSettings.performerVer = 1

        def json = new JsonBuilder(gen)
        json {
            impl impl
            connections {
                cluster cluster.toJsonRaw(false)

                performer {
                    hostname "localhost"
                    hostname_docker InitialiseSDKPerformer.CONTAINER_NAME
                    port stagePerformer.port()
                }

                database(config.database)
            }
            runs runsAsYaml
            settings topLevelSettings
            jobId ctx.jc.jobId
        }

        def converted = YamlConverter.convertJsonToYaml(new StringReader(json.toString()))

        new File(outputFilenameAbs).write(converted)
    }
}