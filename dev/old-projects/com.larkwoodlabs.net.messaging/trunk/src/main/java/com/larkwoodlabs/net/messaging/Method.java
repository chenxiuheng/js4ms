package com.larkwoodlabs.net.messaging;

/**
 * A request method identifier.
 *
 * @author Gregory Bumgardner
 */
public final class Method {

    String name;
    
    public Method(String name) {
        this.name = name;
    }
    
    public String getName() {
        return this.name;
    }
    
    @Override
    public boolean equals(Object object) {
        return (object instanceof Method) && this.name.equals(((Method)object).name);
    }
    
    @Override
    public String toString() {
        return this.name;
    }
}
