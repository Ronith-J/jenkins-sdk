package com.couchbase.perf.sdk.stages

import com.couchbase.context.StageContext
import com.couchbase.stages.Stage
import com.couchbase.tools.performer.BuildDockerColumnarGoPerformer
import com.couchbase.tools.performer.VersionToBuildUtil
import groovy.transform.CompileStatic

@CompileStatic
class BuildDockerColumnarGoSDKPerformer extends Stage{

    private final String sdkVersion
    final String imageName

    static String genImageName(String sdkVersion) {
        return "performer-columnar-go" + sdkVersion
    }

    BuildDockerColumnarGoSDKPerformer(String sdkVersion) {
        this(BuildDockerColumnarGoSDKPerformer.genImageName(sdkVersion), sdkVersion)
    }

    BuildDockerColumnarGoSDKPerformer(String imageName, String sdkVersion) {
        this.sdkVersion = sdkVersion
        this.imageName = imageName
    }

    @Override
    String name() {
        return "Building image ${imageName}"
    }

    @Override
    void executeImpl(StageContext ctx) {
        BuildDockerColumnarGoPerformer.build(ctx.env, ctx.sourceDir(), VersionToBuildUtil.from(sdkVersion, null), imageName)
    }

    String getImageName(){
        return imageName
    }
}
