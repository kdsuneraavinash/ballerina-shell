module io.ballerina.shell.cli {
    requires io.ballerina.shell;
    requires info.picocli;
    requires jline;

    exports io.ballerina.shell.cli;
    exports io.ballerina.shell.cli.handlers.help;
    exports io.ballerina.shell.cli.handlers;
}
