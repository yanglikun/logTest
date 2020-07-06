package com.github.yanglikun.dlog;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author yanglikun
 */
public class LogLevelMonitor {

    public static void start() {
        new Thread(new Monitor()).start();
    }

    private static class Monitor implements Runnable {

        private long lastModified;

        @Override
        public void run() {
            for (; ; ) {
                try {
                    //加载配置文件
                    URL resource = LogLevelMonitor.class.getClassLoader().getResource("loglevel.properties");
                    if (resource == null) {
                        continue;
                    }
                    File logLevelFile = new File(resource.getPath());
                    if (!logLevelFile.exists()) {
                        continue;
                    }
                    if (lastModified <= 0) {
                        lastModified = logLevelFile.lastModified();
                    } else {
                        if (lastModified == logLevelFile.lastModified()) {//文件没有变化
                            continue;
                        } else {
                            lastModified = logLevelFile.lastModified();//文件变化，赋值为新的
                        }
                    }
                    lastModified = logLevelFile.lastModified();
                    //如果不sleep，下面读到会是空内容，不知道为什么，经过测试用vi不会出现这个问题，vscode和idea会出现。估计和编辑器刷盘有关
                    TimeUnit.SECONDS.sleep(1);
                    List<String> levelConfs = FileUtils.readLines(logLevelFile, "utf-8");

                    //logback特有的设置
                    for (int i = 0; i < levelConfs.size(); i++) {
                        String line = levelConfs.get(i);
                        if (StringUtils.isEmpty(line)) {
                            continue;
                        }
                        String[] split = line.split("=");
                        String loggerName = StringUtils.trim(split[0]);
                        String levelName = StringUtils.trim(split[1]);
                        LoggerContext loggerContext = (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();//这里有强制转换
                        Logger logger = null;
                        if (StringUtils.isNoneBlank(loggerName)) {
                            logger = loggerContext.getLogger(loggerName);
                        } else {
                            logger = loggerContext.getLogger("ROOT");
                        }
                        if (logger != null) {
                            if (StringUtils.isBlank(levelName)) {
                                levelName = "INFO";
                            }
                            logger.setLevel(ch.qos.logback.classic.Level.toLevel(levelName));
                        }
                    }
                    System.out.println("log配置变化，加载成功");
                    TimeUnit.SECONDS.sleep(1);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


}

