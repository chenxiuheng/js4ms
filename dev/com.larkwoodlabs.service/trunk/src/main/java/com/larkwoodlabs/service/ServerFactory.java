package com.larkwoodlabs.service;

import java.util.Properties;

public interface ServerFactory {
    Server construct(Properties properties);
}
