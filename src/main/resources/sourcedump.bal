// Copyright (c) $today.year, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
//
//
// ========================== CONVERSION SCRIPT ===========================
// This is a simple script to dump variable values to a file and reload.
// Used by the template generator.
// ========================== CONVERSION SCRIPT ===========================
import ballerina/io;

type everytype any|error;

# Writes a JSON to the given file.
#
# + content - JSON content to write.
# + path - Path to write the JSON file to. This file should be empty or non-existing.
# + return - Error if there was any.
function write(json content, string path) returns error? {
    io:WritableByteChannel wbc = check io:openWritableFile(<@untainted> path);
    io:WritableCharacterChannel wch = new (wbc, "UTF8");
    var result = wch.writeJson(content);
    var close_res = wch.close();
    return result;
}

# Join a string array by a delimeter.
#
# + delimeter - String to use for joining.
# + list - String array to join.
# + return - Joined string representation.
function join_arr(string delimeter, string[] list) returns string {
    string joined = list.reduce(function(string full, string now) returns string {
                                    return full + delimeter + now;
                                }, "");
    if joined.length() < delimeter.length() {
        return joined;
    }
    // remove first delimiter
    return joined.substring(delimeter.length());
}

function get_repr(everytype value, string access) returns string {
    string|error result = str_repr(value, access);
    if result is string {
        return result;
    }else{
        io:println("\u{001b}[30;1m[WARNING] Type of " + access +  " is not yet fully supported by REPL." +
        "It will be treated as a constant and no change will be persisted.\u{001b}[0m");
        return access;
    }
}

# Converts value to a string representation regardless of type.
# The returned representation should be a initializer that
# can be used to give a variable the said value.
# For example, "hello" is converted to "\"Hello\"".
# An error is thrown if representation failed.
#
# + value - Value to represent.
# + access - A string repr of call to access self.
# + return - Representation. Error if failed.
function str_repr(everytype value, string access) returns string|error {
    // simple: nil, boolean, int, float, decimal
    if value is () { // nil
        return "()";
    } else if value is boolean|int|float|decimal { // boolean, int, float, decimal
        return io:sprintf("%s", value);
    }

    // sequence: string, xml
    if value is string { // string
        return string `"${value}"`;
    } else if value is xml { // xml
        return io:sprintf("xml `%s`", value);
    }

    // structured: list, map, record, table[x]
    if value is everytype[] { // list
        string[] mapped = [];
        foreach int i in 0 ..< value.length() {
            mapped.push(get_repr(value[i], string `${access}[${i}]`));
        }
        return "[" + join_arr(",", mapped) + "]";
    } else if (value is map<everytype>) { // map
        string[] mapped = [];
        foreach [string, everytype] [k, v] in value.entries() {
            string mapped_v = get_repr(v, string `${access}["${k}"]`);
            mapped.push(k + ":" + mapped_v);
        }
        return "{" + join_arr(",", mapped) + "}";
    }
    // TODO: Table serialization?
    // Table serializer should be generated by finding all the
    // available table defs and generating the code by replacing id
    // if (value is table<everytype> key(id)) { // table with key
    //     string[] mapped = [];
    //     foreach everytype k in value.iterator() {
    //     }
    //     return "table [" + join_arr(",", mapped) + "]";
    // }
    // if (value is table<everytype>) { // table without key
    //     // No reason, immutable?
    //     return access;
    // }

    // TODO: behavioral: error[x], function[x], object[x], future[x], service[x],
    // typedesc[x], handle[x], stream[x]
    // These cannot be persisted. Maybe show a message stating as such.
    // Or keep snippets that change these values. ??

    // TODO: other: singleton[x], any, never, readonly, distinct[x],
    //        union, intersection, optional, anydata, json, byte
    // any, readonly, union, intersection, optional, anydata should already be handled.
    if value is never {
        return access; // cannot be, whatever
    }

    // Currently unhandled: so will be immutable
    // Types: table, error, function, object, future, service, typedesc, handle,
    //        stream, type reference, singleton, distinct
    return error("Unhandled type");
}

# Converts a variable declaration map into the json format.
# An error is thrown if representation failed.
#
# + vars - Variable declarations map. Should follow `{..., vari: vari_val, ...}` format.
# + return - The json representation of the variable declaration. Error if failed.
function json_repr(map<everytype> vars) returns @untainted map<string> {
    map<string> mapped = {};
    foreach [string, everytype] [name, value] in vars.entries() {
        mapped[name]  = get_repr(value, name);
    }
    return mapped;
}

function restore() {
// Restore variables here
}

function dump(string filename) {
    // Dump variables here
    json repr = json_repr({
    // Variables here

    });
    var result = write(repr, filename);
}


// ========================== CONVERSION SCRIPT ===========================

public function main(string filename) {
    // Dump variables here
    json repr = {
        vars: json_repr({
            // Variables here
        })
    };
    var result = write(repr, filename);
}



