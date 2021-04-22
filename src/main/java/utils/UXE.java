package utils;

import java.util.LinkedHashMap;

public class UXE implements Thread.UncaughtExceptionHandler{
    ElasticHelper elasticHelper = ElasticHelper.getElasticHelperInstance();

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        LinkedHashMap<String, String> logArgs = new LinkedHashMap<>();
        logArgs.put("action", "uncaught_exception");
        logArgs.put("thread", t.getName());
        logArgs.put("error_cause", e.getCause().toString());
        logArgs.put("error_stacktrace", e.getStackTrace().toString());
        elasticHelper.indexLogs(UXE.class, logArgs);
    }
}
