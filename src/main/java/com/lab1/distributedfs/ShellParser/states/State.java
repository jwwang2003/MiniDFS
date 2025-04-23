package com.lab1.distributedfs.ShellParser.states;

import java.util.List;

import com.lab1.distributedfs.ShellParser.ParseException;

public abstract class State {
    public abstract List<String> parse(String parsing, String accumulator, List<String> parsed, State referrer) throws ParseException;
}