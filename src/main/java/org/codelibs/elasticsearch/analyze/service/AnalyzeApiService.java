package org.codelibs.elasticsearch.analyze.service;

import java.io.IOException;

import org.codelibs.elasticsearch.analyze.AnalyzeApiPlugin.PluginComponent;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.analysis.AnalysisRegistry;
import org.elasticsearch.indices.IndicesService;

public class AnalyzeApiService extends AbstractLifecycleComponent {

    private IndicesService indicesService;

    private AnalysisRegistry analysisRegistry;

    private ClusterService clusterService;

    @Inject
    public AnalyzeApiService(final Settings settings, final IndicesService indicesService, final AnalysisRegistry analysisRegistry,
            final ClusterService clusterService, final PluginComponent pluginComponent) {
        super(settings);
        this.indicesService = indicesService;
        this.analysisRegistry = analysisRegistry;
        this.clusterService = clusterService;

        pluginComponent.setAnalyzeApiService(this);
    }

    public IndicesService getIndicesService() {
        return indicesService;
    }

    public AnalysisRegistry getAnalysisRegistry() {
        return analysisRegistry;
    }

    public ClusterService getClusterService() {
        return clusterService;
    }

    @Override
    protected void doStart() {
    }

    @Override
    protected void doStop() {
    }

    @Override
    protected void doClose() throws IOException {
    }

}
