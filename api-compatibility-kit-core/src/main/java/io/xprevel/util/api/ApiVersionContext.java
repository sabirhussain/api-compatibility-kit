package io.xprevel.util.api;

public class ApiVersionContext<E extends Enum<E>> {
    private E version;

    public E getVersion() {
        return version;
    }

    public void setVersion(E version) {
        this.version = version;
    }
}
