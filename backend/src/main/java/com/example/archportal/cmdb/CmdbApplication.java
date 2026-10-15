package com.example.archportal.cmdb;

/** The subset of a CMDB configuration item that the portal uses. */
public record CmdbApplication(String name, String owner, String criticality) {
}
