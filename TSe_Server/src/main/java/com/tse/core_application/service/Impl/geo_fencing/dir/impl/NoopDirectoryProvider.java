package com.tse.core_application.service.Impl.geo_fencing.dir.impl;

import com.tse.core_application.service.Impl.geo_fencing.dir.DirectoryProvider;
import com.tse.core_application.service.Impl.geo_fencing.dir.EntityRef;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class NoopDirectoryProvider implements DirectoryProvider {

    @Override
    public List<EntityRef> listUsersByOrg(long orgId) {
        return Collections.emptyList();
    }

    @Override
    public List<EntityRef> listTeamsByOrg(long orgId) {
        return Collections.emptyList();
    }

    @Override
    public List<EntityRef> listProjectsByOrg(long orgId) {
        return Collections.emptyList();
    }

    @Override
    public EntityRef getOrgRef(long orgId) {
        return new EntityRef(orgId, "Org " + orgId);
    }
}
