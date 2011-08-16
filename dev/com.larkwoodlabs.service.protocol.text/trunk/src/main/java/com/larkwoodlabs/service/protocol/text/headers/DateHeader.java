package com.larkwoodlabs.service.protocol.text.headers;

import java.util.Date;

import com.larkwoodlabs.common.exceptions.ParseException;
import com.larkwoodlabs.service.protocol.text.message.FormattedMessageHeader;
import com.larkwoodlabs.service.protocol.text.message.MessageHeader;
import com.larkwoodlabs.service.protocol.text.util.DateUtil;


public class DateHeader extends FormattedMessageHeader {

    private Date date;

    public DateHeader(final String name, final Date value) {
        super(name);
        this.date = value;
    }

    public DateHeader(final DateHeader header) {
        super(header);
        this.date = header.date;
    }

    @Override
    public void appendHeader(MessageHeader header) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void parse(String value) throws ParseException {
        try {
            this.date = DateUtil.toDate(value);
        }
        catch (java.text.ParseException e) {
            throw new ParseException(e);
        }
    }

    @Override
    protected String format() {
        return DateUtil.DATE_FORMAT_RFC_1123.format(this.date);
    }

    @Override
    public Object clone() {
        return new DateHeader(this);
    }

}
