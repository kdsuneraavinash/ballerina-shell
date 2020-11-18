module io.ballerina.shell {
    requires io.ballerina.parser;
    requires io.ballerina.tools.api;
    requires org.jline;
    requires freemarker;
    requires commons.cli;
    requires com.google.gson;

    exports io.ballerina.shell;
    exports io.ballerina.shell.executor;
    exports io.ballerina.shell.preprocessor;
    exports io.ballerina.shell.snippet;
    exports io.ballerina.shell.snippet.types;
    exports io.ballerina.shell.transformer;
    exports io.ballerina.shell.treeparser;
    exports io.ballerina.shell.postprocessor;
    exports io.ballerina.shell.exceptions;

    exports org.ballerina.repl;
}
