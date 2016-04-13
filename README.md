Elasticsearch Analyze API Pugin
=======================

## Overview

Analyze API Plugin provides a feature to analyze texts.
This plugin will be one of solutions if you want to use elasticsearch as a server to analyze texts for Machine Learning.

## Version

| Version   | Tested on Elasticsearch |
|:---------:|:-----------------------:|
| master    | 2.3.X                   |
| 2.2.0     | 2.2.2                   |
| 2.1.1     | 2.1.1                   |
| 1.5.0     | 1.5.2                   |

### Issues/Questions

Please file an [issue](https://github.com/codelibs/elasticsearch-dynarank/issues "issue").
(Japanese forum is [here](https://github.com/codelibs/codelibs-ja-forum "here").)

## Installation

### Install Analyze API Plugin

    $ $ES_HOME/bin/plugin install org.codelibs/elasticsearch-analyze-api/2.2.0

## Getting Started

### How to analyze texts

This plugin provides /_analyze_api API as below.

    curl -XPOST "http://localhost:9200/{default_index}/_analyze_api?analyzer={default_analyzer}&{option_name}=[true|false]" -d'
    {
      "{target_name1}":{
        "index":"{index_name}",
        "analyzer":"{analyzer_name}"
        "text":"{target_text1}"
      },
      "{target_name2}":{
        "index":"{index_name}",
        "analyzer":"{analyzer_name}"
        "text":"{target_text2}"
      },
      ...
    }'

You can analyze multiple texts in 1 request.

| Parameter        | Description                                                    |
|:----------------:|:---------------------------------------------------------------|
| default_index    | (Optional) A default index name to analyze texts.              |
| default_analyzer | (Optional) A default analyzer name to analyze texts.           |
| index_name       | (Optional) An index name to analyze text of the same block.    |
| analyzer_name    | (Optional) An analyzer name to analyze text of the same block. |
| target_name      | A name for the analyzing text.                                 |
| target_text      | An analzyed text.                                              |
| option_name      | Lucene's attribute name, such as start_offset.                 |

To analyze texts, it's better to create an index for analyzing(ex. create .analyzer index).

### Analyze 1 text

    curl -XPOST "http://localhost:9200/.analyzer/_analyze_api?analyzer=standard" -d'
    {
      "test1":{
        "text":"This is a pen."
      }
    }

The response is:

    {
       "test1": [
           {
              "term": "this"
           },
           {
              "term": "is"
           },
           {
              "term": "a"
           },
           {
              "term": "pen"
           }
        ]
    }

If you want to add start_offset and end_offset, send:

    curl -XPOST "http://localhost:9200/.analyzer/_analyze_api?analyzer=standard&start_offset=true&end_offset=true" -d'
    ...

The response is:

    {
        "test1": [
            {
               "term": "this",
               "start_offset": 0,
               "end_offset": 4
            },
            {
               "term": "is",
               "start_offset": 5,
               "end_offset": 7
            },
            {
               "term": "a",
               "start_offset": 8,
               "end_offset": 9
            },
            {
               "term": "pen",
               "start_offset": 10,
               "end_offset": 13
            }
        ]
    }

### Analyze multiple texts in 1 request

    curl -XPOST "http://localhost:9200/.analyzer/_analyze_api?analyzer=standard" -d'
    {
      "test1":{
        "text":"This is a pen."
      },
      "test2":{
        "analyzer":"kuromoji_analyzer",
        "text":"今日は晴れです。"
      }
    }'

The response is:

    {
       "test2": [
          {
             "term": "今日"
          },
          {
             "term": "は"
          },
          {
             "term": "晴れ"
          },
          {
             "term": "です"
          },
          {
             "term": "。"
          }
       ],
       "test1": [
          {
             "term": "this"
          },
          {
             "term": "is"
          },
          {
             "term": "a"
          },
          {
             "term": "pen"
          }
       ]
    }
