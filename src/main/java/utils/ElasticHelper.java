package utils;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

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
}
