package com.tse.core_application.service.Impl.geo_fencing.dir;

import java.util.List;

public interface DirectoryProvider {
    List<EntityRef> listUsersByOrg(long orgId);
    List<EntityRef> listTeamsByOrg(long orgId);
    List<EntityRef> listProjectsByOrg(long orgId);
    EntityRef getOrgRef(long orgId);
}
