package dst.ass2.aop.tests.plugin;

import dst.ass2.aop.sample.AbstractPluginExecutable;

import java.util.logging.Logger;

public class LoggerPluginExecutable extends AbstractPluginExecutable {
    @SuppressWarnings("unused")
    private static Logger log = Logger.getLogger(LoggerPluginExecutable.class.getName());
}
