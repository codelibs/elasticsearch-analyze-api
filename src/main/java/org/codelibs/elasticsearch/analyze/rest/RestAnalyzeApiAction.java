package org.codelibs.elasticsearch.analyze.rest;

import static org.elasticsearch.rest.RestStatus.OK;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.util.Attribute;
import org.apache.lucene.util.AttributeReflector;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.cluster.metadata.AliasOrIndex;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.IndexService;
import org.elasticsearch.index.analysis.AnalysisRegistry;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.search.lookup.SourceLookup;

public class RestAnalyzeApiAction extends BaseRestHandler {

    private IndicesService indicesService;

    private AnalysisRegistry analysisRegistry;

    private ClusterService clusterService;

    @Inject
    public RestAnalyzeApiAction(final Settings settings, final RestController controller, final ClusterService clusterService,
            final IndicesService indicesService, final AnalysisRegistry analysisRegistry) {
        super(settings);
        this.clusterService = clusterService;
        this.indicesService = indicesService;
        this.analysisRegistry = analysisRegistry;

        controller.registerHandler(RestRequest.Method.GET, "/_analyze_api", this);
        controller.registerHandler(RestRequest.Method.GET, "/{index}/_analyze_api", this);
        controller.registerHandler(RestRequest.Method.POST, "/_analyze_api", this);
        controller.registerHandler(RestRequest.Method.POST, "/{index}/_analyze_api", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        BytesReference content = request.content();
        if (content == null) {
            return channel -> sendErrorResponse(channel, new ElasticsearchException("No contents."));
        }

        final String defaultIndex = request.param("index");
        final String defaultAnalyzer = request.param("analyzer");

        try {
            final Map<String, Object> sourceAsMap = SourceLookup.sourceAsMap(content);

            final XContentBuilder builder = JsonXContent.contentBuilder();
            if (request.hasParam("pretty")) {
                builder.prettyPrint().lfAtEnd();
            }
            builder.startObject();

            for (Map.Entry<String, Object> entry : sourceAsMap.entrySet()) {
                final String name = entry.getKey();
                @SuppressWarnings("unchecked")
                final Map<String, Object> analyzeData = (Map<String, Object>) entry.getValue();

                String indexName = (String) analyzeData.get("index");
                if (indexName == null) {
                    if (defaultIndex != null) {
                        indexName = defaultIndex;
                    } else {
                        throw new ElasticsearchException("index is not found in your request: " + analyzeData);
                    }
                }
                String analyzerName = (String) analyzeData.get("analyzer");
                if (analyzerName == null) {
                    if (defaultAnalyzer != null) {
                        analyzerName = defaultAnalyzer;
                    } else {
                        throw new ElasticsearchException("analyzer is not found in your request: " + analyzeData);
                    }
                }
                final String text = (String) analyzeData.get("text");
                if (text == null) {
                    throw new ElasticsearchException("text is not found in your request: " + analyzeData);
                }

                builder.startArray(name);

                Analyzer analyzer = getAnalyzer(indexName, analyzerName);
                if (analyzer == null) {
                    throw new ElasticsearchException(analyzerName + " in " + indexName + " is not found.");
                }

                try (TokenStream stream = analyzer.tokenStream(null, new StringReader(text))) {
                    stream.reset();

                    CharTermAttribute term = stream.addAttribute(CharTermAttribute.class);
                    PositionIncrementAttribute posIncr = stream.addAttribute(PositionIncrementAttribute.class);

                    int position = 0;
                    while (stream.incrementToken()) {
                        builder.startObject();

                        int increment = posIncr.getPositionIncrement();
                        if (increment > 0) {
                            position = position + increment;
                        }

                        builder.field("term", term.toString());
                        if (request.paramAsBoolean("position", false)) {
                            builder.field("position", position);
                        }

                        stream.reflectWith(new AttributeReflector() {
                            @Override
                            public void reflect(Class<? extends Attribute> attClass, String key, Object value) {
                                String keyName = decamelize(key);
                                if (request.paramAsBoolean(keyName, false)) {
                                    if (value instanceof BytesRef) {
                                        final BytesRef p = (BytesRef) value;
                                        value = p.toString();
                                    }
                                    try {
                                        builder.field(keyName, value);
                                    } catch (IOException e) {
                                        logger.warn("Failed to write " + key + ":" + value, e);
                                    }
                                }
                            }
                        });

                        builder.endObject();
                    }
                    stream.end();
                }
                builder.endArray();
            }
            builder.endObject();
            return channel -> channel.sendResponse(new BytesRestResponse(OK, builder));
        } catch (Exception e) {
            return channel -> sendErrorResponse(channel, e);
        }

    }

    private Analyzer getAnalyzer(final String indexName, final String analyzerName) throws IOException {
        final MetaData metaData = clusterService.state().getMetaData();
        final AliasOrIndex aliasOrIndex = metaData.getAliasAndIndexLookup().get(indexName);
        if (aliasOrIndex != null) {
            for (final IndexMetaData indexMD : aliasOrIndex.getIndices()) {
                final IndexService indexService = indicesService.indexService(indexMD.getIndex());
                if (indexService != null) {
                    final Analyzer analyzer = indexService.getIndexAnalyzers().get(analyzerName);
                    if (analyzer != null) {
                        return analyzer;
                    }
                }
            }
        }
        return analysisRegistry.getAnalyzer(analyzerName);
    }

    private void sendErrorResponse(final RestChannel channel, final Exception e) {
        try {
            channel.sendResponse(new BytesRestResponse(channel, e));
        } catch (final Exception e1) {
            logger.error("Failed to send a failure response.", e1);
        }
    }

    static String decamelize(final String s) {
        if (s == null) {
            return null;
        }
        StringBuilder buf = new StringBuilder(20);
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (Character.isUpperCase(c)) {
                if (buf.length() != 0) {
                    buf.append('_');
                }
                buf.append(Character.toLowerCase(c));
            } else if (c == ' ') {
                buf.append('_');
            } else if (Character.isAlphabetic(c)) {
                buf.append(Character.toLowerCase(c));
            }
        }
        return buf.toString();
    }

}
