package com.bentahsin.regionshield.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class RegionInfo {
    private String id;
    private List<UUID> owners;
    private List<UUID> members;
    private String provider;
}