package com.lab1.distributedfs.ShellParser.states;

import java.util.List;

import com.lab1.distributedfs.ShellParser.ParseException;

public class StartState extends State {

    @Override
    public List<String> parse(String parsing, String accumulator, List<String> parsed, State referrer) throws ParseException {
        if(parsing.isEmpty()) {
            if(!accumulator.isEmpty()) {
                parsed.add(accumulator);
            }
            return parsed;
        }

        char c = (char)parsing.getBytes()[0];
        if(c == ' ') {
            if(!accumulator.isEmpty()) {
                parsed.add(accumulator);
            }
            return (new StartState()).parse(parsing.substring(1), "", parsed, this);
        } else if(c == '\\') {
            return (new EscapeState()).parse(parsing.substring(1), accumulator, parsed, this);
        } else if(c == '"' || c == '\'') {
            return (new QuoteState(c)).parse(parsing.substring(1), accumulator, parsed, this);
        }

        return (new StartState()).parse(parsing.substring(1), accumulator + c, parsed, this);
    }

}