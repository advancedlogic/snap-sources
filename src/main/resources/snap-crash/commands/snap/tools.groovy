package commands.snap

import com.bdl.snap.kernel.commands.ToolsPlugin
import org.crsh.cli.Usage
import org.crsh.cli.Command
import org.crsh.cli.Option

/**
 * Created by skywalker on 19/05/15.
 */

@Usage("SNAP-Kernel tools")
class tools {
    ToolsPlugin toolsPlugin = new ToolsPlugin()

    @Usage("Display log")
    @Command
    public Object log(@Option(names=["l", "lines"]) Integer lines) {
        if (lines == null || lines == 0) lines = 10
        return toolsPlugin.log(lines)
    }

    @Usage("Quit SNAP-Kernel")
    @Command
    public Object quit() {
        toolsPlugin.quit()
    }
}