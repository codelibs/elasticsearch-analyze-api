package org.codelibs.elasticsearch.analyze.rest;

import junit.framework.TestCase;

public class RestAnalyzeApiActionTest extends TestCase {

    public void test_decamelize() throws Exception {
        assertEquals("term", RestAnalyzeApiAction.decamelize("term"));
        assertEquals("start_offset",
                RestAnalyzeApiAction.decamelize("startOffset"));
        assertEquals("part_of_speech",
                RestAnalyzeApiAction.decamelize("partOfSpeech"));
        assertEquals("part_of_speech_en",
                RestAnalyzeApiAction.decamelize("partOfSpeech (en)"));
    }

}
