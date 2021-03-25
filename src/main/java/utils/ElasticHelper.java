package utils;

import jade.core.Agent;
import org.apache.http.HttpHost;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;

public class ElasticHelper {
    private ElasticHelper(){}
    private static ElasticHelper elasticHelperInstance = new ElasticHelper();
    public static ElasticHelper getElasticHelperInstance(){return elasticHelperInstance;}
    private static Logger LOGGER = LoggerFactory.getLogger(ElasticHelper.class);
    private Settings settingsInstance = Settings.getSettingsInstance();

    private RestHighLevelClient elasticClient;

    public boolean initElasticClient(String HOST, int PORT) throws IOException {
        elasticClient = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(HOST, PORT, "http")
                ));

        try {
            boolean result = elasticClient.ping(RequestOptions.DEFAULT);
            LOGGER.info("Started elastic-client: " + result + ". client: " + elasticClient.toString());
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public RestHighLevelClient getElasticClient() throws IOException {
        if (elasticClient!=null && elasticClient.ping(RequestOptions.DEFAULT)) {
            return elasticClient;
        } else {
            boolean init = initElasticClient(settingsInstance.getELASTIC_HOST(), settingsInstance.getELASTIC_PORT());
            if (init) {
                return elasticClient;
            } else {
                return null;
            }
        }
    }

    public void indexLogs(Agent agent, String logs){
        if (settingsInstance.getUseElastic()) {
            try {
                XContentBuilder xContentBuilder = XContentFactory.jsonBuilder();
                xContentBuilder.startObject();
                {
                    xContentBuilder.field("agent_name", agent.getLocalName());
                    xContentBuilder.field("agent_type", agent.getClass().getSimpleName());
                    xContentBuilder.timeField("timestamp", new Date());
                    xContentBuilder.field("message", logs);
                }
                xContentBuilder.endObject();
                IndexRequest indexRequest = new IndexRequest("smartgridsos").source(xContentBuilder);

                ActionListener listener = new ActionListener<IndexResponse>() {
                    @Override
                    public void onResponse(IndexResponse indexResponse) {
                        LOGGER.info("Successfully logged msg (" + logs + ").");
                    }

                    @Override
                    public void onFailure(Exception e) {
                        LOGGER.warn("Failed to log to elasticsearch:");
                        e.printStackTrace();
                    }
                };

                elasticClient.indexAsync(indexRequest, RequestOptions.DEFAULT, listener);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            LOGGER.info("Attemped to use elastic logging. Not using elastic logging.");
        }

    }
}
