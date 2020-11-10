module io.ballerina.shell {
    requires java.sql;
    requires io.ballerina.parser;
    requires io.ballerina.tools.api;
    requires jline;

    exports io.ballerina.shell;
    exports io.ballerina.shell.executor;
    exports io.ballerina.shell.preprocessor;
    exports io.ballerina.shell.snippet;
    exports io.ballerina.shell.transformer;
    exports io.ballerina.shell.treeparser;
    exports io.ballerina.shell.wrapper;
    exports io.ballerina.shell.postprocessor;

    exports org.ballerina.shell;
}
