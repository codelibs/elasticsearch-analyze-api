package org.codelibs.elasticsearch.analyze;

import static org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner.newConfigs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.codelibs.elasticsearch.runner.net.Curl;
import org.codelibs.elasticsearch.runner.net.CurlResponse;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;
import org.elasticsearch.node.Node;

public class AnalyzeApiPluginTest extends TestCase {

    private ElasticsearchClusterRunner runner;

    private File[] userDictFiles;

    private int numOfNode = 2;

    @Override
    protected void setUp() throws Exception {
        // create runner instance
        runner = new ElasticsearchClusterRunner();
        // create ES nodes
        runner.onBuild(new ElasticsearchClusterRunner.Builder() {
            @Override
            public void build(final int number, final Builder settingsBuilder) {
                settingsBuilder.put("http.cors.enabled", true);
            }
        }).build(
                newConfigs()
                        .clusterName(
                                "es-analyze-api-" + System.currentTimeMillis())
                        .ramIndexStore().numOfNode(numOfNode));

        // wait for yellow status
        runner.ensureYellow();
    }

    @Override
    protected void tearDown() throws Exception {
        // close runner
        runner.close();
        // delete all files
        runner.clean();
    }

    private void updateDictionary(File file, String content)
            throws IOException, UnsupportedEncodingException,
            FileNotFoundException {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file), "UTF-8"))) {
            bw.write(content);
            bw.flush();
        }
    }

    public void test_runCluster() throws Exception {
        userDictFiles = new File[numOfNode];
        for (int i = 0; i < numOfNode; i++) {
            String confPath = runner.getNode(i).settings().get("path.conf");
            userDictFiles[i] = new File(confPath, "userdict_ja.txt");
            updateDictionary(userDictFiles[i],
                    "東京スカイツリー,東京 スカイツリー,トウキョウ スカイツリー,カスタム名詞");
            userDictFiles[i].deleteOnExit();
        }

        Node node = runner.node();

        final String index = "dataset";

        final String indexSettings = "{\"index\":{\"analysis\":{"
                + "\"tokenizer\":{"//
                + "\"kuromoji_user_dict\":{\"type\":\"kuromoji_tokenizer\",\"mode\":\"extended\",\"user_dictionary\":\"userdict_ja.txt\"}"
                + "},"//
                + "\"analyzer\":{"
                + "\"ja_analyzer\":{\"type\":\"custom\",\"tokenizer\":\"kuromoji_user_dict\",\"filter\":[\"kuromoji_stemmer\"]}"
                + "}"//
                + "}}}";
        runner.createIndex(index,
                ImmutableSettings.builder().loadFromSource(indexSettings)
                        .build());

        try (CurlResponse response = Curl
                .post(node, "/_analyze_api")
                .param("index", index)
                .param("analyzer", "standard")
                .body("{\"test1\":{\"text\":\"東京都\"},"
                        + "\"test2\":{\"index\":\"" + index
                        + "\",\"analyzer\":\"ja_analyzer\",\"text\":\"東京都\"}}")
                .execute()) {
            Map<String, Object> contentAsMap = response.getContentAsMap();
            assertEquals(2, contentAsMap.size());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> test1List = (List<Map<String, Object>>) contentAsMap
                    .get("test1");
            assertEquals(3, test1List.size());
            assertEquals("東", test1List.get(0).get("term").toString());
            assertEquals("京", test1List.get(1).get("term").toString());
            assertEquals("都", test1List.get(2).get("term").toString());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> test2List = (List<Map<String, Object>>) contentAsMap
                    .get("test2");
            assertEquals(2, test2List.size());
            assertEquals("東京", test2List.get(0).get("term").toString());
            assertEquals("都", test2List.get(1).get("term").toString());
        }

        try (CurlResponse response = Curl
                .post(node, "/" + index + "/_analyze_api")
                .param("position", "true")
                .param("start_offset", "true")
                .body("{\"test1\":{\"analyzer\":\"ja_analyzer\",\"text\":\"東京都\"}}")
                .execute()) {
            Map<String, Object> contentAsMap = response.getContentAsMap();
            assertEquals(1, contentAsMap.size());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> test1List = (List<Map<String, Object>>) contentAsMap
                    .get("test1");
            assertEquals(2, test1List.size());
            assertEquals("東京", test1List.get(0).get("term").toString());
            assertEquals("1", test1List.get(0).get("position").toString());
            assertEquals("0", test1List.get(0).get("start_offset").toString());
            assertEquals("都", test1List.get(1).get("term").toString());
            assertEquals("2", test1List.get(1).get("position").toString());
            assertEquals("2", test1List.get(1).get("start_offset").toString());
        }
    }

}
