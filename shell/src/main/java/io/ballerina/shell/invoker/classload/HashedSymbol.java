/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.shell.invoker.classload;

import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;

import java.util.Objects;
import java.util.Set;

/**
 * A symbol that can be hashed to search back.
 * The symbol name and kind is used to hash the symbol.
 * No other use than to remember the symbols that were seen earlier.
 */
public class HashedSymbol {
    private final String name;
    private final SymbolKind kind;

    /**
     * Helper constructor to create default symbols.
     *
     * @param name Name of the symbol.
     * @param kind Type of the symbol.
     */
    private HashedSymbol(String name, SymbolKind kind) {
        this.name = name;
        this.kind = kind;
    }

    /**
     * Wraps symbol with hashed symbol to make is hashable.
     *
     * @param symbol Symbol to wrap.
     */
    public HashedSymbol(Symbol symbol) {
        this.name = symbol.name();
        this.kind = symbol.kind();
    }

    /**
     * The symbols that are available by default.
     * These are the symbols that are visible inside the main function.
     * TODO: Infer this information automatically when initializing.
     *
     * @return Set of hashed visible symbols.
     */
    public static Set<HashedSymbol> defaults() {
        return Set.of(
                new HashedSymbol("Thread", SymbolKind.TYPE_DEFINITION),
                new HashedSymbol("StrandData", SymbolKind.TYPE_DEFINITION),
                new HashedSymbol("icon", SymbolKind.ANNOTATION),
                new HashedSymbol("strand", SymbolKind.ANNOTATION),
                new HashedSymbol("tainted", SymbolKind.ANNOTATION),
                new HashedSymbol("typeParam", SymbolKind.ANNOTATION),
                new HashedSymbol("untainted", SymbolKind.ANNOTATION),
                new HashedSymbol("deprecated", SymbolKind.ANNOTATION),
                new HashedSymbol("isolatedParam", SymbolKind.ANNOTATION),
                new HashedSymbol("builtinSubtype", SymbolKind.ANNOTATION),
                new HashedSymbol("xml", SymbolKind.MODULE),
                new HashedSymbol("map", SymbolKind.MODULE),
                new HashedSymbol("int", SymbolKind.MODULE),
                new HashedSymbol("java", SymbolKind.MODULE),
                new HashedSymbol("table", SymbolKind.MODULE),
                new HashedSymbol("float", SymbolKind.MODULE),
                new HashedSymbol("error", SymbolKind.MODULE),
                new HashedSymbol("future", SymbolKind.MODULE),
                new HashedSymbol("object", SymbolKind.MODULE),
                new HashedSymbol("stream", SymbolKind.MODULE),
                new HashedSymbol("string", SymbolKind.MODULE),
                new HashedSymbol("decimal", SymbolKind.MODULE),
                new HashedSymbol("boolean", SymbolKind.MODULE),
                new HashedSymbol("typedesc", SymbolKind.MODULE),
                new HashedSymbol("main", SymbolKind.FUNCTION),
                new HashedSymbol("sprintf", SymbolKind.FUNCTION),
                new HashedSymbol("println", SymbolKind.FUNCTION),
                new HashedSymbol("printerr", SymbolKind.FUNCTION),
                new HashedSymbol("recall_h", SymbolKind.FUNCTION),
                new HashedSymbol("memorize_h", SymbolKind.FUNCTION)
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HashedSymbol xSymbol = (HashedSymbol) o;
        return name.equals(xSymbol.name) && kind == xSymbol.kind;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, kind);
    }
}
