package org.codelibs.elasticsearch.analyze;

import java.util.Arrays;
import java.util.List;

import org.codelibs.elasticsearch.analyze.rest.RestAnalyzeApiAction;
import org.elasticsearch.plugins.ActionPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestHandler;

public class AnalyzeApiPlugin extends Plugin implements ActionPlugin {
    @Override
    public List<Class<? extends RestHandler>> getRestHandlers() {
        return Arrays.asList(RestAnalyzeApiAction.class);
    }
}
