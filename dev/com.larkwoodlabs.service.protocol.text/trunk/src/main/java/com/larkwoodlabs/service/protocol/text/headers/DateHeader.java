package com.larkwoodlabs.service.protocol.text.headers;

import java.util.Date;

import com.larkwoodlabs.service.protocol.text.message.Header;
import com.larkwoodlabs.service.protocol.text.util.DateUtil;


public class DateHeader extends Header {

    public DateHeader(final String name, final Date value) {
        super(name,DateUtil.DATE_FORMAT_RFC_1123.format(value));
    }

}
