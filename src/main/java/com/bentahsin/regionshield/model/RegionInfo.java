package com.bentahsin.regionshield.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@ToString
@SuppressFBWarnings({"EI_EXPOSE_REP2"})
public class RegionInfo {
    private final String id;
    private final List<UUID> owners;
    private final List<UUID> members;
    private final String provider;

    @Builder
    public RegionInfo(String id, List<UUID> owners, List<UUID> members, String provider) {
        this.id = id;
        this.provider = provider;
        this.owners = owners != null ? new ArrayList<>(owners) : new ArrayList<>();
        this.members = members != null ? new ArrayList<>(members) : new ArrayList<>();
    }

    public List<UUID> getOwners() {
        return new ArrayList<>(owners);
    }

    public List<UUID> getMembers() {
        return new ArrayList<>(members);
    }
}