package org.codelibs.elasticsearch.analyze;

import org.codelibs.elasticsearch.analyze.rest.RestAnalyzeApiAction;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestModule;

public class AnalyzeApiPlugin extends Plugin {
    @Override
    public String name() {
        return "analyze-api";
    }

    @Override
    public String description() {
        return "This plugin provides a feature to analyze texts.";
    }

    // for Rest API
    public void onModule(final RestModule module) {
        module.addRestAction(RestAnalyzeApiAction.class);
    }

}
