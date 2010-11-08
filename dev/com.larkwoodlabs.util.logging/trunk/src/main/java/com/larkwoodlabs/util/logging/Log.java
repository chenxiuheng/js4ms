package com.larkwoodlabs.util.logging;

public final class Log {

    final String classPrefix;
    final String objectId;
    final String prefix;

    public Log(final Object object) {
        if (object instanceof Class) {
            this.classPrefix = ((Class<?>)object).getSimpleName() + ".";
            this.objectId = "[ static ]";
        }
        else {
            this.classPrefix = object.getClass().getSimpleName() + ".";
            this.objectId = Logging.identify(object);
        }
        this.prefix = this.objectId + " ";
    }

    public Log(final Object object, final Class<?> clazz) {
        this.classPrefix = clazz.getSimpleName() + ".";
        this.objectId = Logging.identify(object);
        this.prefix = this.objectId + " ";
    }

    public final String entry(final String methodName, final Object ...args) {
        return Logging.entering(this.objectId, this.classPrefix+methodName, args);
    }

    public final String entry(final String methodName) {
        return Logging.entering(this.objectId, this.classPrefix+methodName);
    }

    public final String exit(final String methodName) {
        return Logging.exiting(this.objectId, this.classPrefix+methodName);
    }

    public final String exit(final String methodName, final Object result) {
        return Logging.exiting(this.objectId, this.classPrefix+methodName, result);
    }

    public final String msg(String message) {
        return this.prefix + message;
    }

    public final String getPrefix() {
        return this.prefix;
    }

}
