package dst.ass2.aop.tests.plugin;

import dst.ass2.aop.logging.Invisible;
import dst.ass2.aop.sample.AbstractPluginExecutable;

public class HiddenPluginExecutable extends AbstractPluginExecutable {
    @Override
    @Invisible
    public void execute() {
        super.execute();
    }
}
