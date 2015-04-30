package org.codelibs.elasticsearch.analyze;

import org.codelibs.elasticsearch.analyze.rest.RestAnalyzeApiAction;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.rest.RestModule;

public class AnalyzeApiPlugin extends AbstractPlugin {
    @Override
    public String name() {
        return "AnalyzeApiPlugin";
    }

    @Override
    public String description() {
        return "This is a elasticsearch-analyze-api plugin.";
    }

    // for Rest API
    public void onModule(final RestModule module) {
        module.addRestAction(RestAnalyzeApiAction.class);
    }

}
