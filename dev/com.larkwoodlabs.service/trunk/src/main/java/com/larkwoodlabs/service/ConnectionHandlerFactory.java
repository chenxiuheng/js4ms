package com.larkwoodlabs.service;

public interface ConnectionHandlerFactory {
    ConnectionHandler construct(ConnectionManager manager, Connection connection, Service service);
}
