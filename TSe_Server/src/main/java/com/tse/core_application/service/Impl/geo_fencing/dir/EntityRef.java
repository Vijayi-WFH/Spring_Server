package com.tse.core_application.service.Impl.geo_fencing.dir;

public class EntityRef {
    private final long id;
    private final String name;

    public EntityRef(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
