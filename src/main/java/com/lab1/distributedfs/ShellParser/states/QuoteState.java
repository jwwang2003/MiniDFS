package com.lab1.distributedfs.ShellParser.states;

import java.util.List;

import com.lab1.distributedfs.ShellParser.ParseException;

public class QuoteState extends State {

    char quote;

    public QuoteState(char quote) {
        this.quote = quote;
    }

    @Override
    public List<String> parse(String parsing, String accumulator, List<String> parsed, State referrer) throws ParseException {
        if(parsing.isEmpty()) {
            throw new ParseException("Mismatched quote character: " + this.quote);
        }

        char c = (char)parsing.getBytes()[0];

        if(c == '\\') {
            return (new EscapeState()).parse(parsing.substring(1), accumulator, parsed, this);
        } else if(c == this.quote) {
            return (new StartState()).parse(parsing.substring(1), accumulator, parsed, this);
        } else {
            return (new QuoteState(this.quote)).parse(parsing.substring(1), accumulator + c, parsed, this);
        }
    }

}
