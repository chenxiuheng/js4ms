package org.js4ms.service.rest.message;

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
        return (object instanceof Method) && this.name.equalsIgnoreCase(((Method)object).name);
    }
    
    @Override
    public String toString() {
        return this.name;
    }
}
