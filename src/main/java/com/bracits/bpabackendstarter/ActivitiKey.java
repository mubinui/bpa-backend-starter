package com.bracits.bpabackendstarter;

import lombok.Getter;

@Getter
public enum ActivitiKey {
  AcitvitiKeyName1("key1"),
  AcitvitiKeyName2("key2");



  private final String description;

  ActivitiKey(String description) {
    this.description = description;
  }

  public ActivitiKey getEnum(String name) {
    return valueOf(name);
  }
}
